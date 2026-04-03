package com.foodbridge.web.service;

import com.foodbridge.web.dto.AddDonationRequest;
import com.foodbridge.web.dto.NgoNeedRequest;
import com.foodbridge.web.dto.RegisterRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class FoodBridgeService {

    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JdbcTemplate jdbc;

    public FoodBridgeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> login(String email, String password, String role) {
        String sql = "SELECT user_id, name, role, phone, email FROM Users WHERE email = ? AND password = ? AND role = ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, email, password, role);
        if (rows.isEmpty()) {
            return null;
        }
        autoExpireDonations();
        return rows.get(0);
    }

    public boolean register(RegisterRequest req) {
        String sql = "INSERT INTO Users (name, role, phone, email, password) VALUES (?, ?, ?, ?, ?)";
        try {
            return jdbc.update(sql, req.name(), req.role(), req.phone(), req.email(), req.password()) > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    public List<Map<String, Object>> getAvailableDonations() {
        autoExpireDonations();
        String sql = """
                SELECT dp.donation_id, dp.donor_id, u.name AS donor_name,
                       fi.item_name, fi.category, dp.quantity,
                       DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at, dp.status,
                       dp.request_id
                FROM Donation_Pool dp
                INNER JOIN Users u ON dp.donor_id = u.user_id
                INNER JOIN Food_Items fi ON dp.item_id = fi.item_id
                WHERE dp.status = 'AVAILABLE' AND dp.expiry_at > NOW()
                ORDER BY dp.expiry_at ASC
                """;
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> getDonorDonations(int donorId) {
        autoExpireDonations();
        String sql = """
                SELECT dp.donation_id, dp.donor_id, u.name AS donor_name,
                       fi.item_name, fi.category, dp.quantity,
                       DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at, dp.status,
                       dp.request_id
                FROM Donation_Pool dp
                INNER JOIN Users u ON dp.donor_id = u.user_id
                INNER JOIN Food_Items fi ON dp.item_id = fi.item_id
                WHERE dp.donor_id = ?
                ORDER BY dp.created_at DESC
                """;
        return jdbc.queryForList(sql, donorId);
    }

    @Transactional
    public boolean addDonation(AddDonationRequest req) {
        Integer itemId = getOrCreateFoodItem(req.itemName());
        if (itemId == null) {
            return false;
        }

        LocalDateTime expiry;
        try {
            expiry = LocalDateTime.parse(req.expiryAt(), INPUT_FMT);
        } catch (DateTimeParseException ex) {
            return false;
        }

        Integer requestNgoId = null;
        boolean linkToRequest = req.requestId() != null;

        if (linkToRequest) {
            String reqSql = "SELECT ngo_id FROM NGO_Requests WHERE request_id = ? AND status = 'OPEN'";
            List<Map<String, Object>> reqRows = jdbc.queryForList(reqSql, req.requestId());
            if (reqRows.isEmpty()) {
                return false;
            }
            requestNgoId = ((Number) reqRows.get(0).get("ngo_id")).intValue();
        }

        String status = linkToRequest ? "CLAIMED" : "AVAILABLE";
        String insDonation = "INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status, request_id) VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder kh = new GeneratedKeyHolder();
        int rows = jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(insDonation, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, req.donorId());
            ps.setInt(2, itemId);
            ps.setString(3, req.quantity());
            ps.setTimestamp(4, Timestamp.valueOf(expiry));
            ps.setString(5, status);
            if (req.requestId() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, req.requestId());
            }
            return ps;
        }, kh);

        if (rows <= 0 || kh.getKey() == null) {
            return false;
        }

        int donationId = kh.getKey().intValue();

        if (linkToRequest && requestNgoId != null) {
            jdbc.update("INSERT INTO Claims (donation_id, ngo_id) VALUES (?, ?)", donationId, requestNgoId);

            jdbc.update(
                    """
                    INSERT INTO Impact_Log (donation_id, meals_fed)
                    SELECT ?, GREATEST(1, CAST(REGEXP_REPLACE(quantity, '[^0-9]','') AS UNSIGNED) * 2)
                    FROM Donation_Pool WHERE donation_id = ?
                    """,
                    donationId,
                    donationId
            );

            jdbc.update(
                    "UPDATE NGO_Requests SET status = 'FULFILLED', fulfilled_donation_id = ? WHERE request_id = ?",
                    donationId,
                    req.requestId()
            );
        }

        return true;
    }

    @Transactional
    public boolean claimDonation(int donationId, int ngoId) {
        int updRows = jdbc.update(
                "UPDATE Donation_Pool SET status = 'CLAIMED' WHERE donation_id = ? AND status = 'AVAILABLE'",
                donationId
        );
        if (updRows == 0) {
            return false;
        }

        jdbc.update("INSERT INTO Claims (donation_id, ngo_id) VALUES (?, ?)", donationId, ngoId);
        jdbc.update(
                """
                INSERT INTO Impact_Log (donation_id, meals_fed)
                SELECT ?, GREATEST(1, CAST(REGEXP_REPLACE(quantity, '[^0-9]','') AS UNSIGNED) * 2)
                FROM Donation_Pool WHERE donation_id = ?
                """,
                donationId,
                donationId
        );
        return true;
    }

    public List<Map<String, Object>> getNgoClaims(int ngoId) {
        String sql = """
                SELECT c.claim_id, fi.item_name, dp.quantity, u.name AS donor_name,
                       DATE_FORMAT(c.claim_time, '%d-%m-%Y %H:%i') AS claimed_at,
                       COALESCE(il.meals_fed, 0) AS meals_fed
                FROM Claims c
                INNER JOIN Donation_Pool dp ON c.donation_id = dp.donation_id
                INNER JOIN Food_Items fi ON dp.item_id = fi.item_id
                INNER JOIN Users u ON dp.donor_id = u.user_id
                LEFT JOIN Impact_Log il ON c.donation_id = il.donation_id
                WHERE c.ngo_id = ?
                ORDER BY c.claim_time DESC
                """;
        return jdbc.queryForList(sql, ngoId);
    }

    public Map<String, Object> getPlatformStats() {
                autoExpireDonations();
        String sql = """
                SELECT
                  COUNT(*) AS total_donations,
                  SUM(CASE WHEN status='CLAIMED' THEN 1 ELSE 0 END) AS claimed_count,
                  SUM(CASE WHEN status='AVAILABLE' THEN 1 ELSE 0 END) AS available_count,
                  SUM(CASE WHEN status='EXPIRED' THEN 1 ELSE 0 END) AS expired_count,
                  (SELECT COALESCE(SUM(meals_fed),0) FROM Impact_Log) AS total_meals_fed,
                  (SELECT COUNT(*) FROM NGO_Requests WHERE status = 'OPEN') AS open_requests
                FROM Donation_Pool
                """;
        return jdbc.queryForMap(sql);
    }

    public List<Map<String, Object>> getOpenRequests() {
        String sql = """
                SELECT r.request_id, r.ngo_id, u.name AS ngo_name, r.item_name, r.quantity_needed,
                       r.notes, r.status, DATE_FORMAT(r.created_at, '%d-%m-%Y %H:%i') AS created_at
                FROM NGO_Requests r
                INNER JOIN Users u ON r.ngo_id = u.user_id
                WHERE r.status = 'OPEN'
                ORDER BY r.created_at DESC
                """;
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> getAllRequests() {
        String sql = """
                SELECT r.request_id, r.ngo_id, u.name AS ngo_name, r.item_name, r.quantity_needed,
                       r.notes, r.status, r.fulfilled_donation_id,
                       DATE_FORMAT(r.created_at, '%d-%m-%Y %H:%i') AS created_at
                FROM NGO_Requests r
                INNER JOIN Users u ON r.ngo_id = u.user_id
                ORDER BY r.created_at DESC
                """;
        return jdbc.queryForList(sql);
    }

    public boolean createNgoRequest(NgoNeedRequest req) {
        String sql = "INSERT INTO NGO_Requests (ngo_id, item_name, quantity_needed, notes, status) VALUES (?, ?, ?, ?, 'OPEN')";
        return jdbc.update(sql, req.ngoId(), req.itemName(), req.quantityNeeded(), req.notes()) > 0;
    }

    public List<Map<String, Object>> getAllUsers() {
        String sql = "SELECT user_id, name, role, phone, email, DATE_FORMAT(created_at, '%d-%m-%Y %H:%i') AS created_at FROM Users ORDER BY created_at DESC";
        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> getAllDonations() {
        autoExpireDonations();
        String sql = """
                SELECT dp.donation_id, u.name AS donor_name, fi.item_name, dp.quantity, dp.status,
                       DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at,
                       DATE_FORMAT(dp.created_at, '%d-%m-%Y %H:%i') AS created_at
                FROM Donation_Pool dp
                INNER JOIN Users u ON dp.donor_id = u.user_id
                INNER JOIN Food_Items fi ON dp.item_id = fi.item_id
                ORDER BY dp.created_at DESC
                """;
        return jdbc.queryForList(sql);
    }

    public void autoExpireDonations() {
        jdbc.update("UPDATE Donation_Pool SET status = 'EXPIRED' WHERE status = 'AVAILABLE' AND expiry_at < NOW()");
    }

    private Integer getOrCreateFoodItem(String itemName) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT item_id FROM Food_Items WHERE item_name = ?", itemName);
        if (!rows.isEmpty()) {
            return ((Number) rows.get(0).get("item_id")).intValue();
        }

        KeyHolder kh = new GeneratedKeyHolder();
        int rowsInserted = jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO Food_Items (item_name, category) VALUES (?, 'OTHER')", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, itemName);
            return ps;
        }, kh);

        if (rowsInserted <= 0 || kh.getKey() == null) {
            return null;
        }
        return kh.getKey().intValue();
    }
}

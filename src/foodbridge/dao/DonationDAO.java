package foodbridge.dao;

import foodbridge.models.Donation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DonationDAO — all DB operations on Donation_Pool, Claims, and Impact_Log.
 *
 * QUERIES DEMONSTRATED (for viva):
 *   • INSERT  — add donation
 *   • SELECT with JOIN (2 tables) — list available food with donor name & item name
 *   • SELECT with WHERE — view donor's own donations
 *   • UPDATE  — mark donation as CLAIMED
 *   • INSERT  — create a claim record
 *   • INSERT  — log impact (meals_fed)
 *   • Aggregate (COUNT, SUM) — impact statistics
 *   • AUTO EXPIRY UPDATE — batch update expired donations
 */
public class DonationDAO {

    // ── ADD DONATION ───────────────────────────────────────────────────────
    /**
     * INSERT into Donation_Pool.
     * item_id is looked up from Food_Items by name (sub-query style, done in code).
     */
    public boolean addDonation(int donorId, String itemName, String quantity, String expiryAt) {
        // Step 1: find or insert the food item
        int itemId = getOrCreateFoodItem(itemName);
        if (itemId == -1) {
            System.out.println("[ERROR] Could not resolve food item.");
            return false;
        }

        // Step 2: INSERT into Donation_Pool
        String sql = "INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status) " +
                     "VALUES (?, ?, ?, ?, 'AVAILABLE')";
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return false;
        }
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt   (1, donorId);
            ps.setInt   (2, itemId);
            ps.setString(3, quantity);
            ps.setString(4, expiryAt);

            System.out.println("\n[SQL] " + sql);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next())
                    System.out.println("[DB]  Donation created with ID = " + keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // helper: get item_id from Food_Items, or insert if not present
    private int getOrCreateFoodItem(String itemName) {
        String selectSql = "SELECT item_id FROM Food_Items WHERE item_name = ?";
        String insertSql = "INSERT INTO Food_Items (item_name, category) VALUES (?, 'OTHER')";
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return -1;
        }
        try {
            PreparedStatement sel = con.prepareStatement(selectSql);
            sel.setString(1, itemName);
            System.out.println("[SQL] " + selectSql);
            ResultSet rs = sel.executeQuery();
            if (rs.next()) return rs.getInt("item_id");

            // Not found → insert
            PreparedStatement ins = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            ins.setString(1, itemName);
            System.out.println("[SQL] " + insertSql);
            ins.executeUpdate();
            ResultSet keys = ins.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // ── VIEW DONOR'S OWN DONATIONS ─────────────────────────────────────────
    /**
     * SELECT with JOIN: Donation_Pool ⟶ Food_Items
     * WHERE donor_id = ?
     * Demonstrates: INNER JOIN, WHERE, ORDER BY
     */
    public List<Donation> getDonorDonations(int donorId) {
        List<Donation> list = new ArrayList<>();
        String sql =
            "SELECT dp.donation_id, dp.donor_id, u.name AS donor_name, " +
            "       fi.item_name, fi.category, dp.quantity, " +
            "       DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at, dp.status " +
            "FROM Donation_Pool dp " +
            "INNER JOIN Users      u  ON dp.donor_id = u.user_id " +
            "INNER JOIN Food_Items fi ON dp.item_id  = fi.item_id " +
            "WHERE dp.donor_id = ? " +
            "ORDER BY dp.created_at DESC";

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return list;
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, donorId);
            System.out.println("\n[SQL] " + sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── VIEW AVAILABLE FOOD (NGO) ──────────────────────────────────────────
    /**
     * SELECT with 2 JOINs: Donation_Pool ⟶ Food_Items ⟶ Users
     * WHERE status = 'AVAILABLE' AND expiry_at > NOW()
     * Demonstrates: multi-table JOIN, WHERE with function (NOW())
     */
    public List<Donation> getAvailableDonations() {
        List<Donation> list = new ArrayList<>();
        String sql =
            "SELECT dp.donation_id, dp.donor_id, u.name AS donor_name, " +
            "       fi.item_name, fi.category, dp.quantity, " +
            "       DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at, dp.status " +
            "FROM Donation_Pool dp " +
            "INNER JOIN Users      u  ON dp.donor_id = u.user_id " +
            "INNER JOIN Food_Items fi ON dp.item_id  = fi.item_id " +
            "WHERE dp.status = 'AVAILABLE' AND dp.expiry_at > NOW() " +
            "ORDER BY dp.expiry_at ASC";

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return list;
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {

            System.out.println("\n[SQL] " + sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── CLAIM DONATION ─────────────────────────────────────────────────────
    /**
     * Two operations wrapped in a TRANSACTION:
     *   1. UPDATE Donation_Pool SET status = 'CLAIMED'
     *   2. INSERT into Claims
     *   3. INSERT into Impact_Log (estimated meals = quantity parsed * 2)
     * Demonstrates: TRANSACTION (commit/rollback), UPDATE, INSERT FK relationship
     */
    public boolean claimDonation(int donationId, int ngoId) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            if (con == null) {
                System.err.println("[ERROR] Database connection unavailable.");
                return false;
            }
            con.setAutoCommit(false);   // ← BEGIN TRANSACTION

            // 1. UPDATE status
            String updateSql = "UPDATE Donation_Pool SET status = 'CLAIMED' " +
                                "WHERE donation_id = ? AND status = 'AVAILABLE'";
            PreparedStatement upd = con.prepareStatement(updateSql);
            upd.setInt(1, donationId);
            System.out.println("\n[SQL] " + updateSql);
            int updRows = upd.executeUpdate();

            if (updRows == 0) {
                System.out.println("[WARN] Donation not available (already claimed or expired).");
                con.rollback();
                return false;
            }

            // 2. INSERT into Claims
            String claimSql = "INSERT INTO Claims (donation_id, ngo_id) VALUES (?, ?)";
            PreparedStatement clm = con.prepareStatement(claimSql);
            clm.setInt(1, donationId);
            clm.setInt(2, ngoId);
            System.out.println("[SQL] " + claimSql);
            clm.executeUpdate();

            // 3. INSERT into Impact_Log (estimate: every kg/portion ≈ 2 meals)
            String impactSql = "INSERT INTO Impact_Log (donation_id, meals_fed) " +
                                "SELECT ?, GREATEST(1, CAST(REGEXP_REPLACE(quantity, '[^0-9]','') AS UNSIGNED) * 2) " +
                                "FROM Donation_Pool WHERE donation_id = ?";
            PreparedStatement imp = con.prepareStatement(impactSql);
            imp.setInt(1, donationId);
            imp.setInt(2, donationId);
            System.out.println("[SQL] " + impactSql);
            imp.executeUpdate();

            con.commit();   // ← COMMIT
            System.out.println("[DB]  Transaction committed successfully.");
            return true;

        } catch (SQLException e) {
            System.out.println("[ERROR] Transaction failed — rolling back.");
            try { if (con != null) con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { if (con != null) con.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
        return false;
    }

    // ── NGO CLAIMED HISTORY ────────────────────────────────────────────────
    /**
     * SELECT with 3-table JOIN: Claims ⟶ Donation_Pool ⟶ Food_Items ⟶ Users
     * Demonstrates: complex multi-join query
     */
    public void printNGOClaims(int ngoId) {
        String sql =
            "SELECT c.claim_id, fi.item_name, dp.quantity, u.name AS donor_name, " +
            "       DATE_FORMAT(c.claim_time, '%d-%m-%Y %H:%i') AS claimed_at, il.meals_fed " +
            "FROM Claims c " +
            "INNER JOIN Donation_Pool dp ON c.donation_id = dp.donation_id " +
            "INNER JOIN Food_Items    fi ON dp.item_id    = fi.item_id " +
            "INNER JOIN Users         u  ON dp.donor_id   = u.user_id " +
            "LEFT  JOIN Impact_Log    il ON c.donation_id = il.donation_id " +
            "WHERE c.ngo_id = ? " +
            "ORDER BY c.claim_time DESC";

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return;
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, ngoId);
            System.out.println("\n[SQL] " + sql);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n╔══════╦════════════════════╦══════════╦═══════════════════╦═════════════════════╦═══════════╗");
            System.out.println( "║ Clm# ║ Food Item          ║ Qty      ║ Donated By        ║ Claimed At          ║ Meals Fed ║");
            System.out.println( "╠══════╬════════════════════╬══════════╬═══════════════════╬═════════════════════╬═══════════╣");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("║ %-4d ║ %-18s ║ %-8s ║ %-17s ║ %-19s ║ %-9d ║%n",
                    rs.getInt("claim_id"),
                    rs.getString("item_name"),
                    rs.getString("quantity"),
                    rs.getString("donor_name"),
                    rs.getString("claimed_at"),
                    rs.getInt("meals_fed"));
            }

            if (!found) System.out.println("║               No claims found for this NGO.                                        ║");
            System.out.println("╚══════╩════════════════════╩══════════╩═══════════════════╩═════════════════════╩═══════════╝");

        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── OPEN NGO REQUESTS (DONOR LIVE VIEW) ─────────────────────────────
    /**
     * SELECT open NGO requests so donors can fulfill urgent needs.
     * Demonstrates: JOIN + WHERE + ORDER BY.
     */
    public void printOpenNgoRequests() {
        String sql =
            "SELECT r.request_id, u.name AS ngo_name, r.item_name, r.quantity_needed, " +
            "       COALESCE(r.notes, '-') AS notes, " +
            "       DATE_FORMAT(r.created_at, '%d-%m-%Y %H:%i') AS created_at " +
            "FROM NGO_Requests r " +
            "INNER JOIN Users u ON r.ngo_id = u.user_id " +
            "WHERE r.status = 'OPEN' " +
            "ORDER BY r.created_at DESC";

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            System.out.println("\n[SQL] " + sql);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n  ┌─────────────────────────────────────────────────────────────────────────────────────────────────────────┐");
            System.out.println(  "  │                                         LIVE NGO REQUESTS                                               │");
            System.out.println(  "  ├──────┬─────────────────────┬─────────────────────┬──────────────┬──────────────────────────┬──────────────┤");
            System.out.println(  "  │ Req# │ NGO                 │ Item Needed         │ Qty Needed   │ Notes                    │ Requested At │");
            System.out.println(  "  ├──────┼─────────────────────┼─────────────────────┼──────────────┼──────────────────────────┼──────────────┤");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("  │ %-4d │ %-19s │ %-19s │ %-12s │ %-24s │ %-12s │%n",
                    rs.getInt("request_id"),
                    trimCell(rs.getString("ngo_name"), 19),
                    trimCell(rs.getString("item_name"), 19),
                    trimCell(rs.getString("quantity_needed"), 12),
                    trimCell(rs.getString("notes"), 24),
                    trimCell(rs.getString("created_at"), 12));
            }

            if (!found) {
                System.out.println("  │                           No open NGO requests right now. Check again soon.                             │");
            }
            System.out.println("  └──────┴─────────────────────┴─────────────────────┴──────────────┴──────────────────────────┴──────────────┘");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── IMPACT STATISTICS (Aggregate Queries) ─────────────────────────────
    /**
     * Uses COUNT(), SUM() aggregate functions.
     * Demonstrates: GROUP BY, aggregate functions, subquery.
     */
    public void printImpactStats() {
        String sql =
            "SELECT " +
            "  COUNT(*)                          AS total_donations, " +
            "  SUM(CASE WHEN status='CLAIMED'   THEN 1 ELSE 0 END) AS claimed_count, " +
            "  SUM(CASE WHEN status='AVAILABLE' THEN 1 ELSE 0 END) AS available_count, " +
            "  SUM(CASE WHEN status='EXPIRED'   THEN 1 ELSE 0 END) AS expired_count, " +
            "  (SELECT COALESCE(SUM(meals_fed),0) FROM Impact_Log)  AS total_meals_fed " +
            "FROM Donation_Pool";

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return;
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {

            System.out.println("\n[SQL] " + sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("\n  ╔══════════════════════════════╗");
                System.out.println(  "  ║     PLATFORM IMPACT STATS   ║");
                System.out.println(  "  ╠══════════════════════════════╣");
                System.out.printf ( "  ║  Total Donations   : %-7d ║%n", rs.getInt("total_donations"));
                System.out.printf ( "  ║  Claimed           : %-7d ║%n", rs.getInt("claimed_count"));
                System.out.printf ( "  ║  Still Available   : %-7d ║%n", rs.getInt("available_count"));
                System.out.printf ( "  ║  Expired (wasted)  : %-7d ║%n", rs.getInt("expired_count"));
                System.out.printf ( "  ║  Total Meals Fed   : %-7d ║%n", rs.getInt("total_meals_fed"));
                System.out.println(  "  ╚══════════════════════════════╝");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── AUTO-EXPIRE STALE DONATIONS ────────────────────────────────────────
    /**
     * UPDATE with NOW() — marks past-expiry AVAILABLE donations as EXPIRED.
     * Demonstrates: batch UPDATE, date comparison.
     */
    public void autoExpireDonations() {
        String sql = "UPDATE Donation_Pool " +
                     "SET status = 'EXPIRED' " +
                     "WHERE status = 'AVAILABLE' AND expiry_at < NOW()";
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return;
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {

            System.out.println("[SQL] " + sql);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("[DB]  Auto-expired " + rows + " donation(s).");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── HELPER ─────────────────────────────────────────────────────────────
    private Donation mapRow(ResultSet rs) throws SQLException {
        return new Donation(
            rs.getInt("donation_id"),
            rs.getInt("donor_id"),
            rs.getString("donor_name"),
            rs.getString("item_name"),
            rs.getString("category"),
            rs.getString("quantity"),
            rs.getString("expiry_at"),
            rs.getString("status")
        );
    }

    private String trimCell(String value, int width) {
        if (value == null) return "-";
        if (value.length() <= width) return value;
        if (width <= 3) return value.substring(0, width);
        return value.substring(0, width - 3) + "...";
    }
}

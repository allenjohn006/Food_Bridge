package foodbridge.dao;

import foodbridge.models.User;

import java.sql.*;

/**
 * UserDAO — handles all DB operations related to the Users table.
 *
 * QUERIES DEMONSTRATED:
 *   • SELECT with WHERE (login)
 *   • INSERT (register)
 */
public class UserDAO {

    // ── LOGIN ─────────────────────────────────────────────────────────────
    /**
     * SELECT query: fetch user by email + password + role.
     * Demonstrates: WHERE clause, parameterised PreparedStatement.
     */
    public User login(String email, String password, String role) {
        String sql = "SELECT user_id, name, role, phone, email " +
                     "FROM Users " +
                     "WHERE email = ? AND password = ? AND role = ?";
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return null;
        }
        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);
            ps.setString(3, role);

            System.out.println("\n[SQL] " + sql);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getString("role"),
                    rs.getString("phone"),
                    rs.getString("email")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;   // login failed
    }

    // ── REGISTER ──────────────────────────────────────────────────────────
    /**
     * INSERT query: add a new donor or NGO.
     * Demonstrates: INSERT INTO with auto-increment PK retrieval.
     */
    public boolean register(String name, String role, String phone,
                             String email, String password) {
        String sql = "INSERT INTO Users (name, role, phone, email, password) " +
                     "VALUES (?, ?, ?, ?, ?)";
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.err.println("[ERROR] Database connection unavailable.");
            return false;
        }
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, role);
            ps.setString(3, phone);
            ps.setString(4, email);
            ps.setString(5, password);

            System.out.println("\n[SQL] " + sql);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next())
                    System.out.println("[DB]  New user created with ID = " + keys.getInt(1));
                return true;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("[ERROR] Email already registered. Please use a different email.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

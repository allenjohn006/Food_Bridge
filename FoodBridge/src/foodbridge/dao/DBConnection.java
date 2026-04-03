package foodbridge.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection — single-responsibility class that manages the JDBC connection.
 * Update the three constants below to match your local MySQL setup.
 */
public class DBConnection {

    // ── Change these to match your MySQL installation ──
    private static final String URL      = "jdbc:mysql://localhost:3306/foodbridge_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "Malrajadichiku@610";   // ← your MySQL root password

    private static Connection connection = null;

    /** Returns a live connection, creating one if needed. */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] MySQL Driver not found. Add mysql-connector-j.jar to lib/");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[ERROR] Cannot connect to MySQL. Check credentials in DBConnection.java");
            e.printStackTrace();
        }
        return connection;
    }

    /** Closes the connection gracefully. */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

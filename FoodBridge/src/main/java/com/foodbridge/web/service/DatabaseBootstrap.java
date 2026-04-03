package com.foodbridge.web.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseBootstrap {

    private final JdbcTemplate jdbc;

    public DatabaseBootstrap(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void ensureWebSchema() {
        // Keep upgrades idempotent so existing terminal users can move to web safely.
        try {
            jdbc.execute("ALTER TABLE Users MODIFY role ENUM('DONOR','NGO','ADMIN') NOT NULL");
        } catch (Exception ex) {
            // May fail if column already modified
        }

        try {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS NGO_Requests (
                        request_id INT AUTO_INCREMENT PRIMARY KEY,
                        ngo_id INT NOT NULL,
                        item_name VARCHAR(100) NOT NULL,
                        quantity_needed VARCHAR(50) NOT NULL,
                        notes VARCHAR(255),
                        status ENUM('OPEN','FULFILLED','CANCELLED') DEFAULT 'OPEN',
                        fulfilled_donation_id INT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (ngo_id) REFERENCES Users(user_id) ON DELETE CASCADE,
                        FOREIGN KEY (fulfilled_donation_id) REFERENCES Donation_Pool(donation_id) ON DELETE SET NULL
                    )
                    """);
        } catch (Exception ex) {
            // Table may already exist
        }

        // Check if request_id column exists before adding
        try {
            Integer colExists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='Donation_Pool' AND COLUMN_NAME='request_id' AND TABLE_SCHEMA=DATABASE()",
                    Integer.class
            );
            if (colExists == null || colExists == 0) {
                jdbc.execute("ALTER TABLE Donation_Pool ADD COLUMN request_id INT NULL");
            }
        } catch (Exception ex) {
            try {
                jdbc.execute("ALTER TABLE Donation_Pool ADD COLUMN request_id INT NULL");
            } catch (Exception ignored) {
                // Column may already exist
            }
        }

        // Ensure admin user exists
        try {
            Integer adminCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM Users WHERE email = 'admin@foodbridge.com'",
                    Integer.class
            );

            if (adminCount == null || adminCount == 0) {
                jdbc.update(
                        "INSERT INTO Users (name, role, phone, email, password) VALUES (?, 'ADMIN', ?, ?, ?)",
                        "FoodBridge Admin",
                        "9000000000",
                        "admin@foodbridge.com",
                        "admin123"
                );
            }
        } catch (Exception ex) {
            // Admin may already exist or insert may fail
        }
    }
}

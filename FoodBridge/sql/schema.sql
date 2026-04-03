-- ============================================================
--  FoodBridge Database — Full Schema + Sample Data
--  Run this file once in MySQL to set up everything
-- ============================================================

CREATE DATABASE IF NOT EXISTS foodbridge_db;
USE foodbridge_db;

-- ─────────────────────────────────────────────
-- TABLE 1: Users  (Donors + NGOs in one table)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Users (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100)        NOT NULL,
    role      ENUM('DONOR','NGO','ADMIN') NOT NULL,
    phone     VARCHAR(15),
    email     VARCHAR(100)        UNIQUE NOT NULL,
    password  VARCHAR(255)        NOT NULL,   -- plain-text for demo; hash in prod
    created_at DATETIME           DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────────
-- TABLE 2: Food_Items  (catalogue, avoids repetition)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Food_Items (
    item_id     INT AUTO_INCREMENT PRIMARY KEY,
    item_name   VARCHAR(100)              NOT NULL,
    category    ENUM('VEG','NON-VEG','BEVERAGE','BAKERY','OTHER') NOT NULL
);

-- ─────────────────────────────────────────────
-- TABLE 3: Donation_Pool  (MOST IMPORTANT TABLE)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Donation_Pool (
    donation_id  INT AUTO_INCREMENT PRIMARY KEY,
    donor_id     INT            NOT NULL,
    item_id      INT            NOT NULL,
    quantity     VARCHAR(50)    NOT NULL,          -- e.g. "5 kg", "20 portions"
    expiry_at    DATETIME       NOT NULL,
    status       ENUM('AVAILABLE','CLAIMED','EXPIRED') DEFAULT 'AVAILABLE',
    request_id   INT            NULL,
    created_at   DATETIME       DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (donor_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id)  REFERENCES Food_Items(item_id)
);

CREATE TABLE IF NOT EXISTS NGO_Requests (
    request_id            INT AUTO_INCREMENT PRIMARY KEY,
    ngo_id                INT NOT NULL,
    item_name             VARCHAR(100) NOT NULL,
    quantity_needed       VARCHAR(50) NOT NULL,
    notes                 VARCHAR(255),
    status                ENUM('OPEN','FULFILLED','CANCELLED') DEFAULT 'OPEN',
    fulfilled_donation_id INT NULL,
    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (ngo_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (fulfilled_donation_id) REFERENCES Donation_Pool(donation_id) ON DELETE SET NULL
);


CREATE TABLE IF NOT EXISTS Claims (
    claim_id    INT AUTO_INCREMENT PRIMARY KEY,
    donation_id INT      NOT NULL UNIQUE,   -- one donation → one claim
    ngo_id      INT      NOT NULL,
    claim_time  DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (donation_id) REFERENCES Donation_Pool(donation_id) ON DELETE CASCADE,
    FOREIGN KEY (ngo_id)      REFERENCES Users(user_id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────────
-- TABLE 5: Impact_Log  (analytics)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS Impact_Log (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    donation_id INT NOT NULL UNIQUE,
    meals_fed   INT NOT NULL,
    logged_at   DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (donation_id) REFERENCES Donation_Pool(donation_id) ON DELETE CASCADE
);

-- ============================================================
--  SAMPLE DATA
-- ============================================================

-- Users
INSERT INTO Users (name, role, phone, email, password) VALUES
('Hotel Saravana Bhavan', 'DONOR', '9876543210', 'saravana@donor.com',  'donor123'),
('Taj Residency Kitchen',  'DONOR', '9123456780', 'taj@donor.com',       'donor456'),
('Green Earth NGO',        'NGO',   '9988776655', 'greenearth@ngo.com',  'ngo123'),
('Helping Hands Trust',    'NGO',   '9001122334', 'helping@ngo.com',     'ngo456'),
('FoodBridge Admin',       'ADMIN', '9000000000', 'admin@foodbridge.com','admin123');

-- Food Items
INSERT INTO Food_Items (item_name, category) VALUES
('Steamed Rice',      'VEG'),
('Sambar',            'VEG'),
('Grilled Chicken',   'NON-VEG'),
('Bread Loaves',      'BAKERY'),
('Vegetable Biryani', 'VEG'),
('Fruit Juice',       'BEVERAGE'),
('Dal Tadka',         'VEG'),
('Fish Curry',        'NON-VEG');

-- Donation Pool (some available, some claimed, one expired)
INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status) VALUES
(1, 1, '10 kg',       DATE_ADD(NOW(), INTERVAL 4 HOUR),  'AVAILABLE'),
(1, 2, '15 litres',   DATE_ADD(NOW(), INTERVAL 3 HOUR),  'AVAILABLE'),
(2, 3, '30 portions', DATE_ADD(NOW(), INTERVAL 5 HOUR),  'CLAIMED'),
(2, 4, '50 loaves',   DATE_ADD(NOW(), INTERVAL 2 HOUR),  'AVAILABLE'),
(1, 5, '8 kg',        DATE_ADD(NOW(), INTERVAL 6 HOUR),  'AVAILABLE'),
(2, 7, '5 kg',        DATE_ADD(NOW(), INTERVAL -1 HOUR), 'EXPIRED');

-- Claims
INSERT INTO Claims (donation_id, ngo_id) VALUES
(3, 3);

-- Impact Log
INSERT INTO Impact_Log (donation_id, meals_fed) VALUES
(3, 60);

-- NGO Requests (for live donor fulfillment)
INSERT INTO NGO_Requests (ngo_id, item_name, quantity_needed, notes, status) VALUES
(3, 'Cooked Rice', '20 portions', 'For evening shelter service', 'OPEN'),
(4, 'Bread', '30 loaves', 'Morning distribution drive', 'OPEN');

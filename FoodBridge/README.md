# 🌱 FoodBridge — DBMS Project

> A database-driven platform connecting food donors (restaurants/hotels) with NGOs to eliminate food waste.

---

## 📁 Project Structure

```
FoodBridge/
├── .vscode/
│   ├── tasks.json          ← Ctrl+Shift+B to compile & run
│   └── launch.json         ← F5 to debug
├── src/foodbridge/
│   ├── Main.java           ← Entry point
│   ├── models/
│   │   ├── User.java
│   │   └── Donation.java
│   ├── dao/
│   │   ├── DBConnection.java   ← JDBC connection
│   │   ├── UserDAO.java        ← SELECT / INSERT on Users
│   │   └── DonationDAO.java    ← All donation queries
│   └── ui/
│       ├── DonorUI.java
│       └── NGOUI.java
├── sql/
│   └── schema.sql          ← Run this in MySQL first!
├── lib/
│   └── mysql-connector-j-*.jar  ← You must download this!
└── out/                    ← Compiled .class files (auto-created)
```

---

## ⚙️ Setup (Step by Step)

### Step 1 — MySQL Setup
```sql
-- Open MySQL Workbench or terminal and run:
source /path/to/FoodBridge/sql/schema.sql
```

### Step 2 — Download MySQL JDBC Driver
1. Go to: https://dev.mysql.com/downloads/connector/j/
2. Download **Platform Independent** ZIP
3. Extract `mysql-connector-j-x.x.x.jar`
4. **Place the `.jar` file inside the `lib/` folder**

### Step 3 — Configure DB Password
Open `src/foodbridge/dao/DBConnection.java` and update:
```java
private static final String PASSWORD = "your_mysql_password";
```

### Step 4 — Compile & Run in VS Code
- Press `Ctrl + Shift + B` → select **"1 - Compile FoodBridge"**
- Then select **"2 - Run FoodBridge"**

OR run manually in the VS Code terminal:
```bash
# macOS / Linux
mkdir -p out
javac -cp "lib/*" -d out $(find src -name "*.java")
java  -cp "out:lib/*" foodbridge.Main

# Windows (PowerShell)
mkdir out
javac -cp "lib\*" -d out src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java
java  -cp "out;lib\*" foodbridge.Main
```

---

## 🔑 Demo Login Credentials

| Role  | Email                  | Password |
|-------|------------------------|----------|
| Donor | saravana@donor.com     | donor123 |
| Donor | taj@donor.com          | donor456 |
| NGO   | greenearth@ngo.com     | ngo123   |
| NGO   | helping@ngo.com        | ngo456   |

---

## 🗃️ Database Tables Summary

| Table           | Purpose                              | Key Fields                              |
|-----------------|--------------------------------------|-----------------------------------------|
| `Users`         | Both donors and NGOs                 | user_id (PK), role, email, password     |
| `Food_Items`    | Food catalogue (avoids repetition)   | item_id (PK), item_name, category       |
| `Donation_Pool` | **CORE TABLE** — all donations       | donation_id (PK), donor_id (FK), item_id (FK), status |
| `Claims`        | Tracks who claimed what              | claim_id (PK), donation_id (FK/UNIQUE), ngo_id (FK) |
| `Impact_Log`    | Analytics — meals served             | log_id (PK), donation_id (FK), meals_fed |

---

## 📋 SQL Queries Used (Viva Reference)

### 1. INSERT — Add Donation
```sql
INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status)
VALUES (?, ?, ?, ?, 'AVAILABLE');
```

### 2. SELECT + JOIN — Available Food (NGO view)
```sql
SELECT dp.donation_id, u.name AS donor_name, fi.item_name, fi.category,
       dp.quantity, dp.expiry_at, dp.status
FROM Donation_Pool dp
INNER JOIN Users      u  ON dp.donor_id = u.user_id
INNER JOIN Food_Items fi ON dp.item_id  = fi.item_id
WHERE dp.status = 'AVAILABLE' AND dp.expiry_at > NOW()
ORDER BY dp.expiry_at ASC;
```

### 3. UPDATE — Claim a Donation
```sql
UPDATE Donation_Pool
SET status = 'CLAIMED'
WHERE donation_id = ? AND status = 'AVAILABLE';
```

### 4. INSERT — Record Claim
```sql
INSERT INTO Claims (donation_id, ngo_id) VALUES (?, ?);
```

### 5. Aggregate Query — Impact Statistics
```sql
SELECT
  COUNT(*) AS total_donations,
  SUM(CASE WHEN status='CLAIMED'   THEN 1 ELSE 0 END) AS claimed_count,
  SUM(CASE WHEN status='AVAILABLE' THEN 1 ELSE 0 END) AS available_count,
  (SELECT COALESCE(SUM(meals_fed),0) FROM Impact_Log)  AS total_meals_fed
FROM Donation_Pool;
```

### 6. UPDATE — Auto-Expire Stale Donations
```sql
UPDATE Donation_Pool
SET status = 'EXPIRED'
WHERE status = 'AVAILABLE' AND expiry_at < NOW();
```

### 7. Complex JOIN — NGO Claim History (4 tables)
```sql
SELECT c.claim_id, fi.item_name, dp.quantity, u.name AS donor_name,
       c.claim_time, il.meals_fed
FROM Claims c
INNER JOIN Donation_Pool dp ON c.donation_id = dp.donation_id
INNER JOIN Food_Items    fi ON dp.item_id    = fi.item_id
INNER JOIN Users         u  ON dp.donor_id   = u.user_id
LEFT  JOIN Impact_Log    il ON c.donation_id = il.donation_id
WHERE c.ngo_id = ?
ORDER BY c.claim_time DESC;
```

---

## 🎓 Viva Q&A Cheatsheet

**Q: What is JDBC?**
A: Java Database Connectivity — a Java API that allows Java programs to connect and execute SQL queries on relational databases like MySQL.

**Q: Why PreparedStatement over Statement?**
A: PreparedStatement prevents SQL injection attacks, is pre-compiled (faster for repeated queries), and supports parameterized queries cleanly.

**Q: What is a transaction and where did you use it?**
A: A transaction groups multiple SQL operations so they all succeed or all fail together. We use it in `claimDonation()` — UPDATE + INSERT into Claims + INSERT into Impact_Log must all succeed together. If any fail, we `rollback()`.

**Q: What are the relationships in your DB?**
A: 
- One User (donor) → Many Donation_Pool records
- One User (NGO) → Many Claims
- One Donation → One Claim (enforced by UNIQUE constraint on donation_id in Claims)
- One Food_Item → Many Donations

**Q: Which is your most important table?**
A: `Donation_Pool` — it is the central table that links donors, food items, quantity, expiry, and status. All core operations (add, view, claim) revolve around it.

**Q: What normalization forms did you apply?**
A: 
- 1NF: All attributes are atomic, no repeating groups
- 2NF: Separated Food_Items table to remove partial dependency on item_name
- 3NF: No transitive dependencies — donor info is in Users, food info in Food_Items

**Q: What does `ON DELETE CASCADE` do?**
A: If a User is deleted, all their Donation_Pool records are automatically deleted too, maintaining referential integrity.

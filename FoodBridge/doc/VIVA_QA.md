# Viva Questions & Answers — FoodBridge

Common DBMS/Java project viva questions with detailed answers.

---

## 📚 Database Design Questions

### Q1: What is normalization? Which forms did you use?

**Answer:**
Normalization is a database design technique to organize tables to reduce data redundancy and improve data integrity.

**We implemented:**

**1NF (First Normal Form)**
- All attributes are atomic (indivisible)
- No repeating groups
- Example: `quantity` is stored as text "5 kg", not as separate `amount` and `unit` columns

**2NF (Second Normal Form)**
- Satisfies 1NF
- No partial dependencies
- Every non-key attribute fully depends on the entire primary key
- Example: We separated `Food_Items` table instead of storing item_name in every donation

**3NF (Third Normal Form)**
- Satisfies 2NF
- No transitive dependencies
- Non-key attributes don't depend on other non-key attributes
- Example: Donor name is in `Users` table, not repeated in `Donation_Pool`

**Why?** 
- ✅ Reduces data redundancy (less storage, less inconsistency)
- ✅ Maintains data integrity (one place to update)
- ✅ Improves query performance (smaller tables)

---

### Q2: What are your database tables and their relationships?

**Answer:**

5 tables:

1. **Users** - Both donors and NGOs (role discrimination)
   - PK: user_id
   - 1 User → Many Donations

2. **Food_Items** - Catalogue (avoids repetition)
   - PK: item_id
   - 1 Item → Many Donations

3. **Donation_Pool** - Core table, all donations
   - PK: donation_id
   - FK: donor_id → Users
   - FK: item_id → Food_Items
   - 1 Donation → 1 Claim (UNIQUE constraint)

4. **Claims** - Tracks who claimed what
   - PK: claim_id
   - FK: donation_id → Donation_Pool (UNIQUE)
   - FK: ngo_id → Users

5. **Impact_Log** - Analytics/metrics
   - PK: log_id
   - FK: donation_id → Donation_Pool (UNIQUE)

**Relationships:**
```
Users (1) ─→ (Many) Donation_Pool
         ─→ (Many) Claims

Donation_Pool (1) ─→ (1) Claims
Donation_Pool (1) ─→ (1) Impact_Log
```

---

### Q3: What is ON DELETE CASCADE? Where did you use it?

**Answer:**
`ON DELETE CASCADE` ensures referential integrity by automatically deleting child records when parent is deleted.

**Usage in FoodBridge:**

```sql
-- When donor is deleted, all their donations are deleted
FOREIGN KEY (donor_id) REFERENCES Users(user_id) ON DELETE CASCADE

-- When donation is deleted, related claim is deleted
FOREIGN KEY (donation_id) REFERENCES Donation_Pool(donation_id) ON DELETE CASCADE

-- When donation is deleted, impact log is deleted
FOREIGN KEY (donation_id) REFERENCES Donation_Pool(donation_id) ON DELETE CASCADE
```

**Example:**
```
DELETE FROM Users WHERE user_id = 1;  -- Donor deletion

Cascading effects:
→ All Donation_Pool records with donor_id=1 are deleted
→ All Claims records referencing those donations are deleted
→ All Impact_Log records for those donations are deleted
```

**Why important?**
- ✅ Prevents orphaned records (claims without donations)
- ✅ Maintains data consistency
- ✅ No manual cleanup needed

---

### Q4: What constraints did you use and why?

**Answer:**

| Constraint | Example | Purpose |
|-----------|---------|---------|
| PRIMARY KEY | `user_id INT AUTO_INCREMENT PRIMARY KEY` | Unique identifier, enforces uniqueness |
| UNIQUE | `email VARCHAR(100) UNIQUE` | No duplicate emails |
| NOT NULL | `name VARCHAR(100) NOT NULL` | Required fields |
| FOREIGN KEY | `donor_id INT FK → Users` | Data integrity, prevents invalid references |
| DEFAULT | `created_at DATETIME DEFAULT NOW()` | Auto-populate timestamps |
| AUTO_INCREMENT | `user_id INT AUTO_INCREMENT` | Auto-generate sequential IDs |
| ENUM | `status ENUM('AVAILABLE','CLAIMED','EXPIRED')` | Restrict to valid values |
| CHECK | `expiry_at > NOW()` (implicit) | Validate data |

---

## 📋 SQL Query Questions

### Q5: What types of SQL queries did you use?

**Answer:**

We demonstrated:

1. **INSERT** - Add donations, register users
   ```sql
   INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status)
   VALUES (?, ?, ?, ?, 'AVAILABLE');
   ```

2. **SELECT with WHERE** - Login verification
   ```sql
   SELECT * FROM Users WHERE email = ? AND password = ? AND role = ?;
   ```

3. **SELECT with JOIN** - View donations with donor name, food name
   ```sql
   SELECT dp.*, u.name, fi.item_name FROM Donation_Pool dp
   INNER JOIN Users u ON dp.donor_id = u.user_id
   INNER JOIN Food_Items fi ON dp.item_id = fi.item_id;
   ```

4. **UPDATE** - Mark donation as claimed
   ```sql
   UPDATE Donation_Pool SET status = 'CLAIMED' 
   WHERE donation_id = ? AND status = 'AVAILABLE';
   ```

5. **Aggregate Functions** - Impact statistics
   ```sql
   SELECT COUNT(*), SUM(meals_fed) FROM ...;
   ```

6. **Transaction** - Atomic claim operation
   - All three operations succeed or all fail

---

### Q6: Explain your most complex query

**Answer:**

**NGO Claim History Query (4 JOINs + LEFT JOIN):**

```sql
SELECT c.claim_id, fi.item_name, dp.quantity, u.name AS donor_name,
       c.claim_time, il.meals_fed
FROM Claims c 
INNER JOIN Donation_Pool dp ON c.donation_id = dp.donation_id 
INNER JOIN Food_Items fi ON dp.item_id = fi.item_id 
INNER JOIN Users u ON dp.donor_id = u.user_id 
LEFT JOIN Impact_Log il ON c.donation_id = il.donation_id 
WHERE c.ngo_id = ? 
ORDER BY c.claim_time DESC;
```

**Why complex?**
- 5 tables involved
- 4 INNER JOINs (required) + 1 LEFT JOIN (optional)
- Multiple relationship paths
- Sorting and filtering

**What it does:**
- Retrieves all claims made by an NGO
- Shows food item name (from Food_Items)
- Shows donor name (from Users)
- Shows meals fed (from Impact_Log, may not exist)
- Ordered by latest first

---

### Q7: What is a transaction? Where did you use it?

**Answer:**

A transaction is a sequence of operations that must all succeed or all fail (ACID principle).

**We used it in `claimDonation()`:**

```java
conn.setAutoCommit(false);  // BEGIN TRANSACTION

try {
    // Query 1: Update donation status
    ps1.executeUpdate();  // UPDATE status = 'CLAIMED'
    
    // Query 2: Insert claim record
    ps2.executeUpdate();  // INSERT INTO Claims
    
    // Query 3: Insert impact log
    ps3.executeUpdate();  // INSERT INTO Impact_Log
    
    conn.commit();  // SUCCESS: All changes saved
    
} catch (SQLException e) {
    conn.rollback();  // FAILURE: All changes reverted
}
```

**Why?**
- ✅ Prevents partial updates
- ✅ If any operation fails, donation remains AVAILABLE for others
- ✅ Maintains data consistency

**Example scenario:**
```
Scenario 1 (Success):
→ Donation marked CLAIMED
→ Claim record created
→ Impact logged
✓ All changes saved

Scenario 2 (Partial failure - database error during INSERT):
→ Donation marked CLAIMED ✓
→ Claim record INSERT fails ✗
→ ROLLBACK: Donation status reverted to AVAILABLE
✓ No orphaned data
```

---

## 🔌 JDBC & Java Questions

### Q8: What is JDBC? Why PreparedStatement over Statement?

**Answer:**

**JDBC** = Java Database Connectivity  
API that allows Java programs to interact with databases (MySQL, PostgreSQL, etc.)

**Why PreparedStatement over Statement?**

| Feature | Statement | PreparedStatement |
|---------|-----------|------------------|
| SQL Injection | ❌ Vulnerable | ✅ Safe |
| Performance | Compiled each time | Pre-compiled, reused |
| Parameter Binding | Manual string concatenation | Safe parameterized |
| Example | Vulnerable | Safe |

**Vulnerable code (Statement):**
```java
String sql = "SELECT * FROM Users WHERE email = '" + email + "' AND password = '" + password + "'";
// If email = "admin' OR '1'='1", query becomes:
// SELECT * FROM Users WHERE email = 'admin' OR '1'='1' AND password = '...'
// ↑ Logs in as ANY user!
```

**Safe code (PreparedStatement):**
```java
String sql = "SELECT * FROM Users WHERE email = ? AND password = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, email);      // email parameter
ps.setString(2, password);   // password parameter
// Even if email = "admin' OR '1'='1'", it's treated as literal string
```

**We used PreparedStatement everywhere for security.**

---

### Q9: Explain your connection management

**Answer:**

**DBConnection.java - Singleton Pattern:**

```java
public class DBConnection {
    private static Connection connection = null;  // Single instance
    
    static {
        // Load driver class on startup
        Class.forName("com.mysql.cj.jdbc.Driver");
    }
    
    public static Connection getConnection() {
        try {
            // Reuse existing connection if alive
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] Driver not found");
        } catch (SQLException e) {
            System.err.println("[ERROR] Cannot connect");
        }
        return connection;
    }
    
    public static void closeConnection() {
        if (connection != null) {
            connection.close();
        }
    }
}
```

**Features:**
- ✅ Single connection reused (Singleton pattern)
- ✅ Connection testing before reuse (isClosed())
- ✅ Lazy initialization (created when first needed)
- ✅ Error handling (missing driver, connection failed)
- ✅ Graceful shutdown (closeConnection())

---

### Q10: What is try-with-resources?

**Answer:**

Java feature that auto-closes resources (connection, statement, result set).

**Before (manual closing):**
```java
Connection con = DBConnection.getConnection();
PreparedStatement ps = con.prepareStatement(sql);
try {
    ResultSet rs = ps.executeQuery();
    // Use rs
} finally {
    ps.close();  // Manual close
    // Note: We DON'T close connection (reused)
}
```

**After (try-with-resources):**
```java
try (Connection con = DBConnection.getConnection();
     PreparedStatement ps = con.prepareStatement(sql)) {
    ResultSet rs = ps.executeQuery();
    // Use rs
}
// Auto-closes ps and con
```

**Benefits:**
- ✅ No resource leaks
- ✅ Exception-safe (closes even if exception thrown)
- ✅ Cleaner, more readable code

**We used it everywhere in DAOs.**

---

## 🏗️ Architecture & Design Questions

### Q11: How is your project structured?

**Answer:**

**3-Tier Architecture:**

```
UI Layer → DAO Layer → Database
```

1. **UI Layer** (`Main.java`, `DonorUI.java`, `NGOUI.java`)
   - User interaction (console menus)
   - No database code directly

2. **DAO Layer** (`UserDAO.java`, `DonationDAO.java`, `DBConnection.java`)
   - All SQL queries
   - Database operations
   - Insulates UI from DB changes

3. **Model Layer** (`User.java`, `Donation.java`)
   - Entity objects
   - DataType mapping

4. **Database Layer** (MySQL)
   - Tables, constraints, indexes

**Benefits:**
- ✅ Maintains separation of concerns
- ✅ If database changes, only DAO needs update
- ✅ If UI changes, DAOs unaffected
- ✅ Easy to test each layer

---

### Q12: What is the DAO pattern?

**Answer:**

**DAO** = Data Access Object  
Design pattern that encapsulates all database operations in separate classes.

**Without DAO:**
```java
// Main.java
Connection con = DriverManager.getConnection(...);
Statement stmt = con.createStatement();
ResultSet rs = stmt.executeQuery(...);
// ... 50 lines of DB code scattered throughout
```

**With DAO:**
```java
// UserDAO.java
public User login(String email, String password, String role) {
    // All DB logic here, 20 lines
}

// Main.java
User user = userDAO.login(email, password, role);
// Clean, simple call
```

**Benefits:**
- ✅ Separation of concerns
- ✅ DB code in one place (easy to maintain)
- ✅ Can swap databases (just rewrite DAO)
- ✅ Easy to test

---

## 🎯 Functionality & Features Questions

### Q13: Explain your complete workflow from donation to claim

**Answer:**

**Donor Flow:**
1. Donor registers/logs in
2. Views donation dashboard
3. Clicks "Add Donation"
4. Enters: food item, quantity, expiry time
5. System:
   - Looks up food item in catalogue
   - Creates entry in Donation_Pool
   - Sets status = 'AVAILABLE'
6. Donation listed as available

**NGO Flow:**
1. NGO logs in
2. Clicks "View Available Food"
3. System:
   - Auto-expires old donations (UPDATE)
   - Queries available donations (SELECT with JOINs)
   - Shows list with donor name, expiry time, quantity
4. NGO clicks "Claim Donation"
5. System (in transaction):
   - UPDATE Donation_Pool: status = 'CLAIMED'
   - INSERT into Claims: which NGO claimed it
   - INSERT into Impact_Log: meals fed estimate
   - All succeed or all rollback

**Result:**
- Donation status: CLAIMED
- NGO can see it in "Claim History"
- Platform shows "1 meal claimed" in impact stats

---

### Q14: How does auto-expiry work?

**Answer:**

Called when donor/NGO logs in:

```java
public void autoExpireDonations() {
    UPDATE Donation_Pool 
    SET status = 'EXPIRED' 
    WHERE status = 'AVAILABLE' AND expiry_at < NOW();
}
```

**Example:**
```
Sample donations:
- Donation 1: expiry = 2 hours ago, status = AVAILABLE
- Donation 2: expiry = 1 hour from now, status = AVAILABLE
- Donation 3: expiry = 1 day ago, status = CLAIMED

Auto-expire runs:
→ Donation 1: NOW() > expiry_at ✓ → Status = EXPIRED
→ Donation 2: NOW() < expiry_at ✗ → No change
→ Donation 3: Status != AVAILABLE ✗ → No change

Result:
- Donation 1: EXPIRED (wasted)
- Donation 2: AVAILABLE
- Donation 3: CLAIMED
```

**Why?**
- ✅ Realistic simulation (food has limited shelf life)
- ✅ Prevents NGOs from claiming expired food
- ✅ Tracks waste (EXPIRED donations in impact stats)

---

## 🧪 Testing & Demonstration Questions

### Q15: How would you demonstrate the project to faculty?

**Answer:**

**Demo flow (5 minutes):**

1. **Start App**
   - Show splash screen
   - Show main menu

2. **Donor Login**
   - Email: `saravana@donor.com`
   - Password: `donor123`
   - Show: "Welcome back, Hotel Saravana Bhavan!"

3. **Add Donation**
   - Add: "Biryani", "5 kg", "2026-04-05 18:00"
   - Show: Donation created with ID
   - Show printed SQL query

4. **View My Donations**
   - Show table with all donations
   - Show statuses (AVAILABLE, CLAIMED, EXPIRED)

5. **Logout and NGO Login**
   - Email: `greenearth@ngo.com`
   - Password: `ngo123`

6. **View Available Food**
   - Show filtered list (only AVAILABLE, not expired)
   - Show multi-table JOIN working (donor name, food name)

7. **Claim Donation**
   - Claim donation ID 1
   - Confirm claim
   - Show: "Donation claimed successfully! Impact logged."
   - Explain: This was a TRANSACTION with rollback support

8. **View Claim History**
   - Show claimed donation with all details
   - Show meals fed calculation

9. **Impact Statistics**
   - Show COUNT, SUM queries working
   - Show: Total donations, claimed, available, expired, meals fed

10. **Show Database**
    - Open MySQL Workbench
    - Show data was actually inserted/updated
    - Show relationships between tables

---

### Q16: What would you improve in production?

**Answer:**

**Current Demo Limitations:**

1. **Security**
   - Plain-text passwords → Use bcrypt/SHA-256 hashing
   - No user authentication beyond login → Add JWT tokens
   - Hard-coded DB credentials → Use environment variables

2. **Scalability**
   - Single connection → Use connection pooling (HikariCP)
   - Console UI → Build web UI (Spring Boot + React)
   - No API → Build REST API

3. **Data**
   - Quantity as text ("5 kg") → Use separate amount + unit columns
   - Meals estimate hardcoded × 2 → ML weights per food type
   - No images → Add food photos

4. **Features**
   - No notifications → Email when donation claimed
   - No search/filter → Add donation search
   - No ratings → Add donor/NGO ratings
   - No donations edit → Allow donors to modify

5. **Operations**
   - No logging → SLF4J/Logback for structured logs
   - No monitoring → Application Performance Monitoring (APM)
   - No automated tests → JUnit + Mockito unit tests
   - No CI/CD → GitHub Actions, Docker containers

---

## 🎓 Final Tips for Viva

**What to emphasize:**

✅ **3-Tier Architecture** - Show separation of concerns  
✅ **Normalization** - Explain 1NF, 2NF, 3NF benefits  
✅ **Transactions** - Explain ACID properties with your claim example  
✅ **JOINs** - Show complex query joins with real output  
✅ **Security** - PreparedStatement prevents SQL injection  
✅ **Design Patterns** - Singleton (DBConnection), DAO (UserDAO/DonationDAO)  
✅ **DBMS Concepts** - Foreign keys, constraints, cascading deletes  
✅ **Live Demo** - Show actual queries running, see results  

**What to avoid:**

❌ Don't just recite code - explain logic  
❌ Don't say "I copied from internet" - own your work  
❌ Don't ignore questions - ask for clarification  
❌ Don't be defensive - bugs are learning opportunities  
❌ Don't over-explain - be concise and clear  

**Practice answering:**
- "Why did you use MySQL instead of MongoDB?"
- "How would you handle 1 million donations?"
- "What happens if two NGOs claim same donation simultaneously?"
- "Why use transactions instead of three separate queries?"

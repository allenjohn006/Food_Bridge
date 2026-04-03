# Project Architecture — FoodBridge

Complete technical architecture, design patterns, and system design documentation.

---

## 🏗️ High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                    USER INTERFACE LAYER              │
│                  (Console-based)                     │
│  ┌──────────────────┐      ┌──────────────────┐     │
│  │   Main.java      │      │   DonorUI.java   │     │
│  │ (Login/Register) │      │  (Donor Menu)    │     │
│  └──────────────────┘      └──────────────────┘     │
│  ┌──────────────────┐                               │
│  │   NGOUI.java     │                               │
│  │  (NGO Menu)      │                               │
│  └──────────────────┘                               │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                     DAO LAYER                        │
│           (Data Access Objects)                      │
│  ┌──────────────────────────────────────────────┐   │
│  │        UserDAO.java                          │   │
│  │  • login(email, password, role)              │   │
│  │  • register(name, role, phone, email, pass)  │   │
│  └──────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────┐   │
│  │        DonationDAO.java                      │   │
│  │  • addDonation()                             │   │
│  │  • getDonorDonations()                       │   │
│  │  • getAvailableDonations()                   │   │
│  │  • claimDonation() [TRANSACTION]             │   │
│  │  • printNGOClaims()                          │   │
│  │  • printImpactStats()                        │   │
│  │  • autoExpireDonations()                     │   │
│  └──────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────┐   │
│  │        DBConnection.java                     │   │
│  │  • getConnection() [Singleton]               │   │
│  │  • closeConnection()                         │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                   MODEL LAYER                        │
│            (Entity/Data Models)                      │
│  ┌──────────────────┐      ┌──────────────────┐     │
│  │   User.java      │      │ Donation.java    │     │
│  │ • userId         │      │ • donationId     │     │
│  │ • name           │      │ • donorId        │     │
│  │ • role           │      │ • foodItem       │     │
│  │ • phone          │      │ • quantity       │     │
│  │ • email          │      │ • expiry_at      │     │
│  └──────────────────┘      │ • status         │     │
│                             └──────────────────┘     │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                  DATABASE LAYER                      │
│                  (MySQL)                             │
│  ┌──────────────────────────────────────────────┐   │
│  │  Schemas & Tables:                           │   │
│  │  • Users                                      │   │
│  │  • Food_Items                                │   │
│  │  • Donation_Pool                             │   │
│  │  • Claims                                     │   │
│  │  • Impact_Log                                │   │
│  │                                              │   │
│  │  Relationships:                              │   │
│  │  • Foreign Keys                              │   │
│  │  • Indexes on lookup columns                 │   │
│  │  • Constraints (UNIQUE, NOT NULL)            │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

---

## 🎯 Three-Tier Architecture (3-Tier)

### Tier 1: Presentation Layer (UI)
- **Files:** `Main.java`, `DonorUI.java`, `NGOUI.java`
- **Responsibility:** Console-based user interaction
- **Methods:** Menu display, input handling, output formatting
- **Independence:** No direct database access

### Tier 2: Business Logic Layer (DAO)
- **Files:** `UserDAO.java`, `DonationDAO.java`, `DBConnection.java`
- **Responsibility:** Execute all database operations
- **Methods:** Query preparation, result mapping, transaction handling
- **Independence:** Abstracts database details from UI

### Tier 3: Data Layer (Database)
- **Technology:** MySQL
- **Responsibility:** Data storage and retrieval
- **Objects:** Tables, indexes, constraints, relationships

### Benefits
✅ **Separation of Concerns** - Each tier has single responsibility  
✅ **Maintainability** - Changes in DB don't affect UI  
✅ **Testability** - Each tier can be tested independently  
✅ **Scalability** - Easy to extend each tier  

---

## 🔗 Data Flow

### Example 1: Donor Login Flow

```
User Input
    ↓
Main.java → login()
    ↓
UserDAO.java → login(email, password, role)
    ↓
DBConnection.java → getConnection()
    ↓
MySQL: SELECT * FROM Users WHERE email=? AND password=? AND role=?
    ↓
User.java → Construct User object
    ↓
DonorUI.java → Display dashboard
```

### Example 2: Add Donation Flow

```
User Input (Food item, quantity, expiry)
    ↓
DonorUI.java → addDonation()
    ↓
DonationDAO.java → addDonation(donorId, itemName, quantity, expiry)
    ↓
Helper: getOrCreateFoodItem(itemName)
    ↓
MySQL INSERT into Donation_Pool
    ↓
Return donation_id
    ↓
DonorUI → Display success message
```

### Example 3: Claim Donation (Transaction)

```
User Input (Donation ID)
    ↓
NGOUI.java → claimDonation()
    ↓
DonationDAO.java → claimDonation(donationId, ngoId)
    ↓
conn.setAutoCommit(false) [BEGIN TRANSACTION]
    ↓
Query 1: UPDATE Donation_Pool SET status='CLAIMED'
    ↓
Query 2: INSERT INTO Claims
    ↓
Query 3: INSERT INTO Impact_Log
    ↓
conn.commit() [ALL OR NOTHING]
    ↓
NGOUI → Display success message
```

---

## 🏛️ Design Patterns Used

### Pattern 1: Singleton Pattern (DBConnection)

**Purpose:** Ensure only one database connection exists

```java
public class DBConnection {
    private static Connection connection = null;  // Singleton instance
    
    public static Connection getConnection() {
        if (connection == null || connection.isClosed()) {
            // Create connection only when needed
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }
}
```

**Benefits:**
- ✅ Single connection reused across application
- ✅ Resource efficient
- ✅ Thread-safe (in this demo, single-threaded)

### Pattern 2: DAO Pattern (Data Access Object)

**Purpose:** Encapsulate database operations

```java
public class UserDAO {
    public User login(String email, String password, String role) { ... }
    public boolean register(String name, String role, String phone, String email, String password) { ... }
}

public class DonationDAO {
    public boolean addDonation(int donorId, String itemName, String quantity, String expiryAt) { ... }
    public List<Donation> getDonorDonations(int donorId) { ... }
    // ... more methods
}
```

**Benefits:**
- ✅ Separation of database logic from business logic
- ✅ Easy to change database later (just rewrite DAOs)
- ✅ Reusable across multiple UI layers

### Pattern 3: Model/Entity Pattern

**Purpose:** Represent data entities

```java
public class User {
    private int userId;
    private String name;
    private String role;
    private String phone;
    private String email;
    // Getters
}

public class Donation {
    private int donationId;
    private int donorId;
    private String donorName;
    // ... more fields
    // Getters
}
```

**Benefits:**
- ✅ Type-safe object representation
- ✅ Easy data mapping from ResultSet
- ✅ Clear contract between layers

### Pattern 4: Try-with-resources (Resource Management)

**Purpose:** Auto-close database resources

```java
try (Connection con = DBConnection.getConnection();
     PreparedStatement ps = con.prepareStatement(sql)) {
    // Use connection and statement
    ps.setString(1, email);
    ResultSet rs = ps.executeQuery();
} catch (SQLException e) {
    e.printStackTrace();
}
// Automatically closed after try block
```

**Benefits:**
- ✅ No resource leaks
- ✅ Cleaner code
- ✅ Exception-safe cleanup

### Pattern 5: Transaction Pattern

**Purpose:** Atomic multi-operation updates

```java
try {
    conn.setAutoCommit(false);  // BEGIN TRANSACTION
    
    // Operation 1
    UPDATE...
    
    // Operation 2
    INSERT...
    
    // Operation 3
    INSERT...
    
    conn.commit();  // COMMIT all
} catch (Exception e) {
    conn.rollback();  // ROLLBACK all
}
```

**Benefits:**
- ✅ All-or-nothing operations
- ✅ Data consistency
- ✅ No partial updates

---

## 📦 Class Diagram

```
┌──────────────────┐
│      Main        │
├──────────────────┤
│ + main()         │
│ + login()        │
│ + register()     │
│ + printBanner()  │
└────────┬─────────┘
         │
    uses │
         ├─────→ UserDAO
         ├─────→ DonorUI
         └─────→ NGOUI
         
┌──────────────────┐      ┌────────────────┐
│   UserDAO        │      │  DonationDAO   │
├──────────────────┤      ├────────────────┤
│ + login()        │      │ + addDonation()        │
│ + register()     │      │ + getDonorDonations()  │
└────────┬─────────┘      │ + getAvailableDonations() │
         │                │ + claimDonation()      │
         │                │ + printNGOClaims()     │
         │                │ + printImpactStats()   │
         │                │ + autoExpireDonations()│
         │                │ - getOrCreateFoodItem()│
         │                └────────┬──────────────┘
         │                         │
         └────────┬────────────────┘
                  │
            uses  │
                  └─────→ DBConnection
                          (Singleton)
                          
┌──────────────────┐
│      User        │
├──────────────────┤
│ - userId: int    │
│ - name: String   │
│ - role: String   │
│ - phone: String  │
│ - email: String  │
├──────────────────┤
│ + getters()      │
└──────────────────┘

┌──────────────────┐
│   Donation       │
├──────────────────┤
│ - donationId: int│
│ - donorId: int   │
│ - donorName: String │
│ - foodItem: String  │
│ - quantity: String  │
│ - expiry_at: String │
│ - status: String    │
├──────────────────┤
│ + getters()      │
└──────────────────┘
```

---

## 🔄 Control Flow: Complete User Session

```
START
  ↓
Main.main()
  ↓
printBanner()
  ↓
┌─────────────────────────────────────┐
│  MAIN MENU LOOP                     │
│  ┌──────────────────────────────┐   │
│  │ 1. Login                     │   │
│  │ 2. Register                  │   │
│  │ 3. Exit                      │   │
│  └──────────────────────────────┘   │
│         ↓                            │
│    [User choice]                    │
│         ↓                            │
│  ┌──────┬──────┬──────┐             │
│  ↓      ↓      ↓      ↓             │
│ [1]   [2]    [3]   [else]           │
│  │      │      │      │             │
│  ↓      ↓      ↓      ↓             │
│ login reg  exit invalid             │
└─────────────────────────────────────┘
  │
  ├─→ LOGIN
  │    │
  │    ├─→ UserDAO.login()
  │    │    [Query 1: SELECT]
  │    │
  │    ├─→ Success: Load DonorUI or NGOUI
  │    │    │
  │    │    └─→ ┌─────────────────────┐
  │    │        │ DONOR UI LOOP       │
  │    │        ├─────────────────────┤
  │    │        │ 1. Add Donation     │ → Query 3: INSERT
  │    │        │ 2. View Donations   │ → Query 4: SELECT+JOIN
  │    │        │ 3. Impact Stats     │ → Query 10: Aggregate
  │    │        │ 4. Logout           │
  │    │        └─────────────────────┘
  │    │
  │    └─→ Or: ┌─────────────────────┐
  │            │ NGO UI LOOP         │
  │            ├─────────────────────┤
  │            │ 1. View Available   │ → Query 5: SELECT+2JOIN
  │            │ 2. Claim Donation   │ → Queries 6,7,8 [TRANSACTION]
  │            │ 3. Claim History    │ → Query 9: SELECT+4JOIN
  │            │ 4. Impact Stats     │ → Query 10: Aggregate
  │            │ 5. Logout           │
  │            └─────────────────────┘
  │
  ├─→ REGISTER
  │    │
  │    └─→ UserDAO.register()
  │         [Query 2: INSERT]
  │
  └─→ EXIT
       │
       └─→ "Goodbye!" → quit()
```

---

## 🔐 Error Handling Strategy

### Connection Level

```java
public static Connection getConnection() {
    try {
        // Try to reuse existing connection
        if (connection == null || connection.isClosed()) {
            // Create new one
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
    } catch (ClassNotFoundException e) {
        // JDBC driver missing
        System.err.println("[ERROR] MySQL Driver not found");
    } catch (SQLException e) {
        // Connection failed
        System.err.println("[ERROR] Cannot connect to MySQL");
    }
    return connection;  // May be null
}
```

### Query Level

```java
public User login(String email, String password, String role) {
    Connection con = DBConnection.getConnection();
    
    if (con == null) {
        System.err.println("[ERROR] Database connection unavailable");
        return null;  // Graceful failure
    }
    
    try {
        // Execute query
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return null;  // Login failed
}
```

### Transaction Level

```java
public boolean claimDonation(int donationId, int ngoId) {
    Connection con = null;
    try {
        con = DBConnection.getConnection();
        if (con == null) return false;
        
        con.setAutoCommit(false);
        // Operations...
        con.commit();  // All succeeded
        return true;
        
    } catch (SQLException e) {
        try {
            con.rollback();  // All failed, revert
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        e.printStackTrace();
    }
    return false;
}
```

---

## 🎓 Key Architecture Decisions

| Decision | Why | Trade-off |
|----------|-----|-----------|
| 3-Tier Architecture | Separation of concerns | More classes to manage |
| Singleton DBConnection | Resource efficiency | Less flexible for multi-threading |
| DAO Pattern | Database independence | Slightly more code upfront |
| Console UI | Simple, no web framework | Limited UI capabilities |
| PlainText passwords | Demo simplicity | Security risk (use hashing in production) |
| Transaction on claim | Data consistency | Slightly slower than non-atomic |

---

## 🚀 Production Improvements

Would make these changes for production:

1. **Password Hashing** - Use bcrypt/SHA-256 instead of plain text
2. **Connection Pooling** - Use HikariCP or C3P0 for multiple connections
3. **Configuration** - Externalize DB credentials to config file
4. **Web Framework** - Use Spring Boot instead of Console UI
5. **API Layer** - REST API instead of direct method calls
6. **Logging** - Use SLF4J/Logback instead of System.out
7. **Web UI** - HTML/CSS/JavaScript for better UX
8. **Caching** - Redis for frequently accessed data
9. **Testing** - JUnit, Mockit for unit/integration tests
10. **Monitoring** - Application performance monitoring (APM)

---

## 📊 Performance Characteristics

| Operation | Complexity | Time |
|-----------|-----------|------|
| Login | O(1) | ~10-50ms |
| Add Donation | O(1) | ~20-100ms |
| List Donations | O(n) | ~50-200ms |
| Claim (Transaction) | O(1) | ~50-150ms |
| Impact Stats | O(n) | ~50-200ms |

*Assuming small dataset (~100 donations). Scale with database size.*

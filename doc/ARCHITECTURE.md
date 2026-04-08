# 🏗️ FoodBridge System Architecture

## 🎯 Architecture Overview

FoodBridge follows a **layered architecture** with two parallel interfaces sharing a single MySQL database.

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
├──────────────────────┬──────────────────────────────────────┤
│   Web Interface      │      Terminal Interface              │
│  - HTML 5/CSS3       │   - Scanner-based CLI               │
│  - Vanilla JS        │   - Menu-driven UX                   │
│  - Real-time Polling │   - Table formatting                 │
│  (app.js)            │   - DonorUI, NGOUI                  │
│                      │                                      │
│  http://localhost    │   java -cp out foodbridge.Main       │
│        :8080         │                                      │
└──────────────────────┴──────────────────────────────────────┘
                           ↓↑
┌──────────────────────────────────────────────────────────────┐
│                   APPLICATION LAYER                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Spring Boot REST API        │   CLI User Interface         │
│  (ApiController.java)         │   (DonorUI.java, NGOUI.java)│
│                               │                              │
│  12+ REST Endpoints:          │   Menu Options:              │
│  ├─ /api/auth/*              │   ├─ 1. Login                │
│  ├─ /api/donations           │   ├─ 2. Register             │
│  ├─ /api/claims              │   ├─ 3. Add Donation         │
│  ├─ /api/ngo/requests        │   ├─ 4. View Donations       │
│  ├─ /api/stats               │   └─ 5. Exit                 │
│  └─ /api/admin/*             │                              │
│                               │   Data Processing:           │
│  Handling:                     │   DonationDAO logic runs on  │
│  ├─ Request validation        │   user selection             │
│  ├─ Auto-expiry logic         │                              │
│  ├─ Business logic            │                              │
│  └─ Response serialization    │                              │
│                               │                              │
│           FoodBridgeService.java (Core Business Logic)       │
│           ├─ addDonation()                                  │
│           ├─ claimDonation()                                │
│           ├─ autoExpireDonations()                          │
│           ├─ getAvailableDonations()                        │
│           └─ getImpactStats()                               │
└──────────────────────────────────────────────────────────────┘
                           ↓↑
┌──────────────────────────────────────────────────────────────┐
│                    DATA ACCESS LAYER                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  UserDAO                    │     DonationDAO              │
│  ├─ login()                 │     ├─ addDonation()         │
│  ├─ register()              │     ├─ getDonations()        │
│  └─ getUser()               │     ├─ claimDonation()       │
│                             │     ├─ getImpactStats()      │
│  DBConnection Singleton     │     └─ autoExpire()          │
│  └─ getConnection()         │                              │
│  └─ closeConnection()       │     (All use JDBC +          │
│                             │      PreparedStatements)     │
└──────────────────────────────────────────────────────────────┘
                           ↓↑
┌──────────────────────────────────────────────────────────────┐
│                 DATABASE LAYER (MySQL 8.0)                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Users ──→ Donation_Pool ──→ Claims ──→ Impact_Log          │
│                  ↓                                           │
│            Food_Items                                        │
│                  ↓                                           │
│            NGO_Requests                                      │
│                                                              │
│  6 Tables, 1 Database: foodbridge_db                        │
│  ├─ 12+ Indexes (on FK, PK, expiry_at)                      │
│  ├─ Referential Integrity (ON DELETE CASCADE)               │
│  ├─ Transactions support (for claim operations)             │
│  └─ Auto-increment ID generation                           │
└──────────────────────────────────────────────────────────────┘
```

---

## 📊 Detailed Component Breakdown

### 1. **Web Interface (Frontend)**

**Location:** `src/main/resources/static/`

**Files:**
- `index.html` — Login/Register page
- `donor.html` — Donor dashboard
- `ngo.html` — NGO dashboard
- `admin.html` — Admin analytics
- `app.js` — Frontend logic (polling, API calls, state)
- `styles.css` — Responsive styling

**Key Features:**
- Vanilla JavaScript (no frameworks for simplicity)
- Fetch API for HTTP requests
- Real-time polling: `setInterval(loadDonations, 2000)`
- localStorage for session management
- Dynamic table rendering with innerHTML

**Data Flow:**
```
User Action (Click "Claim")
    ↓
JavaScript Event Handler
    ↓
Fetch API POST to /api/donations/{id}/claim
    ↓
Wait for response
    ↓
Update UI: show success message
    ↓
Trigger loadDonations() to refresh list
    ↓
User sees donation removed (after polling cycle)
```

---

### 2. **Backend Application (Spring Boot)**

**Location:** `src/main/java/com/foodbridge/web/`

#### **FoodBridgeWebApplication.java**
- `@SpringBootApplication` entry point
- Starts embedded Tomcat on port 8080
- Auto-configures JDBC connection pool (HikariCP)
- Serves static files from `src/main/resources/static/`

#### **ApiController.java** (REST Endpoints)
```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    // Authentication
    POST /api/auth/register   → UserDAO.register()
    POST /api/auth/login      → UserDAO.login()
    GET  /api/me              → Return session user
    
    // Donations
    GET  /api/donations                → All donations
    GET  /api/donations/available      → Unclaimed + non-expired
    POST /api/donations                → FoodBridgeService.addDonation()
    GET  /api/donations/donor/{id}     → User's donations
    POST /api/donations/{id}/claim     → FoodBridgeService.claimDonation()
    
    // NGO Operations
    GET  /api/requests/open            → Open NGO requests
    POST /api/ngo/requests             → FoodBridgeService.createNgoRequest()
    GET  /api/ngo/{id}/claims          → NGO's claims
    
    // Admin
    GET  /api/stats                    → Platform statistics
    GET  /api/admin/users              → All users
    GET  /api/admin/donations          → All donations
    POST /api/admin/expire             → Trigger expiry
}
```

#### **FoodBridgeService.java** (Business Logic)
```java
@Service
public class FoodBridgeService {
    
    // Core Operations (all with validation + auto-expiry)
    public void addDonation(AddDonationRequest req)
    public void claimDonation(int donationId, int ngoId)
    public List<Map> getAvailableDonations()
    public List<Map> getDonations()
    public Map getPlatformStats()
    
    // Helper Methods
    private void autoExpireDonations()      // Run before every SELECT
    private int getOrCreateFoodItem(name)  // Idempotent lookup
}
```

**Key Pattern:**
- Every read operation calls `autoExpireDonations()` first
- `claimDonation()` is marked `@Transactional` (UPDATE + INSERT together)
- All queries use PreparedStatements (no SQL injection)

#### **DatabaseBootstrap.java**
- Runs on startup (Spring lifecycle)
- Checks if tables exist
- If not, creates schema from inline SQL
- Inserts sample data (demo users, donations)

#### **DTO Classes** (Data Transfer Objects)
```java
record LoginRequest(String email, String password, String role) {}
record RegisterRequest(String name, String email, String password, String phone, String role) {}
record AddDonationRequest(int donorId, String itemName, String quantity, String expiryAt, Integer requestId) {}
```

---

### 3. **Data Access Layer (CLI)**

**Location:** `src/foodbridge/dao/`

#### **DBConnection.java** (Singleton)
```java
public class DBConnection {
    private static Connection connection = null;
    
    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
    }
    
    public static Connection getConnection() {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }
}
```

**Why Singleton?**
- Single database connection (connection pooling in production)
- Reused across all DAO calls
- Cleaner than passing connection everywhere

#### **UserDAO.java** (Authentication)
```java
public class UserDAO {
    
    public User login(String email, String password, String role) {
        String sql = "SELECT * FROM Users WHERE email=? AND password=? AND role=?";
        // Return User object or null
    }
    
    public boolean register(String name, String role, String phone, String email, String password) {
        String sql = "INSERT INTO Users (name, role, phone, email, password) VALUES (?, ?, ?, ?, ?)";
        // Catch SQLIntegrityConstraintViolationException for duplicate email
    }
}
```

#### **DonationDAO.java** (Complex Queries)
```java
public class DonationDAO {
    
    // Claim operation with transaction
    public void claimDonation(int donationId, int ngoId) {
        // 1. UPDATE Donation_Pool SET status='CLAIMED'
        // 2. INSERT INTO Claims
        // 3. INSERT INTO Impact_Log
        // Commit all or rollback
    }
    
    // Complex JOIN for display
    public List<Donation> getNGOClaimHistory(int ngoId) {
        String sql = """
            SELECT c.claim_id, fi.item_name, dp.quantity, u.name AS donor_name,
                   c.claim_time, COALESCE(il.meals_fed, 0) as meals_fed
            FROM Claims c
            INNER JOIN Donation_Pool dp ON c.donation_id = dp.donation_id
            INNER JOIN Food_Items fi ON dp.item_id = fi.item_id
            INNER JOIN Users u ON dp.donor_id = u.user_id
            LEFT JOIN Impact_Log il ON c.donation_id = il.donation_id
            WHERE c.ngo_id = ?
            ORDER BY c.claim_time DESC
        """;
        // Map ResultSet to Donation objects
    }
}
```

---

### 4. **Terminal Interface (CLI)**

**Location:** `src/foodbridge/ui/`

#### **DonorUI.java**
```java
public class DonorUI {
    
    public void show(User donor) {
        while(true) {
            System.out.println("DONOR MENU");
            System.out.println("1. Add Donation");
            System.out.println("2. View My Donations");
            System.out.println("3. Platform Statistics");
            
            // Process choice → call DonationDAO methods
            // Display results in formatted tables
        }
    }
}
```

**Features:**
- ASCII art banner & table formatting
- Colored status icons (🟢 AVAILABLE, 🔵 CLAIMED, 🔴 EXPIRED)
- Auto-expiry on login

#### **NGOUI.java**
- Same structure as DonorUI
- Menu options: Create request, View available, View claims
- Delegate to DAO

---

## 🗄️ Database Design

### **Schema: foodbridge_db**

```sql
-- Users: Donors, NGOs, Admins
Users (
    user_id         INT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    role            ENUM('DONOR', 'NGO', 'ADMIN'),
    phone           VARCHAR(20),
    email           VARCHAR(100) UNIQUE NOT NULL,
    password        VARCHAR(100),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Food Catalog (avoid repetition in Donation_Pool)
Food_Items (
    item_id         INT PRIMARY KEY AUTO_INCREMENT,
    item_name       VARCHAR(100) NOT NULL,
    category        ENUM('VEG', 'NON_VEG', 'BEVERAGE', 'OTHER'),
    UNIQUE(item_name, category)
)

-- ⭐ CORE TABLE: All donations
Donation_Pool (
    donation_id     INT PRIMARY KEY AUTO_INCREMENT,
    donor_id        INT FK REFERENCES Users,
    item_id         INT FK REFERENCES Food_Items,
    quantity        VARCHAR(50),  -- "5 kg", "20 portions"
    expiry_at       DATETIME,
    status          ENUM('AVAILABLE', 'CLAIMED', 'EXPIRED'),
    request_id      INT FK REFERENCES NGO_Requests (nullable),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX(donor_id),
    INDEX(status),
    INDEX(expiry_at)
)

-- NGO Requests
NGO_Requests (
    request_id      INT PRIMARY KEY AUTO_INCREMENT,
    ngo_id          INT FK REFERENCES Users,
    item_name       VARCHAR(100),
    quantity_needed VARCHAR(50),
    notes           TEXT,
    status          ENUM('OPEN', 'FULFILLED'),
    fulfilled_donation_id INT FK REFERENCES Donation_Pool (nullable),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX(status)
)

-- Claims: Who claimed what
Claims (
    claim_id        INT PRIMARY KEY AUTO_INCREMENT,
    donation_id     INT FK REFERENCES Donation_Pool UNIQUE,  -- 1:1 mapping
    ngo_id          INT FK REFERENCES Users,
    claim_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Impact Tracking: Meals fed estimate
Impact_Log (
    log_id          INT PRIMARY KEY AUTO_INCREMENT,
    donation_id     INT FK REFERENCES Donation_Pool UNIQUE,  -- 1:1 mapping
    meals_fed       INT,  -- Estimate: 2 meals per 1kg
    logged_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

### **Key Relationships**

```
Users (Donor) ──1:N──→ Donation_Pool
Users (NGO)   ──1:N──→ Claims
Users (NGO)   ──1:N──→ NGO_Requests

Donation_Pool ──1:N──→ Claims (actually 1:1 due to donation_id UNIQUE)
Donation_Pool ──1:N──→ Impact_Log (actually 1:1)
Donation_Pool ──N:1──→ Food_Items
Donation_Pool ──N:1──→ NGO_Requests (optional, nullable request_id)
NGO_Requests  ──N:1←── Donation_Pool (fulfilled_donation_id)
```

### **Normalization**

✅ **1NF:** All values atomic (no arrays/objects in cells)  
✅ **2NF:** Separated Food_Items to remove partial dependency on item_name  
✅ **3NF:** No transitive dependencies (donor info in Users, not Donation_Pool)

---

## ⚡ Real-Time Polling System

### **Frontend Polling Logic** (`app.js`)

**Every 2 seconds on Donor Dashboard:**
```javascript
setInterval(() => {
    loadRequests();      // GET /api/requests/open
    loadDonations();     // GET /api/donations/donor/{id}
}, 2000);
```

**Every 2 seconds on NGO Dashboard:**
```javascript
setInterval(() => {
    loadAvailable();     // GET /api/donations/available
    loadClaims();        // GET /api/ngo/{id}/claims
}, 2000);
```

**Every 3 seconds on Admin Dashboard:**
```javascript
setInterval(() => {
    loadStats();         // GET /api/stats
    loadUsers();         // GET /api/admin/users
    loadDonations();     // GET /api/admin/donations
}, 3000);
```

### **Backend Auto-Expiry** (FoodBridgeService.java)

**Before every SELECT query:**
```java
private void autoExpireDonations() {
    jdbc.update("""
        UPDATE Donation_Pool 
        SET status = 'EXPIRED' 
        WHERE status = 'AVAILABLE' AND expiry_at < NOW()
    """);
}
```

**Called at:**
- Line 52: getAvailableDonations()
- Line 68: getDonorDonations()
- Line 197: getPlatformStats()
- Line 246: getAllDonations() (admin)

**Why?**
- Ensures expired donations never shown as available
- Simpler than scheduled tasks
- Runs only when needed

---

## 🔒 Security Model

### **Current (Development)**
- ✅ SQL injection prevention: PreparedStatements
- ✅ Input validation: Type checking, null checks
- ❌ No password hashing (plain text)
- ❌ No HTTPS
- ❌ No JWT/session tokens

### **Production Recommendations**

1. **Password Hashing**
   ```java
   @Bean
   public BCryptPasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder();
   }
   
   // On register/login:
   encode(password) // Instead of storing plain text
   ```

2. **JWT Authentication**
   ```java
   // Generate JWT on login
   String token = Jwts.builder()
       .setSubject(user.getEmail())
       .signWith(key, SignatureAlgorithm.HS256)
       .compact();
   ```

3. **Spring Security**
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       // Define authorization rules
       // Require @Authenticated on all endpoints except /api/auth/*
   }
   ```

4. **Environment Variables**
   ```properties
   spring.datasource.password=${DB_PASSWORD}
   ```

5. **HTTPS/SSL**
   - Configure certificates
   - Redirect HTTP → HTTPS

---

## 📈 Performance Characteristics

| Operation | Time | Notes |
|-----------|------|-------|
| Load available donations (JOIN) | 50-100ms | 2 table JOIN |
| Auto-expire UPDATE | 10-50ms | Batch UPDATE with indexed column |
| Claim transaction (3 UPDATEs/INSERTs) | 100-200ms | Transactional |
| Frontend polling cycle | 2000ms | Configurable interval |
| Page load (all data + rendering) | 300-500ms | Fetch + DOM update |

### **Optimization Opportunities**
- Add indexes on frequently filtered columns (done: donor_id, status, expiry_at)
- Implement caching for /api/stats (admin dashboard)
- Batch poll requests (combine into single HTTP request)
- Lazy-load tables (pagination for large datasets)

---

## 🔄 Transaction Flow: Claim Operation

```
User clicks "Claim" button
    ↓
JavaScript: POST /api/donations/{donationId}/claim
    ↓
ApiController.claimDonation() receives request
    ↓
FoodBridgeService.claimDonation() starts TRANSACTION
    ↓
    ├─ UPDATE Donation_Pool SET status='CLAIMED' WHERE donation_id=?
    │  (Fails if already claimed by someone else)
    │
    ├─ INSERT INTO Claims (donation_id, ngo_id) VALUES (?, ?)
    │  (Creates claim record)
    │
    └─ INSERT INTO Impact_Log (donation_id, meals_fed) VALUES (?, ?)
       (Calculate meals_fed from quantity: regex extract number * 2)
    ↓
If ALL succeed: COMMIT
If ANY fails: ROLLBACK (Donation_Pool still shows AVAILABLE)
    ↓
Response sent to frontend: { success: true, message: "Donation claimed!" }
    ↓
JavaScript: call loadAvailable() to refresh donation list
    ↓
UI updates: Claimed donation disappears from list
    ↓
🎉 Done!
```

---

## 📊 Deployment Architecture

### **Development (Local)**
```
Local Machine
├─ Java 17 + Maven build
├─ Spring Boot runs on port 8080 (Tomcat)
├─ MySQL server listening on port 3306
└─ Browser @ http://localhost:8080
```

### **Production (Recommended)**
```
Internet
    ↓
        Load Balancer (AWS ELB)
    ↓
Application Servers (2-3 instances)
    ├─ Spring Boot JAR running
    ├─ Port 8080
    └─ HikariCP with 10-20 connections
    ↓
Database Layer
    ├─ MySQL RDS or managed database
    ├─ Read replicas for analytics
    ├─ Automated backups
    └─ Connection pooling
```

See [DEPLOYMENT.md](../DEPLOYMENT.md) for Docker, Heroku, AWS setup.

---

## 🎯 Design Patterns Used

| Pattern | Usage |
|---------|-------|
| **DAO Pattern** | UserDAO, DonationDAO separate DB logic |
| **Singleton** | DBConnection for single JDBC connection |
| **MVC** | Spring Boot: Controller → Service → DAO |
| **Request/Response DTO** | Clean separation of concerns |
| **Polling** | Frontend real-time updates |
| **Transaction** | ACID properties for claim operation |
| **PreparedStatement** | SQL injection prevention |

---

This document reflects the complete architecture as of v1.0.0.

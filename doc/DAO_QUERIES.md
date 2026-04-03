# DAO Queries — FoodBridge

Complete documentation of all SQL queries used in the project.

---

## 📋 Query Index

| # | Query Type | Purpose | File | Method |
|---|-----------|---------|------|--------|
| 1 | SELECT + WHERE | Login verification | UserDAO | login() |
| 2 | INSERT | User registration | UserDAO | register() |
| 3 | INSERT | Add donation | DonationDAO | addDonation() |
| 4 | SELECT + JOIN | View donor's donations | DonationDAO | getDonorDonations() |
| 5 | SELECT + 2 JOINs | View available food (NGO) | DonationDAO | getAvailableDonations() |
| 6 | UPDATE | Mark donation as CLAIMED | DonationDAO | claimDonation() |
| 7 | INSERT | Record claim | DonationDAO | claimDonation() |
| 8 | INSERT | Log impact (meals fed) | DonationDAO | claimDonation() |
| 9 | SELECT + 4 JOINs | NGO claim history | DonationDAO | printNGOClaims() |
| 10 | Aggregate (COUNT, SUM) | Platform impact stats | DonationDAO | printImpactStats() |
| 11 | UPDATE | Auto-expire donations | DonationDAO | autoExpireDonations() |

---

## Query 1: Login Verification (SELECT + WHERE)

**Purpose:** Authenticate user by email, password, and role

**File:** `src/foodbridge/dao/UserDAO.java`  
**Method:** `login()`  
**Type:** SELECT with WHERE clause

### SQL Query

```sql
SELECT user_id, name, role, phone, email 
FROM Users 
WHERE email = ? AND password = ? AND role = ?
```

### Parameters
- `email` - User email address
- `password` - User password (plain text in demo)
- `role` - Either 'DONOR' or 'NGO'

### Returns
- `User` object if credentials match
- `null` if no match found

### DBMS Concepts
- ✅ WHERE clause with multiple conditions (AND)
- ✅ PreparedStatement (parameterized query, prevents SQL injection)
- ✅ ResultSet mapping to object

### Example Usage
```java
User user = userDAO.login("saravana@donor.com", "donor123", "DONOR");
// Returns: User[id=1, name=Hotel Saravana Bhavan, role=DONOR]
```

---

## Query 2: User Registration (INSERT)

**Purpose:** Register a new donor or NGO

**File:** `src/foodbridge/dao/UserDAO.java`  
**Method:** `register()`  
**Type:** INSERT

### SQL Query

```sql
INSERT INTO Users (name, role, phone, email, password) 
VALUES (?, ?, ?, ?, ?)
```

### Parameters
- `name` - Organization/restaurant name
- `role` - 'DONOR' or 'NGO'
- `phone` - Contact number
- `email` - Unique email address
- `password` - Plain text password

### Returns
- `true` if registration successful
- `false` if email already exists or error occurred

### DBMS Concepts
- ✅ INSERT INTO statement
- ✅ PreparedStatement parameter binding
- ✅ UNIQUE constraint on email (prevents duplicates)
- ✅ Auto-increment primary key retrieval using `RETURN_GENERATED_KEYS`
- ✅ `SQLIntegrityConstraintViolationException` handling

### Example Usage
```java
boolean ok = userDAO.register("New NGO", "NGO", "9999999999", "new@ngo.com", "pass123");
// Returns: true if successful
```

---

## Query 3: Add Donation (INSERT)

**Purpose:** Donor creates a new food donation

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `addDonation()`  
**Type:** INSERT

### SQL Query

```sql
INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status) 
VALUES (?, ?, ?, ?, 'AVAILABLE')
```

### Parameters
- `donor_id` - ID of the donor
- `item_id` - ID of the food item (looked up or created)
- `quantity` - E.g., "5 kg", "20 portions"
- `expiry_at` - DateTime when food expires (YYYY-MM-DD HH:MM)

### Returns
- `true` if donation created successfully
- `false` if error occurred

### DBMS Concepts
- ✅ INSERT with foreign key references
- ✅ Default value for status ('AVAILABLE')
- ✅ Auto-increment primary key
- ✅ RETURN_GENERATED_KEYS to get new donation_id

### Related Helper Query

Before INSERT, check if food item exists:

```sql
SELECT item_id FROM Food_Items WHERE item_name = ?
```

If not found, insert new item:

```sql
INSERT INTO Food_Items (item_name, category) VALUES (?, 'OTHER')
```

### Example Usage
```java
boolean ok = donationDAO.addDonation(1, "Biryani", "5 kg", "2026-04-05 20:00");
// Returns: true if successful
// Prints: [DB] Donation created with ID = 7
```

---

## Query 4: View Donor's Donations (SELECT + JOIN)

**Purpose:** List all donations made by a specific donor

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `getDonorDonations()`  
**Type:** SELECT with 2 INNER JOINs

### SQL Query

```sql
SELECT 
    dp.donation_id, 
    dp.donor_id, 
    u.name AS donor_name, 
    fi.item_name, 
    fi.category, 
    dp.quantity, 
    DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at, 
    dp.status 
FROM Donation_Pool dp 
INNER JOIN Users u ON dp.donor_id = u.user_id 
INNER JOIN Food_Items fi ON dp.item_id = fi.item_id 
WHERE dp.donor_id = ? 
ORDER BY dp.created_at DESC
```

### Parameters
- `donor_id` - ID of the donor

### Returns
- `List<Donation>` of all donations by this donor
- Empty list if no donations

### DBMS Concepts
- ✅ INNER JOIN (multiple tables)
- ✅ WHERE clause for filtering
- ✅ ORDER BY for sorting
- ✅ DATE_FORMAT for readable dates
- ✅ Column aliasing (AS)

### Example Output
```
Donation ID: 1
Donor Name: Hotel Saravana Bhavan
Food Item: Steamed Rice
Category: VEG
Quantity: 10 kg
Expiry: 02-04-2026 22:00
Status: 🟢 AVAILABLE
```

---

## Query 5: View Available Donations (SELECT + 2 JOINs)

**Purpose:** NGO sees all available food ready to claim

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `getAvailableDonations()`  
**Type:** SELECT with 2 INNER JOINs + WHERE conditions

### SQL Query

```sql
SELECT 
    dp.donation_id, 
    dp.donor_id, 
    u.name AS donor_name, 
    fi.item_name, 
    fi.category, 
    dp.quantity, 
    DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at, 
    dp.status 
FROM Donation_Pool dp 
INNER JOIN Users u ON dp.donor_id = u.user_id 
INNER JOIN Food_Items fi ON dp.item_id = fi.item_id 
WHERE dp.status = 'AVAILABLE' AND dp.expiry_at > NOW() 
ORDER BY dp.expiry_at ASC
```

### Parameters
- None (retrieves all available donations)

### Returns
- `List<Donation>` of all claimable donations
- Sorted by expiry time (earliest first)

### DBMS Concepts
- ✅ Multi-table JOIN
- ✅ WHERE with multiple conditions (AND)
- ✅ NOW() function for current timestamp comparison
- ✅ ORDER BY for priority (urgent items first)

### Key Filters
1. **Status = 'AVAILABLE'** - Only not yet claimed
2. **expiry_at > NOW()** - Only non-expired
3. **ORDER BY ASC** - Urgent (expiring soon) first

### Example Output
```
ID: 1, Donor: Hotel Saravana Bhavan, Item: Steamed Rice, Qty: 10 kg, Expiry: 02-04-2026 22:00
ID: 4, Donor: Taj Residency, Item: Bread Loaves, Qty: 50 loaves, Expiry: 02-04-2026 20:00
```

---

## Query 6: Mark Donation as CLAIMED (UPDATE)

**Purpose:** Update donation status after NGO claims it

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `claimDonation()` — Part of Transaction  
**Type:** UPDATE

### SQL Query

```sql
UPDATE Donation_Pool 
SET status = 'CLAIMED' 
WHERE donation_id = ? AND status = 'AVAILABLE'
```

### Parameters
- `donation_id` - ID of donation to claim

### Returns
- Rows affected (should be 1 if successful, 0 if already claimed)

### DBMS Concepts
- ✅ UPDATE statement
- ✅ WHERE clause with multiple conditions
- ✅ Atomic update (ensures donation isn't double-claimed)

### Safety
- Only updates if status is currently 'AVAILABLE'
- Prevents race condition where two NGOs claim same donation

---

## Query 7: Record Claim (INSERT)

**Purpose:** Log which NGO claimed which donation

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `claimDonation()` — Part of Transaction  
**Type:** INSERT

### SQL Query

```sql
INSERT INTO Claims (donation_id, ngo_id) 
VALUES (?, ?)
```

### Parameters
- `donation_id` - ID of claimed donation
- `ngo_id` - ID of NGO claiming it

### Returns
- Rows affected (should be 1)

### DBMS Concepts
- ✅ INSERT with foreign key references
- ✅ UNIQUE constraint on donation_id (prevents double-claim)
- ✅ Part of multi-statement transaction

---

## Query 8: Log Impact (INSERT)

**Purpose:** Record how many meals were fed from claimed donation

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `claimDonation()` — Part of Transaction  
**Type:** INSERT with subquery

### SQL Query

```sql
INSERT INTO Impact_Log (donation_id, meals_fed) 
SELECT ?, 
       GREATEST(1, CAST(REGEXP_REPLACE(quantity, '[^0-9]','') AS UNSIGNED) * 2) 
FROM Donation_Pool 
WHERE donation_id = ?
```

### Parameters
- `donation_id` (twice) - ID of claimed donation

### Logic
1. **REGEXP_REPLACE(quantity, '[^0-9]','')** - Extract only numbers from quantity
   - "5 kg" → "5"
   - "30 portions" → "30"
2. **× 2** - Estimate: 1 unit = 2 meals
   - 5 kg → 10 meals
   - 30 portions → 60 meals
3. **GREATEST(1, ...)** - Minimum 1 meal even if extraction fails

### Returns
- Rows affected (should be 1)

### DBMS Concepts
- ✅ INSERT with subquery
- ✅ String manipulation (REGEXP_REPLACE)
- ✅ Type casting (CAST ... AS UNSIGNED)
- ✅ Math functions (GREATEST)

---

## Query 9: Claim History (SELECT + 4 JOINs + LEFT JOIN)

**Purpose:** Show NGO all their claimed donations with full details

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `printNGOClaims()`  
**Type:** SELECT with complex multi-table JOIN

### SQL Query

```sql
SELECT 
    c.claim_id, 
    fi.item_name, 
    dp.quantity, 
    u.name AS donor_name, 
    DATE_FORMAT(c.claim_time, '%d-%m-%Y %H:%i') AS claimed_at, 
    il.meals_fed 
FROM Claims c 
INNER JOIN Donation_Pool dp ON c.donation_id = dp.donation_id 
INNER JOIN Food_Items fi ON dp.item_id = fi.item_id 
INNER JOIN Users u ON dp.donor_id = u.user_id 
LEFT JOIN Impact_Log il ON c.donation_id = il.donation_id 
WHERE c.ngo_id = ? 
ORDER BY c.claim_time DESC
```

### Parameters
- `ngo_id` - ID of the NGO

### Returns
- Displays formatted table with claim history

### DBMS Concepts
- ✅ INNER JOIN (4 tables required)
- ✅ LEFT JOIN (Impact_Log may not exist yet)
- ✅ Date formatting
- ✅ WHERE clause
- ✅ ORDER BY DESC (newest first)

### Table Structure
```
Claim# | Food Item | Qty | Donated By | Claimed At | Meals Fed
-------|-----------|-----|------------|------------|----------
1      | Biryani   | 8kg | Hotel XYZ  | 02-04 20:30| 16
```

---

## Query 10: Impact Statistics (Aggregate Queries)

**Purpose:** Platform-wide impact metrics

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `printImpactStats()`  
**Type:** SELECT with GROUP BY, aggregate functions, subquery

### SQL Query

```sql
SELECT 
    COUNT(*) AS total_donations, 
    SUM(CASE WHEN status='CLAIMED' THEN 1 ELSE 0 END) AS claimed_count, 
    SUM(CASE WHEN status='AVAILABLE' THEN 1 ELSE 0 END) AS available_count, 
    SUM(CASE WHEN status='EXPIRED' THEN 1 ELSE 0 END) AS expired_count, 
    (SELECT COALESCE(SUM(meals_fed), 0) FROM Impact_Log) AS total_meals_fed 
FROM Donation_Pool
```

### Returns
- Single row with aggregate metrics

### Metrics Calculated
1. **total_donations** - COUNT(*) all records
2. **claimed_count** - COUNT with CASE WHEN status='CLAIMED'
3. **available_count** - COUNT with CASE WHEN status='AVAILABLE'
4. **expired_count** - COUNT with CASE WHEN status='EXPIRED'
5. **total_meals_fed** - SUM from Impact_Log table (subquery)

### DBMS Concepts
- ✅ COUNT() aggregate function
- ✅ SUM() aggregate function
- ✅ CASE WHEN for conditional counting
- ✅ Subquery for related table
- ✅ COALESCE() to handle NULL values (returns 0 if sum is NULL)

### Example Output
```
Platform Impact Stats
├─ Total Donations: 6
├─ Claimed: 1
├─ Still Available: 4
├─ Expired (wasted): 1
└─ Total Meals Fed: 60
```

---

## Query 11: Auto-Expire Donations (UPDATE)

**Purpose:** Batch update to mark expired donations

**File:** `src/foodbridge/dao/DonationDAO.java`  
**Method:** `autoExpireDonations()`  
**Type:** UPDATE with WHERE conditions

### SQL Query

```sql
UPDATE Donation_Pool 
SET status = 'EXPIRED' 
WHERE status = 'AVAILABLE' AND expiry_at < NOW()
```

### Parameters
- None

### Returns
- Number of rows updated

### Execution
- Automatically called when donor/NGO logs in
- Updates all past-due 'AVAILABLE' donations to 'EXPIRED'

### DBMS Concepts
- ✅ Batch UPDATE
- ✅ WHERE with multiple conditions
- ✅ NOW() function (current timestamp)
- ✅ Prevents claims after expiry

### Safety
- Only updates if CURRENTLY 'AVAILABLE'
- Already CLAIMED donations are not affected
- Already EXPIRED donations are not affected

---

## 🔄 Transaction Example: Claim Process

**Method:** `claimDonation()`

### Steps (All or Nothing)

```java
conn.setAutoCommit(false);  // BEGIN TRANSACTION

try {
    // Query 6: UPDATE donation status
    UPDATE Donation_Pool SET status = 'CLAIMED' 
    WHERE donation_id = ? AND status = 'AVAILABLE';
    
    // Query 7: INSERT claim record
    INSERT INTO Claims (donation_id, ngo_id) VALUES (?, ?);
    
    // Query 8: INSERT impact log
    INSERT INTO Impact_Log (donation_id, meals_fed) SELECT ...;
    
    conn.commit();  // All succeeded
} catch (Exception e) {
    conn.rollback();  // All failed, revert
    throw e;
}
```

### DBMS Concepts
- ✅ Transaction (ACID properties)
- ✅ Atomic operation (all or nothing)
- ✅ Rollback on error
- ✅ Commit on success

### Example Scenarios

**Success:** All 3 operations complete
- Donation marked CLAIMED
- Claim record created
- Impact logged

**Failure:** If any operation fails
- All changes rolled back
- Donation remains AVAILABLE
- No claim record created
- No impact logged

---

## 📊 Query Performance Notes

| Query | Indexes Recommended | Complexity |
|-------|-------------------|-----------|
| Login | email, role | O(1) - Direct lookup |
| Available Food | status, expiry_at | O(log n) - Search + sort |
| Claim History | ngo_id, claim_time | O(log n) - Multiple joins |
| Impact Stats | Aggregate | O(n) - Full table scan |

---

## 🎓 DBMS Concepts Demonstrated

✅ **SELECT, INSERT, UPDATE** - Basic CRUD  
✅ **WHERE clause** - Filtering  
✅ **INNER JOIN, LEFT JOIN** - Relationships  
✅ **Aggregate functions** - COUNT, SUM, COALESCE  
✅ **Transaction** - ACID properties, rollback  
✅ **PreparedStatement** - SQL injection prevention  
✅ **Foreign Keys** - Referential integrity  
✅ **Auto-increment** - Primary key generation  
✅ **Constraints** - UNIQUE, NOT NULL, DEFAULT  

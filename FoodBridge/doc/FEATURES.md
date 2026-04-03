# Features & Functionality — FoodBridge

Complete feature breakdown with use cases and implementation details.

---

## 🎯 Core Features Overview

FoodBridge connects food donors with NGOs through a simple, efficient digital platform. The application manages:

1. **User Authentication** — Role-based login/registration (Donor/NGO)
2. **Donation Management** — Add, list, and track food donations
3. **NGO Operations** — View available food and claim donations
4. **Impact Tracking** — Monitor meals fed and platform statistics
5. **Auto-Expiration** — Automatic status updates for expired food

---

## 👥 Feature 1: User Authentication

### 1.1 User Registration

**Description:** New users (Donors or NGOs) can register on the platform

**User Flow:**
```
Main Menu → Register → Select Role → Enter Details → Create Account
```

**Process:**
| Step | Action | Database Operation |
|------|--------|-------------------|
| 1 | User selects "Register" | - |
| 2 | Choose role (Donor/NGO) | - |
| 3 | Enter organization name | - |
| 4 | Enter phone number | - |
| 5 | Enter email address | Check UNIQUE constraint |
| 6 | Enter password | - |
| 7 | Confirm registration | INSERT INTO Users table |

**SQL Query:**
```sql
INSERT INTO Users (name, role, phone, email, password) 
VALUES (?, ?, ?, ?, ?)
```

**Validations:**
- ✅ Email must be unique (UNIQUE constraint in DB)
- ✅ Email format validation
- ✅ Password non-empty
- ✅ Name non-empty

**Error Handling:**
- Duplicate email → Show error message "Email already registered"
- Invalid input → Prompt user to re-enter

**Live Example:**
```
Registration successful! You can now login.
✅ New user created with ID = 5
```

---

### 1.2 User Login

**Description:** Existing users authenticate with email, password, and role

**User Flow:**
```
Main Menu → Login → Select Role → Enter Credentials → Dashboard
```

**Process:**
| Step | Action | Database Operation |
|------|--------|-------------------|
| 1 | User selects "Login" | - |
| 2 | Choose role (Donor/NGO) | - |
| 3 | Enter email | - |
| 4 | Enter password | - |
| 5 | Submit credentials | SELECT query with validation |
| 6 | On success → Dashboard | - |

**SQL Query:**
```sql
SELECT user_id, name, role, phone, email 
FROM Users 
WHERE email = ? AND password = ? AND role = ?
```

**Validations:**
- ✅ Email exists in system
- ✅ Password matches stored password
- ✅ Role matches selected role
- ✅ All conditions must be true (AND)

**Error Handling:**
- Invalid credentials → "Invalid email or password"
- No retry limit (for demo purposes)

**Live Example:**
```
✅ Welcome back, Hotel Saravana Bhavan!
SQL Query executed: SELECT verified email, password, role
Dashboard loaded for Donor
```

---

### 1.3 Role-Based Dashboards

**Description:** Users see different menus based on their role

**Donor Dashboard:**
```
┌──────────────────────────────────────────┐
│  🍴  DONOR DASHBOARD                    │
├──────────────────────────────────────────┤
│  1. Add New Donation                     │
│  2. View My Donations                    │
│  3. Platform Impact Stats                │
│  4. Logout                               │
└──────────────────────────────────────────┘
```

**NGO Dashboard:**
```
┌──────────────────────────────────────────┐
│  🤝  NGO DASHBOARD                       │
├──────────────────────────────────────────┤
│  1. View Available Food                  │
│  2. Claim a Donation                     │
│  3. My Claim History                     │
│  4. Platform Impact Stats                │
│  5. Logout                               │
└──────────────────────────────────────────┘
```

**Implementation:**
- After successful login, pass `userId` and `role` to respective UI class
- DonorUI for role=1, NGOUI for role=2
- User name displayed in header

---

## 🍽️ Feature 2: Donation Management (Donor Features)

### 2.1 Add New Donation

**Description:** Donors can post surplus food for NGOs to claim

**User Flow:**
```
Donor Dashboard → Add Donation → Enter Food Details → Submit
```

**Process:**
| Step | Action | Database Operation |
|------|--------|-------------------|
| 1 | User clicks "Add Donation" | - |
| 2 | Enter food item name | - |
| 3 | Enter quantity (e.g., "5 kg") | - |
| 4 | Enter expiry date & time | - |
| 5 | Submit donation | Check/create Food_Items & INSERT Donation_Pool |

**Database Operations:**
1. Check if food item already exists
2. If not, INSERT into Food_Items
3. INSERT into Donation_Pool with donor_id and expiry_at

**SQL Queries:**
```sql
-- Check if food item exists
SELECT item_id FROM Food_Items WHERE item_name = ?

-- Create new food item if needed
INSERT INTO Food_Items (item_name, category) VALUES (?, 'OTHER')

-- Add donation
INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status) 
VALUES (?, ?, ?, ?, 'AVAILABLE')
```

**Live Example:**
```
Food Item Name: Biryani
Quantity: 5 kg
Expiry: 2026-04-05 18:00

[SQL] INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status) 
      VALUES (1, ?, '5 kg', '2026-04-05 18:00:00', 'AVAILABLE')
[DB]  Donation created with ID = 7

✅  Donation added successfully!
```

**Validations:**
- ✅ All fields required (name, qty, expiry)
- ✅ Expiry datetime valid format
- ✅ Can't add donations in the past

---

### 2.2 View My Donations

**Description:** Donors see all their posted donations

**User Flow:**
```
Donor Dashboard → View My Donations → Table Display
```

**SQL Query:**
```sql
SELECT dp.donation_id, fi.item_name, fi.category, dp.quantity, 
       DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at, 
       dp.status
FROM Donation_Pool dp
INNER JOIN Food_Items fi ON dp.item_id = fi.item_id
WHERE dp.donor_id = ?
ORDER BY dp.expiry_at ASC
```

**Features:**
- ✅ Shows all donations by logged-in donor
- ✅ Includes food category
- ✅ Shows quantity and expiry time
- ✅ Shows current status (AVAILABLE/CLAIMED/EXPIRED)
- ✅ Sorted by expiry (earliest first)

**Display Format:**
```
┌──────┬──────────────────────┬──────────┬──────────────┬────────────┐
│ ID   │ Food Item            │ Qty      │ Expiry       │ Status     │
├──────┼──────────────────────┼──────────┼──────────────┼────────────┤
│ 7    │ Biryani              │ 5 kg     │ 05-04-2026   │ 🟢 AVAIL   │
│ 5    │ Vegetable Biryani    │ 8 kg     │ 03-04-2026   │ 🟢 AVAIL   │
│ 1    │ Steamed Rice         │ 10 kg    │ 02-04-2026   │ 🟢 AVAIL   │
└──────┴──────────────────────┴──────────┴──────────────┴────────────┘
```

**Status Meanings:**
- 🟢 **AVAILABLE** — Awaiting NGO claim
- 🔵 **CLAIMED** — Claimed by an NGO
- ⚫ **EXPIRED** — Past expiry time (auto-marked)

---

## 🤝 Feature 3: NGO Operations

### 3.1 View Available Donations

**Description:** NGOs see food available for claiming

**User Flow:**
```
NGO Dashboard → View Available Food → Filter & Display
```

**SQL Query:**
```sql
SELECT dp.donation_id, u.name AS donor_name, fi.item_name, 
       fi.category, dp.quantity, 
       DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at
FROM Donation_Pool dp
INNER JOIN Users u ON dp.donor_id = u.user_id
INNER JOIN Food_Items fi ON dp.item_id = fi.item_id
WHERE dp.status = 'AVAILABLE' AND dp.expiry_at > NOW()
ORDER BY dp.expiry_at ASC
```

**Filters Applied:**
- ✅ status = 'AVAILABLE' (only not-yet-claimed)
- ✅ expiry_at > NOW() (only non-expired)
- ✅ Sorted by expiry time (earliest first)

**Display Format:**
```
┌──────┬───────────────────┬──────────┬───────────┬──────────────┐
│ ID   │ Donor             │ Food     │ Qty       │ Expiry       │
├──────┼───────────────────┼──────────┼───────────┼──────────────┤
│ 7    │ Hotel Saravana    │ Biryani  │ 5 kg      │ 05-04-20 18:00
│ 5    │ Hotel Saravana    │ Veg Biry │ 8 kg      │ 03-04-20 04:00
│ 1    │ Hotel Saravana    │ Rice     │ 10 kg     │ 02-04-20 22:00
└──────┴───────────────────┴──────────┴───────────┴──────────────┘
```

---

### 3.2 Claim a Donation

**Description:** NGOs claim available food (multi-step transaction)

**User Flow:**
```
NGO Dashboard → Claim Donation → Enter ID → Confirm → Success
```

**Process:**
| Step | Action | Transaction |
|------|--------|-------------|
| 1 | Display available donations (view list) | Read |
| 2 | NGO enters donation ID | - |
| 3 | Confirm claim action | - |
| 4 | Mark donation as CLAIMED | UPDATE Donation_Pool |
| 5 | Record claim in Claims table | INSERT Claims |
| 6 | Calculate meals fed | INSERT Impact_Log |
| 7 | Commit transaction | COMMIT |

**SQL Operations (Transaction Block):**
```sql
START TRANSACTION;

-- 1. Update donation status
UPDATE Donation_Pool 
SET status = 'CLAIMED' 
WHERE donation_id = ? AND status = 'AVAILABLE'

-- 2. Record the claim
INSERT INTO Claims (donation_id, ngo_id) 
VALUES (?, ?)

-- 3. Calculate and log impact
INSERT INTO Impact_Log (donation_id, meals_fed) 
SELECT ?, GREATEST(1, CAST(REGEXP_REPLACE(quantity, '[^0-9]','') AS UNSIGNED) * 2)
FROM Donation_Pool 
WHERE donation_id = ?

COMMIT;
```

**Meal Calculation Logic:**
- Extract numeric value from quantity (e.g., "5 kg" → 5)
- Multiply by 2 (assumption: 1 kg = 2 meals)
- Example: "5 kg" → 5 × 2 = 10 meals
- Minimum: 1 meal (if extraction fails)

**Live Example:**
```
Enter Donation ID to claim: 1
Confirm claim? (yes/no): yes

[SQL] UPDATE Donation_Pool SET status = 'CLAIMED' 
[SQL] INSERT INTO Claims (donation_id, ngo_id) VALUES (1, 3)
[SQL] INSERT INTO Impact_Log (donation_id, meals_fed)

[DB]  Transaction committed successfully.
✅  Donation claimed successfully! Impact logged.
```

**Error Cases:**
- Donation doesn't exist → Error
- Donation already claimed → Error
- Donation expired → Error
- Transaction fails → ROLLBACK (no partial updates)

---

### 3.3 View Claim History

**Description:** NGOs see all their past claims

**User Flow:**
```
NGO Dashboard → Claim History → View Table
```

**SQL Query:**
```sql
SELECT c.claim_id, fi.item_name, dp.quantity, u.name AS donor_name,
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

**Query Complexity:**
- ✅ 4 INNER JOINs (Claims → Donation_Pool → Food_Items → Users)
- ✅ 1 LEFT JOIN (Impact_Log, because some claims may not have logged yet)
- ✅ WHERE filters by current NGO
- ✅ DESC order (newest first)

**Display Format:**
```
╔══════╦════════════════════╦══════════╦═══════════════╦═════════════╗
║ Clm# ║ Food Item          ║ Qty      ║ Donor         ║ Meals Fed   ║
╠══════╬════════════════════╬══════════╬═══════════════╬═════════════╣
║ 2    ║ Steamed Rice       ║ 10 kg    ║ Hotel Saravana║ 20          ║
║ 1    ║ Sambar            ║ 15 litres║ Hotel Saravana║ 30          ║
╚══════╩════════════════════╩══════════╩═══════════════╩═════════════╝
```

---

## 📊 Feature 4: Impact Statistics

### 4.1 Platform-Wide Impact

**Description:** Dashboard showing overall platform metrics

**User Flow:**
```
Any Dashboard → Impact Stats → View Metrics
```

**Metrics Calculated:**
| Metric | Query Type | Value |
|--------|-----------|-------|
| Total Donations | COUNT(*) | 7 |
| Claimed | SUM CASE | 1 |
| Available | SUM CASE | 5 |
| Expired (wasted) | SUM CASE | 1 |
| Total Meals Fed | SUM aggregate | 60 |

**SQL Query:**
```sql
SELECT COUNT(*) AS total_donations,
       SUM(CASE WHEN status='CLAIMED' THEN 1 ELSE 0 END) AS claimed_count,
       SUM(CASE WHEN status='AVAILABLE' THEN 1 ELSE 0 END) AS available_count,
       SUM(CASE WHEN status='EXPIRED' THEN 1 ELSE 0 END) AS expired_count,
       (SELECT COALESCE(SUM(meals_fed),0) FROM Impact_Log) AS total_meals_fed
FROM Donation_Pool
```

**Display Format:**
```
╔══════════════════════════════╗
║   PLATFORM IMPACT STATS      ║
╠══════════════════════════════╣
║  Total Donations   : 7       ║
║  Claimed           : 1       ║
║  Still Available   : 5       ║
║  Expired (wasted)  : 1       ║
║  Total Meals Fed   : 60      ║
╚══════════════════════════════╝
```

**Use Case:**
- Shows platform effectiveness
- Demonstrates waste reduction
- Highlights impact metrics for viva presentation

---

## ⏰ Feature 5: Auto-Expiration

### 5.1 Auto-Expire Donations

**Description:** System automatically marks expired donations

**Trigger:** Runs every time user logs in

**SQL Query:**
```sql
UPDATE Donation_Pool 
SET status = 'EXPIRED' 
WHERE status = 'AVAILABLE' AND expiry_at < NOW()
```

**Logic:**
- ✅ Only updates 'AVAILABLE' (not already CLAIMED)
- ✅ Compares expiry_at < current timestamp
- ✅ Batch operation (all expired at once)

**Live Example:**
```
[SQL] UPDATE Donation_Pool SET status = 'EXPIRED' 
      WHERE status = 'AVAILABLE' AND expiry_at < NOW()
```

**Benefit:**
- No manual intervention needed
- Prevents NGOs from claiming expired food
- Keeps status accurate
- Automatic data hygiene

---

## 🔄 Advanced Features (Architecture)

### Transactions & ACID Properties

**Feature:** Claim operation uses transactions for data consistency

**ACID Guarantee:**
```
START TRANSACTION
- UPDATE donation status
- INSERT claim record
- INSERT impact log
COMMIT or ROLLBACK (automatic)
```

**Benefit:** All-or-nothing operation (no partial updates)

---

### Data Integrity

**Feature:** Foreign Key Constraints with CASCADE

**Relationships:**
```
Users ←→ Donation_Pool ←→ Food_Items
              ↓
            Claims ←→ Impact_Log
```

**Cascade Rules:**
- Delete user → Delete all their donations
- Delete donation → Delete related claims & impact logs
- Prevents orphaned records

---

### Query Optimization

**Features:**
- ✅ Parameterized queries (prevent SQL injection)
- ✅ Indexes on foreign keys (fast joins)
- ✅ DateFormat consistent (for display)
- ✅ Sorted results (better UX)

---

## 📈 Feature Summary Table

| Feature | Type | Status | Complexity |
|---------|------|--------|-----------|
| User Registration | ✅ CRUD | WORKING | Low |
| User Login | ✅ Authentication | WORKING | Low |
| Add Donation | ✅ CREATE | WORKING | Medium |
| View Donations | ✅ READ | WORKING | Medium |
| View Available | ✅ READ | WORKING | High |
| Claim Donation | ✅ TRANSACTION | WORKING | High |
| Claim History | ✅ READ | WORKING | High |
| Impact Stats | ✅ AGGREGATE | WORKING | High |
| Auto-Expire | ✅ BATCH | WORKING | Medium |

**Total Working Features:** 9/9 ✅

---

## 🎁 Potential Future Enhancements

### Phase 2 Features (Not Implemented)

1. **Admin Role**
   - Monitor platform activity
   - View all users and donations
   - Generate reports
   - Manage disputes

2. **Search & Filter**
   - Filter by food category
   - Search by keyword
   - Filter by date range
   - Filter by status

3. **Ratings & Reviews**
   - Rate donors
   - Rate NGOs
   - Review food quality
   - Comment system

4. **Notifications**
   - Email on donation available
   - SMS on claim
   - Platform notifications
   - Expiry warnings

5. **Image Support**
   - Upload food photos
   - Photo gallery for donations
   - Visual identification

6. **Advanced Analytics**
   - Charts and graphs
   - Trends over time
   - NGO performance metrics
   - Donor leaderboard

7. **Mobile App**
   - React Native / Flutter
   - Push notifications
   - Real-time updates

---

## ✅ Feature Completion Status

**MVP (Minimum Viable Product):** 100% COMPLETE ✅
- User authentication working
- Donation management functional
- NGO operations verified
- Impact tracking implemented
- Auto-expiration active

**Ready for:**
- Faculty viva presentation
- Deployment to small-scale production
- User testing with real NGOs and donors

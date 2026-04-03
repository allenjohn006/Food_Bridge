# Testing Guide — FoodBridge

Complete testing procedures for manual validation and demonstration.

---

## 🧪 Pre-Testing Checklist

- [ ] MySQL Server is running
- [ ] Database `foodbridge_db` has been created with schema
- [ ] JDBC jar is in `lib/` folder
- [ ] Java 17+ installed and in PATH
- [ ] Project compiled successfully
- [ ] All `.class` files in `out/` folder

---

## 🚀 Test 1: Basic Application Launch

**Purpose:** Verify app starts and menu displays correct

**Steps:**
1. Compile: `javac -encoding UTF-8 -cp "lib\*" -d out -sourcepath src src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java`
2. Run: `java -cp "out;lib\*" foodbridge.Main`

**Expected Output:**
```
╔══════════════════════════════════════════════════════════╗
║    🌱  F O O D B R I D G E                              ║
║        Connecting Surplus Food with Those Who Need It   ║
║    Donors (Hotels/Restaurants) → NGOs → Communities     ║
╚══════════════════════════════════════════════════════════╝

╔════════════════════════════════╗
║       MAIN MENU                ║
╠════════════════════════════════╣
║  1.  Login                     ║
║  2.  Register                  ║
║  3.  Exit                      ║
╚════════════════════════════════╝
Choice: 
```

**Result:** ✅ PASS if menu appears correctly

---

## 🧪 Test 2: Donor Login with Demo Credentials

**Purpose:** Verify authentication works and queries database

**Steps:**
1. Launch app (from Test 1)
2. Input:
   ```
   Choice: 1
   Select role: 1
   Email: saravana@donor.com
   Password: donor123
   ```

**Expected Output:**
```
[SQL] SELECT user_id, name, role, phone, email FROM Users WHERE email = ? AND password = ? AND role = ?

✅  Welcome back, Hotel Saravana Bhavan!
[SQL] UPDATE Donation_Pool SET status = 'EXPIRED' WHERE status = 'AVAILABLE' AND expiry_at < NOW()

╔════════════════════════════════════════╗
║  🍴  DONOR DASHBOARD — Hotel Saravana Bhavan ║
╠════════════════════════════════════════╣
║  1.  Add New Donation                  ║
║  2.  View My Donations                 ║
║  3.  Platform Impact Stats             ║
║  4.  Logout                            ║
╚════════════════════════════════════════╝
Choice: 
```

**Verification Points:**
- ✅ SQL query printed (SELECT with WHERE)
- ✅ Login successful message
- ✅ Auto-expire query executed
- ✅ Donor name retrieved correctly from database
- ✅ Donor menu displayed

**Result:** ✅ PASS if all verification points met

---

## 🧪 Test 3: Add New Donation

**Purpose:** Verify INSERT query and food item creation

**Steps:**
1. From donor dashboard (Test 2):
   ```
   Choice: 1
   Food Item Name: Biryani
   Quantity: 5 kg
   Expiry Date & Time: 2026-04-05 18:00
   ```

**Expected Output:**
```
╔══════════════════════╗
║   ➕ Add Donation    ║
╚══════════════════════╝
Food Item Name   : Biryani
Quantity (e.g. 5 kg / 20 portions) : 5 kg
Expiry Date & Time (YYYY-MM-DD HH:MM) : 2026-04-05 18:00

[SQL] INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status) VALUES (?, ?, ?, ?, 'AVAILABLE')
[DB]  Donation created with ID = 7

✅  Donation added successfully!
```

**Verification Points:**
- ✅ SQL INSERT query printed
- ✅ New donation ID generated (auto-increment)
- ✅ Success message displayed
- ✅ Returned to donor menu

**Database Verification:**
In MySQL:
```sql
SELECT * FROM Donation_Pool WHERE donation_id = 7;
-- Should show: donor_id=1, item_id=?, quantity='5 kg', status='AVAILABLE', expiry_at='2026-04-05 18:00'

SELECT * FROM Food_Items WHERE item_name = 'Biryani';
-- Should exist if new, or reference existing if already present
```

**Result:** ✅ PASS if donation added and visible in database

---

## 🧪 Test 4: View Donor's Donations

**Purpose:** Verify SELECT with JOIN queries work correctly

**Steps:**
1. From donor dashboard:
   ```
   Choice: 2
   ```

**Expected Output:**
```
┌────────────────────────────────────────────────────────────────────┐
│                      MY DONATIONS                                  │
├──────┬──────────────────────┬──────────┬──────────────┬────────────┤
│ ID   │ Food Item            │ Qty      │ Expiry       │ Status     │
├──────┼──────────────────────┼──────────┼──────────────┼────────────┤
│ 7    │ Biryani              │ 5 kg     │ 05-04-2026   │ 🟢 AVAIL   │
│ 5    │ Vegetable Biryani    │ 8 kg     │ 03-04-2026   │ 🟢 AVAIL   │
│ 1    │ Steamed Rice         │ 10 kg    │ 02-04-2026   │ 🟢 AVAIL   │
│ 2    │ Sambar              │ 15 litres│ 02-04-2026   │ 🟢 AVAIL   │
└──────┴──────────────────────┴──────────┴──────────────┴────────────┘
```

**Verification Points:**
- ✅ All donor's donations listed
- ✅ Food item names displayed correctly (JOIN with Food_Items)
- ✅ Quantities shown
- ✅ Status icons display
- ✅ Sorted by most recent first
- ✅ SQL query printed to console

**Result:** ✅ PASS if all donations show with correct details

---

## 🧪 Test 5: Impact Statistics (Aggregate Query)

**Purpose:** Verify COUNT, SUM aggregate functions

**Steps:**
1. From donor dashboard:
   ```
   Choice: 3
   ```

**Expected Output:**
```
[SQL] SELECT COUNT(*) AS total_donations, SUM(CASE WHEN status='CLAIMED' THEN 1 ELSE 0 END) AS claimed_count, SUM(CASE WHEN status='AVAILABLE' THEN 1 ELSE 0 END) AS available_count, SUM(CASE WHEN status='EXPIRED' THEN 1 ELSE 0 END) AS expired_count, (SELECT COALESCE(SUM(meals_fed),0) FROM Impact_Log) AS total_meals_fed FROM Donation_Pool

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

**Verification Points:**
- ✅ Aggregate query printed
- ✅ COUNT(*) shows total donations
- ✅ SUM with CASE WHEN works for each status
- ✅ Subquery for meals_fed works
- ✅ Numbers are correct based on database state

**Database Verification:**
```sql
SELECT COUNT(*) FROM Donation_Pool;
-- Should match "Total Donations" shown

SELECT COUNT(*) FROM Donation_Pool WHERE status = 'CLAIMED';
-- Should match "Claimed" shown

SELECT SUM(meals_fed) FROM Impact_Log;
-- Should match "Total Meals Fed" shown
```

**Result:** ✅ PASS if all statistics calculate correctly

---

## 🧪 Test 6: Logout & NGO Login

**Purpose:** Verify role-based authentication and NGO dashboard

**Steps:**
1. From donor dashboard:
   ```
   Choice: 4
   ```
2. From main menu:
   ```
   Choice: 1
   Select role: 2
   Email: greenearth@ngo.com
   Password: ngo123
   ```

**Expected Output:**
```
👋  Logged out. Thank you for reducing waste!

╔════════════════════════════╗
║       MAIN MENU            ║
╠════════════════════════════╣
║  1.  Login                 ║
║  2.  Register              ║
║  3.  Exit                  ║
╚════════════════════════════╝
Choice: 1

[SQL] SELECT user_id, name, role, phone, email FROM Users WHERE email = ? AND password = ? AND role = ?

✅  Welcome back, Green Earth NGO!
[SQL] UPDATE Donation_Pool SET status = 'EXPIRED' WHERE status = 'AVAILABLE' AND expiry_at < NOW()

╔════════════════════════════════════════╗
║  🤝  NGO DASHBOARD — Green Earth NGO   ║
╠════════════════════════════════════════╣
║  1.  View Available Food               ║
║  2.  Claim a Donation                  ║
║  3.  My Claim History                  ║
║  4.  Platform Impact Stats             ║
║  5.  Logout                            ║
╚════════════════════════════════════════╝
Choice: 
```

**Verification Points:**
- ✅ Logout message displayed
- ✅ Returned to main menu
- ✅ NGO login successful
- ✅ Auto-expire query ran again
- ✅ NGO name retrieved correctly
- ✅ NGO dashboard (not donor dashboard) displayed

**Result:** ✅ PASS if NGO menu shows

---

## 🧪 Test 7: View Available Donations (NGO View)

**Purpose:** Verify SELECT with multi-table JOINs filters correctly

**Steps:**
1. From NGO dashboard:
   ```
   Choice: 1
   ```

**Expected Output:**
```
[SQL] SELECT dp.donation_id, dp.donor_id, u.name AS donor_name, fi.item_name, fi.category, dp.quantity, DATE_FORMAT(dp.expiry_at, '%d-%m-%Y %H:%i') AS expiry_at, dp.status FROM Donation_Pool dp INNER JOIN Users u ON dp.donor_id = u.user_id INNER JOIN Food_Items fi ON dp.item_id = fi.item_id WHERE dp.status = 'AVAILABLE' AND dp.expiry_at > NOW() ORDER BY dp.expiry_at ASC

┌──────────────────────────────────────────────────────────────────────────┐
│              AVAILABLE FOOD DONATIONS                                    │
├──────┬───────────────────────┬──────────┬───────────┬──────────────┬─────┤
│ ID   │ Food Item             │ Category │ Quantity  │ Expiry       │Donor│
├──────┼───────────────────────┼──────────┼───────────┼──────────────┼─────┤
│ 1    │ Steamed Rice          │ VEG      │ 10 kg     │ 02-04-20 22:0│Hote│
│ 2    │ Sambar               │ VEG      │ 15 litres │ 02-04-20 21:0│Hote│
│ 4    │ Bread Loaves         │ BAKERY   │ 50 loaves │ 02-04-20 20:0│Taj │
│ 5    │ Vegetable Biryani    │ VEG      │ 8 kg      │ 03-04-20 04:0│Hote│
│ 7    │ Biryani              │ OTHER    │ 5 kg      │ 05-04-20 18:0│Hote│
└──────┴───────────────────────┴──────────┴───────────┴──────────────┴─────┘
```

**Verification Points:**
- ✅ Complex SQL query with 2 JOINs printed
- ✅ Only AVAILABLE donations shown (status filter)
- ✅ Only non-expired donations shown (expiry_at > NOW())
- ✅ Donor names displayed (JOIN with Users)
- ✅ Food categories shown (JOIN with Food_Items)
- ✅ Sorted by expiry (earliest first)
- ✅ No CLAIMED donations shown

**Result:** ✅ PASS if available list filtered and sorted correctly

---

## 🧪 Test 8: Claim Donation (Transaction Test)

**Purpose:** Verify transaction with COMMIT and multiple operations

**Steps:**
1. From NGO dashboard:
   ```
   Choice: 2
   ```
2. Then:
   ```
   Enter Donation ID to claim: 1
   Confirm claim? (yes/no): yes
   ```

**Expected Output:**
```
⬜ Available donations listed first (view from Test 7 repeats)

Enter Donation ID to claim: 1
Confirm claim? (yes/no): yes

[SQL] UPDATE Donation_Pool SET status = 'CLAIMED' WHERE donation_id = ? AND status = 'AVAILABLE'
[SQL] INSERT INTO Claims (donation_id, ngo_id) VALUES (?, ?)
[SQL] INSERT INTO Impact_Log (donation_id, meals_fed) SELECT ?, GREATEST(1, CAST(REGEXP_REPLACE(quantity, '[^0-9]','') AS UNSIGNED) * 2) FROM Donation_Pool WHERE donation_id = ?
[DB]  Transaction committed successfully.

✅  Donation claimed successfully! Impact logged.
```

**Verification Points:**
- ✅ All 3 SQL queries printed (UPDATE + 2 INSERTs)
- ✅ Transaction committed message shown
- ✅ Success message displayed
- ✅ No errors or rollback messages

**Database Verification:**
In MySQL after claiming donation 1:
```sql
SELECT * FROM Donation_Pool WHERE donation_id = 1;
-- Should show: status = 'CLAIMED'

SELECT * FROM Claims WHERE donation_id = 1;
-- Should show: ngo_id = 3 (Green Earth NGO)

SELECT * FROM Impact_Log WHERE donation_id = 1;
-- Should show: meals_fed = 20 (10 kg × 2)
```

**Result:** ✅ PASS if donation status changes in 3 tables (transaction successful)

---

## 🧪 Test 9: NGO Claim History (Complex JOIN)

**Purpose:** Verify 4-table JOIN with LEFT JOIN

**Steps:**
1. From NGO dashboard:
   ```
   Choice: 3
   ```

**Expected Output:**
```
[SQL] SELECT c.claim_id, fi.item_name, dp.quantity, u.name AS donor_name, DATE_FORMAT(c.claim_time, '%d-%m-%Y %H:%i') AS claimed_at, il.meals_fed FROM Claims c INNER JOIN Donation_Pool dp ON c.donation_id = dp.donation_id INNER JOIN Food_Items fi ON dp.item_id = fi.item_id INNER JOIN Users u ON dp.donor_id = u.user_id LEFT JOIN Impact_Log il ON c.donation_id = il.donation_id WHERE c.ngo_id = ? ORDER BY c.claim_time DESC

╔══════╦════════════════════╦══════════╦═══════════════════╦═════════════════════╦═══════════╗
║ Clm# ║ Food Item          ║ Qty      ║ Donated By        ║ Claimed At          ║ Meals Fed ║
╠══════╬════════════════════╬══════════╬═══════════════════╬═════════════════════╬═══════════╣
║ 2    ║ Steamed Rice       ║ 10 kg    ║ Hotel Saravana    ║ 03-04-2026 12:30    ║ 20        ║
║ 1    ║ Sambar            ║ 15 litres║ Hotel Saravana    ║ 03-04-2026 11:15    ║ 30        ║
╚══════╩════════════════════╩══════════╩═══════════════════╩═════════════════════╩═══════════╝
```

**Verification Points:**
- ✅ Complex query with 4 INNER JOINs + 1 LEFT JOIN printed
- ✅ Only claims by logged-in NGO shown
- ✅ Food names displayed
- ✅ Donor names displayed
- ✅ Meals fed calculated correctly
- ✅ Sorted by latest first (DESC)

**Result:** ✅ PASS if claim history shows correctly

---

## 🧪 Test 10: Registration (New User)

**Purpose:** Verify INSERT and duplicate email prevention

**Steps:**
1. From main menu:
   ```
   Choice: 2
   Select role: 1
   Organisation/Name: New Restaurant
   Phone: 9876543210
   Email: newrest@donor.com
   Password: newpass123
   ```

**Expected Output (Success):**
```
╔══════════════════════╗
║    📝  REGISTER      ║
╚══════════════════════╝
Role:  1. Donor (Restaurant/Hotel)   2. NGO/Charity
Select role: 1
Organisation/Name : New Restaurant
Phone             : 9876543210
Email             : newrest@donor.com
Password          : newpass123

[SQL] INSERT INTO Users (name, role, phone, email, password) VALUES (?, ?, ?, ?, ?)
[DB]  New user created with ID = 5

✅  Registration successful! You can now login.
```

**Verification Points:**
- ✅ SQL INSERT query printed
- ✅ New user ID generated (auto-increment)
- ✅ Success message shown

**Test 10b: Duplicate Email Prevention**

**Steps:**
1. Try registering same email again:
   ```
   Email: saravana@donor.com
   ```

**Expected Output:**
```
[SQL] INSERT INTO Users (name, role, phone, email, password) VALUES (?, ?, ?, ?, ?)
[ERROR] Email already registered. Please use a different email.

❌  Registration failed. Email may already be in use.
```

**Verification Points:**
- ✅ Duplicate email prevented by UNIQUE constraint
- ✅ Error message displayed clearly
- ✅ Registration rejected

**Result:** ✅ PASS if new user created and duplicates rejected

---

## 🧪 Test 11: Query Monitoring

**Purpose:** Verify all queries are printed for monitoring

**Steps:**
Perform any action above and look for `[SQL]` prefix

**Expected:** 
```
[SQL] SELECT user_id, name, role, phone, email FROM Users WHERE email = ? AND password = ? AND role = ?
[SQL] UPDATE Donation_Pool SET status = 'EXPIRED' WHERE status = 'AVAILABLE' AND expiry_at < NOW()
[SQL] INSERT INTO Donation_Pool (donor_id, item_id, quantity, expiry_at, status) VALUES (?, ?, ?, ?, 'AVAILABLE')
...
```

**Verification Points:**
- ✅ Every query is logged with `[SQL]` prefix
- ✅ Useful for debugging and demonstrating to faculty
- ✅ Shows query execution order

**Result:** ✅ PASS if queries print before execution

---

## 🎯 Full Integration Flow (Complete Test)

**Time:** ~10 minutes

**Objective:** Test entire workflow from registration to claim

**Steps:**
1. ✅ Register new NGO
2. ✅ Donor adds donation
3. ✅ NGO views available
4. ✅ NGO claims donation
5. ✅ Verify database updated
6. ✅ View claim history
7. ✅ Check impact stats

---

## 🐛 Troubleshooting Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| "[ERROR] MySQL Driver not found" | JDBC jar missing | Add jar to lib/ |
| "[ERROR] Cannot connect to MySQL" | DB not running/credentials wrong | Check DB, verify password |
| "Unknown database 'foodbridge_db'" | Schema not initialized | Run sql/schema.sql |
| "Could not find or load main class" | Not compiled | Run javac command |
| Login shows "Invalid credentials" even with correct password | Password mismatch in DB | Check demo credentials |
| Donation not appearing after add | Transaction failed silently | Check DB directly |

---

## 📊 Success Criteria

All tests must pass:
- [ ] Test 1: App launches
- [ ] Test 2: Donor login
- [ ] Test 3: Add donation works
- [ ] Test 4: View donations shows data
- [ ] Test 5: Statistics calculated correctly
- [ ] Test 6: NGO login works
- [ ] Test 7: Available donations filtered
- [ ] Test 8: Claim transaction completes
- [ ] Test 9: Claim history displays
- [ ] Test 10: Registration works
- [ ] Test 11: Queries logged

**If all pass:** ✅ Project is production-ready for viva demonstration

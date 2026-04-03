# Setup Guide — FoodBridge

Complete step-by-step instructions to set up and run FoodBridge on Windows.

---

## ✅ Prerequisites

- ✅ Windows 10/11
- ✅ Java 17+ installed (`javac` and `java` in PATH)
- ✅ MySQL Server 8.0+ running
- ✅ MySQL JDBC Connector JAR file (mysql-connector-j-9.6.0.jar)
- ✅ VS Code with integrated terminal

---

## Step 1: Prepare Project Directory

The project should be organized as:

```
FoodBridge/
├── src/foodbridge/     ← Java source files
├── sql/schema.sql      ← Database schema
├── lib/                ← JDBC JAR goes here
├── out/                ← Compiled .class files (created during build)
├── doc/                ← Documentation (this folder)
└── .vscode/tasks.json  ← Build tasks
```

**Create lib folder if it doesn't exist:**
```bash
mkdir lib
```

---

## Step 2: Add MySQL JDBC Driver

Download from: https://dev.mysql.com/downloads/connector/j/

1. Download the **Platform Independent (ZIP)** version
2. Extract the ZIP file
3. Find `mysql-connector-j-x.x.x.jar`
4. Copy it to the `lib/` folder

**Result:** `FoodBridge/lib/mysql-connector-j-9.6.0.jar`

---

## Step 3: Create MySQL Database

### Option A: Using MySQL Workbench (Recommended)

1. Open MySQL Workbench
2. Click **File → Open SQL Script**
3. Navigate to `sql/schema.sql`
4. Click **Execute** (or `Ctrl+Shift+Enter`)
5. Wait for all statements to complete ✅

### Option B: Using MySQL CLI

```bash
mysql -u root -p < sql/schema.sql
```

**Result:** 
- Database `foodbridge_db` is created
- 5 tables are created
- Sample data is inserted (4 users, 8 food items, 6 donations, 1 claim, 1 impact log)

---

## Step 4: Configure Database Connection

Edit: `src/foodbridge/dao/DBConnection.java`

Find lines 15-17:
```java
private static final String URL      = "jdbc:mysql://localhost:3306/foodbridge_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
private static final String USER     = "root";
private static final String PASSWORD = "your_mysql_password";
```

Update the PASSWORD with your actual MySQL root password.

**Example:**
```java
private static final String PASSWORD = "Malrajadichiku@610";
```

---

## Step 5: Verify Java Installation

Open terminal and run:

```bash
javac -version
java -version
```

**Expected output:**
```
javac 17.0.17
java version "17.0.17"
```

If not found, install Java from: https://www.oracle.com/java/technologies/downloads/

---

## Step 6: Compile the Project

### Option A: Using VS Code Tasks (Recommended)

1. Open the FoodBridge folder in VS Code
2. Press `Ctrl + Shift + B`
3. Select **"1 - Compile FoodBridge"**
4. Wait for compilation to complete

### Option B: Using Terminal Directly

```bash
cd FoodBridge
javac -encoding UTF-8 -cp "lib\*" -d out -sourcepath src src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java
```

**Result:** 
- Compiled `.class` files in `out/` folder
- No errors should appear

---

## Step 7: Run the Application

### Option A: Using VS Code Tasks

1. Press `Ctrl + Shift + B`
2. Select **"2 - Run FoodBridge"**

### Option B: Using Terminal Directly

```bash
cd FoodBridge
java -cp "out;lib\*" foodbridge.Main
```

**Expected Output:**
```
  ╔══════════════════════════════════════════════════════════╗
  ║                                                          ║
  ║    🌱  F O O D B R I D G E                              ║
  ║        Connecting Surplus Food with Those Who Need It   ║
  ║                                                          ║
  ║    Donors (Hotels/Restaurants) → NGOs → Communities     ║
  ║                                                          ║
  ╚══════════════════════════════════════════════════════════╝

  ╔════════════════════════════════╗
  ║       MAIN MENU                ║
  ╠════════════════════════════════╣
  ║  1.  Login                     ║
  ║  2.  Register                  ║
  ║  3.  Exit                      ║
  ╚════════════════════════════════╝
  Choice: _
```

---

## Step 8: Test the Application

### Test 1: Donor Login
```
Choice: 1
Select role: 1
Email: saravana@donor.com
Password: donor123
```

**Expected:** "Welcome back, Hotel Saravana Bhavan!"

### Test 2: Add Donation (as Donor)
```
Choice: 1 (Add New Donation)
Food Item Name: Biryanis
Quantity: 5 kg
Expiry Date & Time: 2026-04-05 20:00
```

**Expected:** "Donation added successfully!"

### Test 3: NPL Claim (Logout then Login as NGO)
```
Choice: 4 (Logout)
Then:
Choice: 1 (Login)
Select role: 2
Email: greenearth@ngo.com
Password: ngo123
```

**Expected:** "Welcome back, Green Earth NGO!"

### Test 4: View & Claim
```
Choice: 1 (View Available Food)
Choice: 2 (Claim a Donation)
Enter Donation ID: 1
Confirm claim: yes
```

**Expected:** "Donation claimed successfully! Impact logged."

---

## ❌ Troubleshooting

### Issue: "MySQL Driver not found"
**Solution:** Ensure `mysql-connector-j-9.6.0.jar` is in `lib/` folder

### Issue: "Unknown database 'foodbridge_db'"
**Solution:** Run `sql/schema.sql` in MySQL Workbench

### Issue: "Access denied for user 'root'"
**Solution:** Update PASSWORD in `DBConnection.java` with correct MySQL password

### Issue: "[ERROR] Cannot connect to MySQL"
**Solution:** Ensure MySQL Server is running. Check Services in Windows

### Issue: "Could not find or load main class foodbridge.Main"
**Solution:** Compile first using `javac` command or VS Code tasks

### Issue: Unicode characters show as "?"
**Solution:** Already fixed in code (UTF-8 encoding in javac command)

---

## 🎯 Next Steps

1. Read [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) to understand tables
2. Read [DAO_QUERIES.md](DAO_QUERIES.md) to see all SQL queries
3. Read [VIVA_QA.md](VIVA_QA.md) for exam preparation
4. Read [TESTING.md](TESTING.md) for advanced testing

---

## ✅ Verification Checklist

- [ ] MySQL JDBC JAR in `lib/` folder
- [ ] MySQL Server is running
- [ ] Database `foodbridge_db` created
- [ ] Tables created (5 tables)
- [ ] Sample data inserted
- [ ] Java 17 installed
- [ ] `DBConnection.java` has correct password
- [ ] Project compiled without errors
- [ ] Application runs and shows menu
- [ ] Can login with demo credentials
- [ ] Can add donations and claim them

**All checked?** You're ready to go! 🚀

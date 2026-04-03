# FoodBridge — DBMS Project

## 🌱 Overview

FoodBridge is a **database-driven web application** that connects food donors (restaurants, hotels) with NGOs/charities to eliminate food waste and improve food security.

**Core Mission:** Connect surplus food with those who need it.

---

## 🎯 Project Features

### Donor Features
- ✅ User registration and authentication
- ✅ Add new food donations
- ✅ View own donations with status tracking
- ✅ See platform-wide impact statistics

### NGO Features
- ✅ User registration and authentication
- ✅ View available donations in real-time
- ✅ Claim donations for distribution
- ✅ Track claim history with donor details
- ✅ See impact statistics

### System Features
- ✅ Automatic expiration of stale donations
- ✅ Transaction-based claim processing (all-or-nothing)
- ✅ Impact logging (meals fed estimates)
- ✅ Role-based access control (Donor/NGO)

---

## 📁 Documentation Index

| Document | Purpose |
|----------|---------|
| [SETUP_GUIDE.md](SETUP_GUIDE.md) | Step-by-step installation and configuration |
| [DATABASE_SCHEMA.md](DATABASE_SCHEMA.md) | Database tables, relationships, normalization |
| [DAO_QUERIES.md](DAO_QUERIES.md) | All SQL queries used in the project |
| [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md) | System architecture and design patterns |
| [FEATURES.md](FEATURES.md) | Detailed feature documentation |
| [VIVA_QA.md](VIVA_QA.md) | Common viva questions and answers |
| [TESTING.md](TESTING.md) | How to test the application |
| [PROJECT_STATUS.md](PROJECT_STATUS.md) | Project completion status and fixes applied |

---

## 🚀 Quick Start

1. **Install MySQL JDBC Driver**
   - Place `mysql-connector-j-9.6.0.jar` in `lib/` folder

2. **Create Database**
   - Run `sql/schema.sql` in MySQL Workbench
   - This creates all tables and sample data

3. **Configure Connection**
   - Edit `src/foodbridge/dao/DBConnection.java`
   - Set credentials: username, password, database name

4. **Compile & Run**
   - Press `Ctrl+Shift+B` in VS Code
   - Select: **1 - Compile FoodBridge**
   - Then: **2 - Run FoodBridge**

---

## 📊 Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Database | MySQL 8.0+ |
| JDBC Driver | mysql-connector-j 9.6.0 |
| IDE | VS Code |
| Architecture | 3-Tier (UI → DAO → Models) |

---

## 🔐 Demo Credentials

### Donors
| Email | Password |
|-------|----------|
| saravana@donor.com | donor123 |
| taj@donor.com | donor456 |

### NGOs
| Email | Password |
|-------|----------|
| greenearth@ngo.com | ngo123 |
| helping@ngo.com | ngo456 |

---

## 📁 Project Structure

```
FoodBridge/
├── src/foodbridge/
│   ├── Main.java              ← Entry point
│   ├── models/
│   │   ├── User.java
│   │   └── Donation.java
│   ├── dao/
│   │   ├── DBConnection.java
│   │   ├── UserDAO.java
│   │   └── DonationDAO.java
│   └── ui/
│       ├── DonorUI.java
│       └── NGOUI.java
├── sql/
│   └── schema.sql            ← Database schema + sample data
├── lib/
│   └── mysql-connector-j-9.6.0.jar
├── out/                      ← Compiled .class files
├── doc/
│   └── *.md                  ← Documentation (this folder)
└── .vscode/
    ├── tasks.json            ← Build/run tasks
    └── launch.json           ← Debug configuration
```

---

## 🗄️ Database Overview

5 Tables:
1. **Users** - Donors and NGOs
2. **Food_Items** - Catalogue of food types
3. **Donation_Pool** - All donations (core table)
4. **Claims** - Who claimed what
5. **Impact_Log** - Analytics (meals fed)

**Relationships:**
- Users (1) → Donations (Many)
- Donations (1) → Claims (1, unique)
- Donations → Impact_Log (1, unique)

---

## 💾 Sample Data Provided

- **2 Donors** (Hotels/Restaurants)
- **2 NGOs** (Charities)
- **6 Donations** (Various food items)
- **1 Claim** (Already claimed donation)
- **1 Impact Log** (60 meals fed)

---

## 🎓 DBMS Concepts Demonstrated

✅ **Database Design:** 3NF normalization, relationships, constraints  
✅ **SQL Queries:** INSERT, SELECT, UPDATE, JOIN, aggregate functions, subqueries  
✅ **Transactions:** ACID properties, rollback/commit  
✅ **JDBC:** Connection pooling, PreparedStatement, parameterized queries  
✅ **Architecture:** 3-tier design, separation of concerns  

---

## 📝 Key SQL Features

| Feature | Where Used |
|---------|-----------|
| INSERT | Add donations, register users |
| SELECT with JOIN | View available food, claim history |
| UPDATE | Mark donation as claimed, auto-expire |
| Transaction | Atomic claim operation |
| Aggregate | Impact statistics (COUNT, SUM) |
| AUTO_INCREMENT | Primary keys |
| Foreign Keys | Referential integrity |
| ON DELETE CASCADE | Clean data deletion |

---

## ✅ Project Completion Status

- ✅ All Java source code complete
- ✅ Full database schema with sample data
- ✅ JDBC connection and DAO layer
- ✅ UI for Donor and NGO roles
- ✅ Transactions and error handling
- ✅ Defensive null-checks for missing DB
- ✅ UTF-8 encoding for Unicode characters
- ✅ Compiled and tested end-to-end

---

## 📞 Questions?

Refer to [VIVA_QA.md](VIVA_QA.md) for common exam questions.

For setup help, see [SETUP_GUIDE.md](SETUP_GUIDE.md).

For architecture details, see [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md).

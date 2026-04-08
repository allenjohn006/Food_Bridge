# How to Run FoodBridge

FoodBridge is a **dual-interface** food donation platform:
- 🌐 **Web Dashboard** — Modern browser-based UI (Spring Boot)
- ⌨️ **CLI Interface** — Terminal-based UI (Java/JDBC)

Both share the same database and show real-time updates!

---

## ⚡ Quick Start (Choose One)

### 🌐 Web Application - SHORTCUT (FASTEST)
```bash
mvn spring-boot:run
```
**Then open:** `http://localhost:8080` (takes ~5-10 seconds)

### ⌨️ CLI Application - SHORTCUT (ONE-LINER)
```bash
javac -encoding UTF-8 -cp lib\mysql-connector-j-9.6.0.jar -d out src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java && java -cp "out;lib\mysql-connector-j-9.6.0.jar" foodbridge.Main
```

---

## 📌 Alternative Commands

### 🌐 Web Application (Full Build)
```bash
cd c:\Users\allen\Downloads\FoodBridge
mvn clean package -DskipTests
java -jar target\foodbridge-web-1.0.0.jar
```
**Use when:** You want a deployable JAR file

### ⌨️ CLI Application (Step-by-Step)
```bash
cd c:\Users\allen\Downloads\FoodBridge
javac -encoding UTF-8 -cp lib\mysql-connector-j-9.6.0.jar -d out src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java
java -cp "out;lib\mysql-connector-j-9.6.0.jar" foodbridge.Main
```
**Use when:** You want to debug compile and run separately

---

## 📋 Prerequisites

Before running FoodBridge, ensure you have:

### Required Software
- **Java 17+** — Check: `java -version`
- **Maven 3.9+** — Check: `mvn -version`
- **MySQL 8.0+** — Must be running
- **Git** (optional) — For version control

### Database Setup (One-Time)

1. **Start MySQL Server**
   - Windows: Open MySQL Command Line or workbench
   - Ensure it's running before proceeding

2. **Create Database & Tables**
   ```bash
   mysql -u root -p < sql/schema.sql
   ```
   - Creates `foodbridge_db` database
   - Creates all tables (Users, Donation_Pool, NGO_Requests, Claims, Impact_Log)
   - Inserts sample data with test users

3. **Verify Connection** (if having issues)
   - Edit: `src/main/resources/application.properties`
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/foodbridge_db
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

---

## 🚀 Running the Web Application

The web application runs on **Spring Boot** with embedded Tomcat server.

### Method 1: Maven (Development - Recommended)

1. **Start the application**
   ```bash
   cd c:\Users\allen\Downloads\FoodBridge
   mvn spring-boot:run
   ```
   - Wait for: `Started FoodBridgeWebApplication in X seconds`
   - App runs on: `http://localhost:8080`
   - Stop with: `Ctrl+C`

### Method 2: Build & Run JAR (Production)

1. **Build the project**
   ```bash
   mvn clean package -DskipTests
   ```
   - Output: `target/foodbridge-web-1.0.0.jar` (~23MB)
   - Takes: ~10 seconds

2. **Run the JAR**
   ```bash
   java -jar target\foodbridge-web-1.0.0.jar
   ```
   - App runs on: `http://localhost:8080`
   - Stop with: `Ctrl+C`

### Method 3: VS Code Task (Optional)
1. `Ctrl+Shift+P` → Tasks: Run Task
2. Select "Build and Run FoodBridge"

---

## 🌐 Web Application Login

Once running on `http://localhost:8080`, use these test accounts:

| Role | Email | Password | Purpose |
|------|-------|----------|---------|
| **Donor** | `saravana@donor.com` | `donor123` | Create & track donations |
| **Donor 2** | `taj@donor.com` | `donor456` | (Alternative donor) |
| **NGO** | `greenearth@ngo.com` | `ngo123` | Claim donations, view requests |
| **NGO 2** | `helping@ngo.com` | `ngo456` | (Alternative NGO) |
| **Admin** | `admin@foodbridge.com` | `admin123` | View analytics & platform stats |

### Web Dashboard URLs

| Page | URL | What You Can Do |
|------|-----|--------|
| Login | http://localhost:8080 | Enter credentials |
| Donor Dashboard | http://localhost:8080/donor.html | Create donations, view live NGO requests |
| NGO Dashboard | http://localhost:8080/ngo.html | Claim donations, fulfill requests |
| Admin Dashboard | http://localhost:8080/admin.html | View analytics, donation history |

---

## ⌨️ Running the CLI Application

The CLI (Command-Line Interface) is a terminal-based interface with the same features as the web app.

### 3-Step Setup & Run

**Step 1: Compile the CLI**
```bash
cd c:\Users\allen\Downloads\FoodBridge
javac -encoding UTF-8 -cp lib\mysql-connector-j-9.6.0.jar -d out src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java
```
- **Why UTF-8?** CLI uses Unicode characters (box borders: ╔══╗, emojis: 🍴🤝📊)
- Windows default encoding (windows-1252) can't read these → compilation would fail
- Output: Compiled `.class` files in `out/` folder

**Step 2: Run the CLI**
```bash
java -cp "out;lib\mysql-connector-j-9.6.0.jar" foodbridge.Main
```
- **Important:** Include `lib\mysql-connector-j-9.6.0.jar` in classpath for database access
- Semicolon `;` separates multiple classpath entries

**Step 3: Log In**
Use the same credentials as web app, choose role (1=Donor, 2=NGO, 3=Admin)

### CLI Features

**Donor Menu:**
```
1. Add New Donation
2. View My Donations
3. View Live NGO Requests  ← NEW! See what NGOs need
4. Platform Impact Stats
5. Logout
```

**NGO Menu:**
```
1. View Available Food
2. Claim a Donation
3. My Claim History
4. Platform Impact Stats
5. Logout
```

**Admin Menu:**
```
ℹ️ Admin features available at: http://localhost:8080/admin.html
```
(Admin redirects to web dashboard)

### Live Refresh in CLI

Option 3 (NGO Requests) and Option 1 (Available Food) support **live refresh:**
- Press **Enter** to refresh data
- Type **B** to go back
- Refreshes automatically to show latest requests/donations

---

## 🔄 Real-Time Updates (Both Web & CLI)

Both interfaces update live every **2-3 seconds**:

| Update | Frequency | Example |
|--------|-----------|---------|
| Donations | 2 seconds | New donations appear instantly |
| NGO Requests | 2 seconds | See urgent food needs in real-time |
| Claims | 2 seconds | Watch donations get claimed |
| Expiry | 2-3 seconds | Expired donations auto-marked |
| Analytics | 3 seconds | Stats update without refresh |

**Web:** Check browser DevTools (F12) → Network tab to see API calls  
**CLI:** SQL queries shown in terminal `[SQL] ...`

---

## 🧪 Testing the Application

### Quick Test Flow

**Web Test (5 minutes):**
1. Open two browser tabs
2. Tab 1: Log in as **Donor** (`saravana@donor.com`)
3. Tab 2: Log in as **NGO** (`greenearth@ngo.com`)
4. Tab 1: Create a donation → See it appear in "My Donations"
5. Tab 2: On NGO dash, click "View Available Food" → See the new donation
6. Tab 2: Click "Claim" → Watch donation disappear from Tab 1 (live update!)

**CLI Test (3 minutes):**
1. Compile: `javac -encoding UTF-8 ...` (above)
2. Run: `java -cp "out;lib\mysql-connector-j-9.6.0.jar" foodbridge.Main`
3. Login as Donor (role 1)
4. Select option 3 to see live NGO requests
5. Press Enter to refresh, see SQL queries printed

### Verify Real-Time Updates
```
Web: F12 → Network tab → Observe API calls every 2-3 seconds
CLI: Look for [SQL] log lines showing queries running
```

---

## 🛠️ Troubleshooting

### ❌ MySQL Connection Failed
**Error:** `[ERROR] Database connection unavailable`
- **Cause:** MySQL server not running
- **Fix:**
  1. Start MySQL Server (Windows: Services or MySQL Workbench)
  2. Verify: `mysql -u root -p` (should connect)
  3. Retry the app

### ❌ CLI: "unmappable character" Error During Compile
**Error:** `unmappable character (0x90) for encoding windows-1252`
- **Cause:** Missing UTF-8 encoding flag
- **Fix:** Use full compile command with `-encoding UTF-8`:
  ```bash
  javac -encoding UTF-8 -cp lib\mysql-connector-j-9.6.0.jar -d out src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java
  ```

### ❌ CLI: "ClassNotFoundException: com.mysql.cj.jdbc.Driver"
**Error:** `java.lang.ClassNotFoundException: com.mysql.cj.jdbc.Driver`
- **Cause:** MySQL driver not in classpath
- **Fix:** Run with driver included:
  ```bash
  java -cp "out;lib\mysql-connector-j-9.6.0.jar" foodbridge.Main
  ```

### ❌ Web: Port 8080 Already in Use
**Error:** `Address already in use: bind`
- **Fix Option 1:** Kill existing process
  ```bash
  netstat -ano | findstr :8080
  taskkill /PID <process_id> /F
  ```
- **Fix Option 2:** Use different port
  ```bash
  java -jar target\foodbridge-web-1.0.0.jar --server.port=8081
  ```
  Then open: `http://localhost:8081`

### ❌ Web: Invalid Login Credentials
**Error:** `❌ Invalid credentials`
- **Check:** Credentials in table above (use `saravana@donor.com`, not `donor1@email.com`)
- **Check:** Database initialized: `mysql< -u root -p foodbridge_db` → `SELECT COUNT(*) FROM Users;`

### ❌ Maven Build Fails
**Error:** `[ERROR] BUILD FAILURE`
- **Fix:** Clean and rebuild
  ```bash
  mvn clean install
  ```
  Or skip tests:
  ```bash
  mvn clean package -DskipTests
  ```

---

## 📦 Project Structure

```
FoodBridge/
├── src/main/java/com/foodbridge/web/
│   ├── FoodBridgeWebApplication.java      ← Spring Boot entry
│   ├── controller/ApiController.java       ← REST endpoints
│   ├── service/FoodBridgeService.java      ← Business logic
│   └── dto/                                ← Data Transfer Objects
├── src/main/resources/
│   ├── static/                             ← HTML, CSS, JS (web dashboards)
│   │   ├── index.html, donor.html, ngo.html, admin.html
│   │   ├── app.js                          ← Real-time polling logic
│   │   └── styles.css
│   └── application.properties              ← Database config
├── src/foodbridge/                         ← CLI application
│   ├── Main.java                           ← CLI entry point
│   ├── dao/DonationDAO.java, UserDAO.java  ← Database access
│   ├── models/Donation.java, User.java     ← Data models
│   └── ui/DonorUI.java, NGOUI.java        ← Terminal menus
├── sql/schema.sql                          ← Database schema + sample data
├── lib/mysql-connector-j-9.6.0.jar        ← MySQL JDBC driver
├── pom.xml                                 ← Maven configuration
└── target/foodbridge-web-1.0.0.jar        ← Built JAR (after mvn build)
```

---

## 🌐 REST API Endpoints (Web Backend)

The web app communicates with backend via REST:

**Authentication:**
- `POST /api/register` — Register new user
- `POST /api/login` — User login

**Food Donations:**
- `GET /api/donations` — Get available donations
- `POST /api/donations` — Add new donation
- `GET /api/donations/my-donations` — Get user's donations

**NGO Requests (NEW):**
- `GET /api/requests/open` — Get open NGO requests (live view)
- `POST /api/requests` — Create new NGO request

**Claims & Impact:**
- `POST /api/claims` — Claim a donation
- `GET /api/claims` — Get all claims
- `GET /api/stats` — Get platform statistics

---

## 🎯 Command Reference

### Build Commands
```bash
# Build only (no run)
mvn clean package -DskipTests

# Build + Run (development)
mvn spring-boot:run

# Full build with tests
mvn clean install
```

### Run Commands
```bash
# Web Application (Option 1)
mvn spring-boot:run

# Web Application (Option 2)
java -jar target\foodbridge-web-1.0.0.jar

# Web Application (Different Port)
java -jar target\foodbridge-web-1.0.0.jar --server.port=8081

# CLI Application (Complete)
javac -encoding UTF-8 -cp lib\mysql-connector-j-9.6.0.jar -d out src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java && java -cp "out;lib\mysql-connector-j-9.6.0.jar" foodbridge.Main
```

### Database Commands
```bash
# Initialize database (one-time)
mysql -u root -p < sql/schema.sql

# Login to database
mysql -u root -p

# Check foodbridge database
mysql -u root -p foodbridge_db
```

---

## ✅ Verification Checklist

Before running, verify:

- [ ] Java 17+: `java -version`
- [ ] Maven 3.9+: `mvn -version`
- [ ] MySQL 8.0+ running: `mysql -u root -p`
- [ ] Database created: `sql/schema.sql` run successfully
- [ ] Driver present: `lib/mysql-connector-j-9.6.0.jar` exists
- [ ] Backdoor: Connection works in Java

---

## 📚 Documentation

- **[README.md](README.md)** — Project overview, features, architecture
- **[DEPLOYMENT.md](DEPLOYMENT.md)** — Deploy to GitHub, Docker, Heroku, AWS
- **[LIVE_UPDATES_FLOW.md](LIVE_UPDATES_FLOW.md)** — Real-time polling explained
- **[doc/ARCHITECTURE.md](doc/ARCHITECTURE.md)** — Detailed system design
- **[doc/API_REFERENCE.md](doc/API_REFERENCE.md)** — Full REST API docs
- **[doc/CONTRIBUTING.md](doc/CONTRIBUTING.md)** — Code style guide

---

## 🎯 30-Second Quick Start

### For Web Dashboard (FASTEST)
```bash
mvn spring-boot:run
```
Then open: `http://localhost:8080` and login with `saravana@donor.com` / `donor123`

### For CLI Terminal (ONE-LINER)
```bash
javac -encoding UTF-8 -cp lib\mysql-connector-j-9.6.0.jar -d out src\foodbridge\Main.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java && java -cp "out;lib\mysql-connector-j-9.6.0.jar" foodbridge.Main
```
Then login with `saravana@donor.com` / `donor123` (role 1 for donor)

---

**Last Updated:** April 8, 2026  
**Status:** ✅ Application ready for development & testing

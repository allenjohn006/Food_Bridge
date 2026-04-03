# How to Run FoodBridge

This guide explains how to build and run the FoodBridge application, which includes both a **web-based interface** (Spring Boot) and a **CLI interface** (Terminal-based).

---

## 📋 Prerequisites

Before running FoodBridge, ensure you have the following installed:

### Required Software
- **Java 17+** — Check: `java -version`
- **Maven 3.9+** — Check: `mvn -version`
- **MySQL 8.0+** — Database server must be running
- **Git** — For version control

### Database Setup
1. **Install MySQL** (if not already installed)
   - Windows: Download from [mysql.com](https://dev.mysql.com/downloads/mysql/)
   - Start MySQL Server

2. **Create FoodBridge Database**
   ```sql
   mysql -u root -p < sql/schema.sql
   ```
   This will:
   - Create the `foodbridge` database
   - Create required tables
   - Insert sample data

3. **Update Database Connection** (if needed)
   - Edit `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/foodbridge
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

---

## 🚀 Running the Web Application

### Option 1: Using Maven (Recommended for Development)

1. **Navigate to project directory**
   ```bash
   cd c:\Users\allen\Downloads\FoodBridge
   ```

2. **Run using Maven**
   ```bash
   mvn spring-boot:run
   ```
   - Application starts on `http://localhost:8080`
   - Press `Ctrl+C` to stop

### Option 2: Build JAR and Run

1. **Build the project**
   ```bash
   mvn clean package -DskipTests
   ```
   - Creates `target/foodbridge-web-1.0.0.jar` (~23MB)
   - Takes ~8-10 seconds

2. **Run the JAR**
   ```bash
   java -jar target\foodbridge-web-1.0.0.jar
   ```
   - Application starts on `http://localhost:8080`
   - Output will show: `Started FoodBridgeWebApplication in X seconds`

### Option 3: Using VS Code Tasks

1. Open the Command Palette: `Ctrl+Shift+P`
2. Select `Tasks: Run Task`
3. Choose `Build and Run FoodBridge`
4. OR choose `Run FoodBridge (Existing JAR)`

---

## 🌐 Accessing the Web Application

### Login Credentials

After the application starts on `http://localhost:8080`, use these test accounts:

#### Donor Account
- **Email:** `donor1@email.com`
- **Password:** `password`
- **Access:** Can donate food items

#### NGO Account
- **Email:** `ngo1@email.com`
- **Password:** `password`
- **Access:** Can claim food donations and track meals fed

#### Admin Account
- **Email:** `admin@email.com`
- **Password:** `password`
- **Access:** View analytics, platform statistics, donation/claim history

### Web Interface URLs

| Role   | URL | Purpose |
|--------|-----|---------|
| Donor  | http://localhost:8080/donor.html | Create and track donations |
| NGO    | http://localhost:8080/ngo.html | Claim donations, track meals |
| Admin  | http://localhost:8080/admin.html | View analytics & platform stats |
| Login  | http://localhost:8080 | Main login page |

---

## ⌨️ Running the CLI Application

The CLI (Command-Line Interface) is the original terminal-based interface.

### Prerequisites
- Compile the CLI first:
  ```bash
  javac -d out src\foodbridge\*.java src\foodbridge\dao\*.java src\foodbridge\models\*.java src\foodbridge\ui\*.java
  ```

### Run the CLI
```bash
java -cp out foodbridge.Main
```

### CLI Features
- Donor Interface: Create and track donations
- NGO Interface: View available donations and claim them
- Terminal-based menu system

---

## 🔄 Real-Time Updates

Both web and CLI interfaces show **live updates**:

- **Donations Update:** Every 2 seconds (refreshes donation list)
- **Claims Update:** Every 2 seconds (shows newly claimed items)
- **Expiry Status:** Every 2-3 seconds (auto-marks expired donations)
- **Admin Analytics:** Every 3 seconds (updates statistics)

*No manual refresh needed!*

---

## 🧪 Testing the Application

### Web Application Testing

1. **Test Donor Flow:**
   - Log in as donor
   - Create a donation (fill food details)
   - See it appear in "My Donations"
   - Watch it get claimed by NGO (live update)

2. **Test NGO Flow:**
   - Log in as NGO in another browser tab
   - Click "View Available Donations"
   - Click "Claim" on a donation
   - Watch donation disappear from Donor's list (live update)

3. **Test Admin Dashboard:**
   - Log in as admin
   - View real-time statistics
   - See donation/claim history
   - Monitor platform metrics

### Testing Real-Time Updates
1. Open browser **Developer Tools** (F12)
2. Go to **Network** tab
3. Observe API calls every 2-3 seconds:
   - `/api/donations` — Get donation list
   - `/api/users` — Get user list
   - `/api/claims` — Get claims

---

## 🛠️ Troubleshooting

### Port 8080 Already in Use
```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (Windows)
taskkill /PID <process_id> /F

# Or use a different port
java -jar target\foodbridge-web-1.0.0.jar --server.port=8081
```

### Database Connection Failed
```
ERROR: Access denied for user 'root'@'localhost'
```
**Solution:**
1. Ensure MySQL is running: `mysql -u root -p`
2. Check credentials in `application.properties`
3. Verify database exists: `SHOW DATABASES;`

### Build Fails
```bash
# Clean and rebuild
mvn clean install

# Skip tests if issues persist
mvn clean package -DskipTests
```

### Cannot Find Java
```bash
# Set JAVA_HOME (Windows)
set JAVA_HOME=C:\Program Files\Java\jdk-17
java -version
```

### Application Won't Start
- Check logs for error messages
- Ensure port 8080 is available
- Verify MySQL database is running
- Check file permissions in project directory

---

## 📦 Project Structure

```
FoodBridge/
├── src/main/java/
│   └── com/foodbridge/web/
│       ├── FoodBridgeWebApplication.java (Spring Boot)
│       ├── controller/ApiController.java (REST endpoints)
│       ├── service/FoodBridgeService.java (Business logic)
│       └── dto/ (Data Transfer Objects)
├── src/main/resources/
│   ├── static/ (HTML, CSS, JS files)
│   └── application.properties (Database config)
├── src/foodbridge/ (CLI application)
│   ├── Main.java
│   ├── dao/ (Database access)
│   ├── models/ (Data models)
│   └── ui/ (Terminal UI)
├── sql/schema.sql (Database schema)
├── pom.xml (Maven configuration)
└── target/foodbridge-web-1.0.0.jar (Built application)
```

---

## 🌐 REST API Endpoints

The web application exposes REST endpoints for frontend communication:

### Authentication
- `POST /api/register` — Register new user
- `POST /api/login` — User login
- `GET /api/me` — Get current user

### Donations
- `GET /api/donations` — Get all available donations
- `POST /api/donations` — Create new donation
- `GET /api/donations/my-donations` — Get user's donations

### NGO Operations
- `GET /api/users` — Get all NGOs
- `POST /api/claims` — Claim a donation
- `GET /api/claims` — Get all claims

### Admin
- `GET /api/stats` — Get platform statistics
- `GET /api/admin/donations` — Get all donations
- `GET /api/admin/claims` — Get all claims

*Detailed API documentation in [DEPLOYMENT.md](DEPLOYMENT.md)*

---

## 📚 Additional Resources

- [DEPLOYMENT.md](DEPLOYMENT.md) — Deploy to GitHub, Docker, Heroku, AWS
- [LIVE_UPDATES_FLOW.md](LIVE_UPDATES_FLOW.md) — Real-time polling system explained
- [doc/SETUP_GUIDE.md](doc/SETUP_GUIDE.md) — Detailed setup instructions
- [doc/PROJECT_ARCHITECTURE.md](doc/PROJECT_ARCHITECTURE.md) — System design
- [doc/TESTING.md](doc/TESTING.md) — Comprehensive testing guide

---

## ✅ Verification Checklist

- [ ] Java 17+ installed `java -version`
- [ ] Maven 3.9+ installed `mvn -version`
- [ ] MySQL is running and database created
- [ ] Project builds successfully `mvn clean package -DskipTests`
- [ ] Application starts on `http://localhost:8080`
- [ ] Can log in with test credentials
- [ ] Real-time updates working (check Network tab)
- [ ] Admin dashboard showing statistics

---

## 🎯 Quick Start (30 seconds)

```bash
# 1. Build the project
mvn clean package -DskipTests

# 2. Run the application
java -jar target\foodbridge-web-1.0.0.jar

# 3. Open in browser
# http://localhost:8080

# 4. Log in with: donor1@email.com / password
```

---

**Status:** ✅ Application is currently running on http://localhost:8080

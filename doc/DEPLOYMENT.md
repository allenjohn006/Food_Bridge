# 🚀 GitHub Deployment Guide

## Overview

This guide will help you push FoodBridge to GitHub and make it production-ready.

---

## 📋 Pre-Deployment Checklist

- ✅ All source code is in `src/` folder
- ✅ Database schema is in `sql/schema.sql`
- ✅ Documentation is complete in `doc/` folder
- ✅ `.gitignore` is configured
- ✅ `pom.xml` has all dependencies
- ✅ `README.md` is comprehensive
- ✅ No sensitive data in repository
  - ✅ No passwords in code (use .env pattern)
  - ✅ No API keys
  - ✅ No database credentials in source files

---

## 🔧 Step 1: Clean Up the Repository

### Remove Build Artifacts
```bash
cd /path/to/FoodBridge
rm -rf target/ out/ *.jar *.war
```

### Verify .gitignore is Configured
```bash
cat .gitignore
# Should include: target/, out/, lib/, etc.
```

### Check for Sensitive Files
```bash
# Remove any plain-text credentials
grep -r "password" src/
grep -r "PASSWORD" src/
# If found, move to .env or application-secret.properties
```

---

## 📝 Step 2: Update Documentation

### Main README
✅ Already comprehensive and includes:
- Quick start guide
- Architecture overview
- Feature list
- Tech stack
- Troubleshooting

### Create DEPLOYMENT.md (this file)
Explains how to:
- Set up locally
- Deploy to cloud
- Configure environment

### Create LIVE_UPDATES_FLOW.md
✅ Explains the real-time polling system in detail

### Create CONTRIBUTING.md (Optional)
For future contributors:
```markdown
# Contributing to FoodBridge

## Getting Started
1. Fork the repository
2. Clone your fork: git clone https://github.com/YOUR_USERNAME/FoodBridge.git
3. Follow SETUP_GUIDE.md

## Making Changes
- Create a feature branch: git checkout -b feature/your-feature
- Make your changes
- Test thoroughly
- Commit with clear messages: git commit -m "feat: description"
- Push: git push origin feature/your-feature
- Create a Pull Request

## Code Style
- Follow Java conventions (PascalCase for classes, camelCase for variables)
- Add JSDoc comments for public methods
- Keep methods focused and under 30 lines
```

---

## 📂 Step 3: Organize Repository Structure

Ensure your repo structure matches:
```
FoodBridge/
├── README.md                    ← Main documentation
├── SETUP_GUIDE.md              ← Installation instructions
├── DEPLOYMENT.md               ← This file (cloud deployment)
├── LIVE_UPDATES_FLOW.md        ← Real-time system docs
├── CONTRIBUTING.md             ← For contributors
├── .gitignore                  ← Git ignore rules
├── pom.xml                     ← Maven build file
│
├── src/
│   ├── foodbridge/            ← Original CLI application
│   └── main/                  ← Spring Boot web application
│
├── sql/
│   └── schema.sql
│
├── doc/
│   ├── README.md
│   ├── SETUP_GUIDE.md
│   ├── DATABASE_SCHEMA.md
│   ├── DAO_QUERIES.md
│   ├── PROJECT_ARCHITECTURE.md
│   ├── FEATURES.md
│   ├── VIVA_QA.md
│   ├── TESTING.md
│   └── PROJECT_STATUS.md
│
└── .vscode/
    ├── tasks.json
    └── launch.json
```

---

## 🔐 Step 4: Create GitHub Repository

### On GitHub.com:

1. **Create New Repository**
   - Go to https://github.com/new
   - Repository name: `FoodBridge`
   - Description: `Full-stack food donation platform with live updates`
   - Public (so others can learn from it)
   - Initialize: NO (we'll push existing code)
   - Add .gitignore: NO (we have our own)
   - Add README: NO (we have our own)

2. **Click "Create Repository"**

---

## ⬆️ Step 5: Push to GitHub

### Initial Push (First Time)

```bash
cd /path/to/FoodBridge

# Initialize git (if not already done)
git init

# Add GitHub as remote
git remote add origin https://github.com/YOUR_USERNAME/FoodBridge.git

# Verify remote
git remote -v

# Add all files
git add .

# Initial commit
git commit -m "Initial commit: FoodBridge - Full-stack food donation platform

- Dual architecture: Terminal CLI + Spring Boot web app
- Real-time polling for live updates
- MySQL database with JDBC
- Complete documentation and setup guides"

# Push to main branch
git branch -M main
git push -u origin main
```

### Subsequent Pushes

```bash
git add .
git commit -m "Brief description of changes"
git push origin main
```

---

## 🏷️ Step 6: Add GitHub Tags & Releases

### Create Version Tags

```bash
# Create a tag for v1.0.0
git tag -a v1.0.0 -m "FoodBridge v1.0.0 - Initial Release

Features:
- Dual interface (CLI + Web)
- Real-time donation tracking
- NGO request management
- Live polling system
- Complete documentation"

# Push tags
git push origin v1.0.0

# Or push all tags at once
git push origin --tags
```

### Create GitHub Release

On GitHub.com:
1. Go to "Releases" tab
2. Click "Create a new release"
3. Choose tag: `v1.0.0`
4. Release title: `FoodBridge v1.0.0 - Initial Release`
5. Add release notes confirming features
6. Publish release

---

## 🌐 Step 7: Cloud Deployment (Optional)

### Option A: Deploy to Heroku

```bash
# Install Heroku CLI
# https://devcenter.heroku.com/articles/heroku-cli

# Login to Heroku
heroku login

# Create Heroku app
heroku create your-app-name

# Add Procfile to root (for Heroku to know how to run)
echo "web: java -jar target/foodbridge-web-1.0.0.jar" > Procfile

# Set environment variables on Heroku
heroku config:set SPRING_DATASOURCE_URL="jdbc:mysql://your-db-host:3306/foodbridge_db"
heroku config:set SPRING_DATASOURCE_USERNAME="your_username"
heroku config:set SPRING_DATASOURCE_PASSWORD="your_password"

# Build and deploy
git push heroku main

# View logs
heroku logs --tail
```

### Option B: Deploy to AWS EC2

```bash
# Launch EC2 instance (Ubuntu)

# SSH into instance
ssh -i your-key.pem ubuntu@your-instance-ip

# Install Java & Maven
sudo apt update
sudo apt install -y openjdk-17-jdk maven

# Clone repository
git clone https://github.com/YOUR_USERNAME/FoodBridge.git
cd FoodBridge

# Build
mvn clean package -DskipTests

# Run with nohup (background)
nohup java -jar target/foodbridge-web-1.0.0.jar > app.log 2>&1 &

# Access via: http://your-instance-ip:8080
```

### Option C: Deploy via Docker

Create `Dockerfile`:
```dockerfile
FROM eclipse-temurin:17-jdk-jammy
COPY target/foodbridge-web-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t foodbridge:1.0.0 .
docker run -p 8080:8080 -e SPRING_DATASOURCE_URL="..." foodbridge:1.0.0
```

---

## 📊 Step 8: Set Up GitHub Actions (CI/CD)

Create `.github/workflows/build.yml`:

```yaml
name: Build & Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
    
    - name: Build with Maven
      run: mvn clean compile
    
    - name: Run Tests
      run: mvn test (if tests exist)
    
    - name: Package JAR
      run: mvn package -DskipTests
```

---

## 📖 Step 9: Enable GitHub Pages (Documentation)

To host documentation on GitHub Pages:

1. Go to "Settings" → "Pages"
2. Select source: "Deploy from a branch"
3. Branch: `main`, folder: `/doc` or `/root`
4. Save

Docs will be available at: `https://your-username.github.io/FoodBridge`

---

## ✅ Step 10: Final Verification

### Verify on GitHub

```bash
# Check if repository is accessible
curl https://api.github.com/repos/YOUR_USERNAME/FoodBridge

# Should return public info about your repo
```

### Verify README Rendering
- Go to GitHub page
- Verify README.md displays correctly
- Check that links work properly

### Verify Documentation Links
- All links in README should point to correct files
- doc/ folder should have all documentation

### Verify Code Structure
- All source files are present
- No sensitive data is exposed
- .gitignore is working (target/, out/ not in repo)

---

## 🎯 After Deployment

### Keep Repository Updated

```bash
# Pull latest changes
git pull origin main

# Make changes
# ... edit files ...

# Commit and push
git add .
git commit -m "Description of changes"
git push origin main
```

### Bug Fixes

```bash
git checkout -b bugfix/issue-name
# Fix the bug
git commit -m "Fix: description of fix"
git push origin bugfix/issue-name
# Create Pull Request on GitHub
```

### Version Updates

```bash
# When making a major update, create a new version
git tag -a v1.0.1 -m "Bugfix release"
git push origin v1.0.1
# Create Release on GitHub with changelog
```

---

## 📝 Documentation Checklist for GitHub

- ✅ README.md is comprehensive
- ✅ SETUP_GUIDE.md is complete
- ✅ Database schema documented
- ✅ Architecture explained
- ✅ Features listed
- ✅ Troubleshooting section included
- ✅ Code examples provided
- ✅ Demo credentials listed
- ✅ Contributing guidelines (optional)
- ✅ License file (optional)
- ✅ .gitignore configured

---

## 🔒 Security Best Practices

### Before Pushing

- [ ] No database passwords in code
- [ ] No API keys in source files
- [ ] No private credentials
- [ ] No secret database URLs
- [ ] No personal information

### Use Environment Variables

Instead of hardcoding:
```java
// ❌ Bad
String password = "mySecretPassword123";

// ✅ Good
String password = System.getenv("DB_PASSWORD");
```

Set in application.properties:
```properties
spring.datasource.password=${DB_PASSWORD}
```

Or in `.env` (which is in .gitignore):
```
DB_PASSWORD=mySecretPassword123
```

---

## 🤝 Share Your Project

After pushing to GitHub:

### Add to Social Media
- Tweet about it: "Check out my food donation platform! #opensource #java"
- LinkedIn: Share the link

### Share in Communities
- Dev.to
- Hacker News (Show HN)
- Reddit (r/java, r/webdev, r/learnprogramming)
- Dev communities

### License Recommendation
Add `LICENSE` file (MIT License recommended for learning projects):

```text
MIT License

Copyright (c) 2026 Allen

Permission is hereby granted, free of charge...
[Full MIT license text]
```

---

## 📞 Troubleshooting Deployment

### Issue: "Push rejected"
```
Solution: 
git pull origin main
git merge --allow-unrelated-histories
git push origin main
```

### Issue: ".gitignore not working"
```
Solution: Files already tracked are not affected
git rm --cached target/
git rm --cached out/
git commit -m "Remove build artifacts"
git push origin main
```

### Issue: "Too many files to upload"
```
Solution: Probably committed target/ or node_modules/
git reset HEAD~1
rm -rf target/
git add .
git commit -m "Without build artifacts"
git push origin main
```

---

## 🎉 You're Done!

Your FoodBridge repository is now live on GitHub and ready for:
- Sharing with others
- Portfolio/resume
- Contributions
- Learning reference

**GitHub URL:** `https://github.com/YOUR_USERNAME/FoodBridge`

Happy coding! 🚀


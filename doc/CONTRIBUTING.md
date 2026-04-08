# Contributing to FoodBridge

Thank you for interest in contributing to FoodBridge! This document provides guidelines for contributing to the project.

## 🎯 Mission

FoodBridge connects food donors with NGOs to eliminate food waste. Every contribution helps make this a reality.

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0+
- Git

### Local Setup
```bash
# 1. Clone repository
git clone https://github.com/yourusername/FoodBridge.git
cd FoodBridge

# 2. Setup database
mysql -u root -p < sql/schema.sql

# 3. Build project
mvn clean package -DskipTests

# 4. Run locally
java -jar target/foodbridge-web-1.0.0.jar
```

---

## 📋 How to Contribute

### 1. Report Issues

Found a bug? Create an issue with:
- **Description:** Clear explanation of the issue
- **Steps to Reproduce:** How to trigger the bug
- **Expected vs Actual:** What should happen vs what happens
- **Environment:** Java version, OS, MySQL version

**Example:**
```
Title: Donation doesn't auto-expire after expiry time
Description: A donation with expiry_at = 2026-04-08 10:00 still shows as AVAILABLE at 10:05
Steps: 1. Create donation with past expiry 2. Refresh page 3. Still visible
Expected: Should show EXPIRED
Actual: Still shows AVAILABLE
Environment: Java 17, Windows, MySQL 8.0
```

### 2. Request Features

Submit feature requests with:
- **Title:** Concise description
- **Problem:** What problem does this solve?
- **Solution:** Proposed implementation
- **Alternatives:** Other approaches considered

**Example:**
```
Title: Email notifications when donation claimed
Problem: Donors don't know when their donation is claimed
Solution: Send email via JavaMailSender to donor_email
Alternatives: SMS (would require Twilio), Push notifications
```

### 3. Submit Code Changes

#### Branch Naming Convention
```
feature/short-description          # New features
bugfix/short-description            # Bug fixes
chore/short-description             # Documentation, cleanup
```

#### Development Workflow
```bash
# 1. Create branch
git checkout -b feature/your-feature

# 2. Make changes
# Edit Java, HTML, CSS, JS files

# 3. Test thoroughly
mvn clean package -DskipTests
java -jar target/foodbridge-web-1.0.0.jar

# 4. Test in browser
# Verify both web interface AND CLI

# 5. Commit with clear message
git commit -m "feat: add your feature description

Longer description explaining why this change is needed.

Fixes #123"

# 6. Push and create pull request
git push origin feature/your-feature
```

#### Commit Message Convention
```
<type>: <subject>

<body>

<footer>
```

**Types:**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation only
- `style:` Code style (formatting, semicolons)
- `refactor:` Code restructuring (no behavior change)
- `perf:` Performance improvements
- `test:` Test additions/updates
- `chore:` Build, dependencies, setup

**Example:**
```
feat: add email notifications for claimed donations

Implement JavaMailSender to send emails to donors when their 
donations are claimed. Uses application.properties for SMTP config.

Fixes #45
```

---

## 💻 Code Style Guide

### Java
```java
// Follow Spring Boot conventions
@Service
@Transactional
public class FoodBridgeService {
    
    // Use meaningful names
    public void claimDonation(int donationId, int ngoId) {
        // Descriptive variable names
        boolean isAlreadyClaimed = checkIfClaimed(donationId);
        
        // Comments for complex logic
        if (isAlreadyClaimed) {
            throw new IllegalStateException("Donation already claimed");
        }
    }
    
    // Use PreparedStatements always
    String sql = "UPDATE Donation_Pool SET status = ? WHERE donation_id = ?";
    PreparedStatement ps = conn.prepareStatement(sql);
    ps.setString(1, "CLAIMED");
    ps.setInt(2, donationId);
}
```

### SQL
```java
// Use PreparedStatements for safety
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM Users WHERE email = ? AND password = ?"
);
ps.setString(1, email);
ps.setString(2, password);

// Use meaningful column names
// Avoid SELECT * in production
String sql = "SELECT user_id, name, role, email FROM Users WHERE user_id = ?";

// Add comments for complex queries
// Join to get donor name for display purposes
String sql = """
    SELECT d.donation_id, u.name AS donor_name, f.item_name
    FROM Donation_Pool d
    INNER JOIN Users u ON d.donor_id = u.user_id
    INNER JOIN Food_Items f ON d.item_id = f.item_id
    WHERE d.status = 'AVAILABLE'
""";
```

### JavaScript
```javascript
// Use const by default, let if needed
const emailInput = document.getElementById('email');
let selectedRequest = requestSelect.value;

// Use meaningful function names
async function loadAvailableDonations() {
    try {
        const res = await fetch('/api/donations/available');
        const data = await res.json();
        renderDonationCards(data);
    } catch (err) {
        console.error('Error loading donations:', err);
        showError('Failed to load donations');
    }
}

// Use arrow functions for callbacks
donations.forEach(d => {
    const card = createDonationCard(d);
    container.appendChild(card);
});
```

### HTML
```html
<!-- Use semantic HTML -->
<section class="panel">
    <h2>Available Donations</h2>
    <div id="donations" class="cards"></div>
</section>

<!-- Use meaningful IDs and classes -->
<button id="claimBtn" class="btn-primary">Claim</button>

<!-- Add ARIA labels for accessibility -->
<input type="email" aria-label="Email address" required />
```

---

## ✅ Testing Your Changes

### 1. Build Test
```bash
mvn clean package -DskipTests
```

### 2. Manual Testing Checklist

**For Web Changes:**
- [ ] Test in Chrome, Firefox, Safari
- [ ] Check mobile responsiveness (F12 → Toggle device toolbar)
- [ ] Verify real-time updates (open Network tab, watch polling)
- [ ] Test all user roles (Donor, NGO, Admin)
- [ ] Test edge cases (expired donations, no data, errors)

**For CLI Changes:**
```bash
# Compile with UTF-8 encoding
javac -encoding UTF-8 -d out src/foodbridge/Main.java src/foodbridge/dao/*.java src/foodbridge/models/*.java src/foodbridge/ui/*.java

# Run and test menu options
java -cp out foodbridge.Main
```

**For Database Changes:**
- [ ] Run schema.sql successfully
- [ ] Verify foreign key relationships
- [ ] Test transactions (claim operation)
- [ ] Verify auto-increment
- [ ] Check indexes on frequently queried columns

### 3. Write Tests (If Applicable)
```java
@Test
public void testClaimDonation() {
    // Arrange
    int donationId = 1;
    int ngoId = 2;
    
    // Act
    foodBridgeService.claimDonation(donationId, ngoId);
    
    // Assert
    Donation claimed = donationDAO.getDonation(donationId);
    assertEquals("CLAIMED", claimed.getStatus());
}
```

---

## 📝 Documentation Guidelines

### For Code Changes
- Add Javadoc comments to public methods
- Explain complex logic with inline comments
- Update [API_REFERENCE.md](API_REFERENCE.md) if adding endpoints
- Update [ARCHITECTURE.md](ARCHITECTURE.md) if changing design

**Example:**
```java
/**
 * Claim a donation for an NGO.
 * 
 * This operation is transactional:
 * 1. Updates donation status to CLAIMED
 * 2. Creates claim record linking NGO to donation
 * 3. Logs impact (meals fed estimate)
 * 
 * @param donationId The donation to claim
 * @param ngoId The NGO claiming the donation
 * @throws IllegalStateException if donation already claimed
 */
@Transactional
public void claimDonation(int donationId, int ngoId) {
    // Implementation...
}
```

### For Database Changes
- Update [DATABASE_SCHEMA.md](doc/DATABASE_SCHEMA.md)
- Document new tables/columns
- Explain relationships and constraints
- Update schema.sql with migration

### For Feature Changes
- Update [FEATURES.md](doc/FEATURES.md)
- Add screenshots if UI change
- Document new user flows
- Update [README.md](README.md) if major feature

---

## 🔄 Pull Request Process

1. **Update your branch**
   ```bash
   git fetch origin
   git rebase origin/main
   ```

2. **Create Pull Request on GitHub**
   - Fill out PR template completely
   - Link related issues (#123)
   - Describe your changes clearly

3. **PR Template**
   ```markdown
   ## Description
   Brief explanation of changes
   
   ## Type of Change
   - [ ] Bug fix
   - [ ] New feature
   - [ ] Breaking change
   - [ ] Documentation
   
   ## Testing
   List steps to test this PR
   
   ## Screenshots (if applicable)
   Add images showing the changes
   
   ## Checklist
   - [ ] Code follows style guide
   - [ ] Tests pass
   - [ ] Documentation updated
   - [ ] No new warnings
   ```

4. **Code Review**
   - Maintainers will review your changes
   - Respond to feedback
   - Make requested changes
   - Push updates to the same branch

5. **Merge**
   - Once approved, maintainers merge to main
   - Your branch is deleted
   - Change appears in next release

---

## 🐛 Reporting Security Issues

⚠️ **Do NOT create public issues for security vulnerabilities.**

Instead, email security@foodbridge.local with:
- Vulnerability description
- Steps to reproduce
- Potential impact
- Suggested fix (optional)

---

## 📚 Documentation to Maintain

When contributing, keep these files updated:

| File | When to Update |
|------|----------------|
| [README.md](README.md) | Major features, setup changes |
| [API_REFERENCE.md](API_REFERENCE.md) | New endpoints or parameter changes |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Design pattern or structure changes |
| [DATABASE_SCHEMA.md](doc/DATABASE_SCHEMA.md) | Table or column additions |
| [SETUP_GUIDE.md](doc/SETUP_GUIDE.md) | Installation or dependency changes |

---

## 🎓 Learning Resources

### Project Structure
- [ARCHITECTURE.md](ARCHITECTURE.md) — System design
- [DATABASE_SCHEMA.md](doc/DATABASE_SCHEMA.md) — Database design
- [API_REFERENCE.md](API_REFERENCE.md) — REST endpoints

### Technologies
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [JDBC API](https://docs.oracle.com/en/java/javase/17/docs/api/)
- [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)

---

## ❓ Questions?

- Check [README.md](README.md) first
- Search [GitHub Issues](../../issues)
- Create a new issue with your question

---

## 📜 License

By contributing, you agree that your contributions will be licensed under the same license as this project.

---

**Thank you for contributing to FoodBridge!** 🌱

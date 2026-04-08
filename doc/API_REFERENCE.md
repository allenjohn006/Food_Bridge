# 🌐 FoodBridge REST API Documentation

> Complete REST API reference for FoodBridge v1.0.0

**Base URL:** `http://localhost:8080/api`  
**Content-Type:** `application/json`  
**Response Format:** JSON  

---

## 🔐 Authentication Endpoints

### 1. Register New User

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "role": "DONOR",
  "name": "Taj Restaurant",
  "phone": "9876543210",
  "email": "taj@donor.com",
  "password": "secure_password"
}
```

**Request Fields:**
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| role | String | ✅ | One of: `DONOR`, `NGO`, `ADMIN` |
| name | String | ✅ | Organization/restaurant name |
| phone | String | ✅ | Contact number |
| email | String | ✅ | Must be unique |
| password | String | ✅ | Plain text (⚠️ use hashing in production) |

**Response (Success - 200):**
```json
{
  "message": "User registered successfully",
  "user": {
    "user_id": 4,
    "name": "Taj Restaurant",
    "role": "DONOR",
    "email": "taj@donor.com",
    "phone": "9876543210"
  }
}
```

**Response (Error - 400):**
```json
{
  "message": "Email already exists"
}
```

⚠️ **Note:** Currently passwords stored in plain text. Hash with BCrypt in production.

---

### 2. Login

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "donor1@email.com",
  "password": "password",
  "role": "DONOR"
}
```

**Request Fields:**
| Field | Type | Required |
|-------|------|----------|
| email | String | ✅ |
| password | String | ✅ |
| role | String | ✅ |

**Response (Success - 200):**
```json
{
  "message": "Login successful",
  "user": {
    "user_id": 1,
    "name": "Restaurant A",
    "role": "DONOR",
    "email": "donor1@email.com",
    "phone": "9123456789"
  }
}
```

**Response (Error - 401):**
```json
{
  "message": "Invalid credentials"
}
```

**Frontend Handling:**
```javascript
// After successful login:
localStorage.setItem('fb_user', JSON.stringify(response.user));
// Redirect to dashboard
window.location.href = '/donor.html'; // or '/ngo.html'
```

---

### 3. Get Current User

**Endpoint:** `GET /api/me`

**Headers:**
```
Authorization: Bearer {token}  (optional - uses session from localStorage)
```

**Response (200):**
```json
{
  "user_id": 1,
  "name": "Restaurant A",
  "role": "DONOR",
  "email": "donor1@email.com"
}
```

---

## 🍽️ Donation Endpoints

### 4. Get All Donations

**Endpoint:** `GET /api/donations`

**Query Parameters:** None

**Response (200):**
```json
[
  {
    "donation_id": 1,
    "donor_id": 1,
    "donor_name": "Restaurant A",
    "item_name": "Rice",
    "category": "VEG",
    "quantity": "10 kg",
    "expiry_at": "2026-04-10 18:30",
    "status": "AVAILABLE"
  },
  {
    "donation_id": 2,
    "donor_id": 1,
    "donor_name": "Restaurant A",
    "item_name": "Biryani",
    "category": "NON_VEG",
    "quantity": "20 portions",
    "expiry_at": "2026-04-08 20:00",
    "status": "EXPIRED"
  }
]
```

---

### 5. Get Available Donations (NGO View)

**Endpoint:** `GET /api/donations/available`

**Description:** Returns only AVAILABLE (unclaimed, non-expired) donations. Backend automatically runs auto-expiry before returning.

**Response (200):**
```json
[
  {
    "donation_id": 1,
    "donor_id": 1,
    "donor_name": "Restaurant A",
    "item_name": "Rice",
    "category": "VEG",
    "quantity": "10 kg",
    "expiry_at": "2026-04-10 18:30",
    "status": "AVAILABLE"
  }
]
```

**Frontend Usage:**
```javascript
// Called every 2 seconds by NGO dashboard
const res = await fetch('/api/donations/available');
const donations = await res.json();
// Render in "Available Donations" card section
```

---

### 6. Get Donor's Donations

**Endpoint:** `GET /api/donations/donor/{userId}`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | Integer | Logged-in donor's user_id |

**Response (200):**
```json
[
  {
    "donation_id": 1,
    "donor_id": 1,
    "donor_name": "Restaurant A",
    "item_name": "Rice",
    "quantity": "10 kg",
    "expiry_at": "2026-04-10 18:30",
    "status": "AVAILABLE"
  },
  {
    "donation_id": 2,
    "donor_id": 1,
    "donor_name": "Restaurant A",
    "item_name": "Biryani",
    "quantity": "20 portions",
    "expiry_at": "2026-04-08 20:00",
    "status": "CLAIMED"
  }
]
```

---

### 7. Create Donation

**Endpoint:** `POST /api/donations`

**Request Body:**
```json
{
  "donorId": 1,
  "itemName": "Rice",
  "quantity": "10 kg",
  "expiryAt": "2026-04-10 18:30",
  "requestId": null
}
```

**Request Fields:**
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| donorId | Integer | ✅ | Logged-in user's ID |
| itemName | String | ✅ | E.g., "Rice", "Chicken Curry" |
| quantity | String | ✅ | E.g., "5 kg", "20 portions" |
| expiryAt | String | ✅ | ISO 8601 format: `YYYY-MM-DD HH:MM` |
| requestId | Integer | ❌ | If fulfilling specific NGO request |

**Response (Success - 200):**
```json
{
  "message": "Donation added successfully",
  "donationId": 15,
  "status": "AVAILABLE"
}
```

**Response (Error - 400):**
```json
{
  "message": "Invalid expiry date"
}
```

**Frontend Example:**
```javascript
const req = {
  donorId: me.user_id,
  itemName: document.getElementById('itemName').value,
  quantity: document.getElementById('quantity').value,
  expiryAt: document.getElementById('expiryAt').value,
  requestId: requestIdRaw ? parseInt(requestIdRaw) : null
};

const res = await fetch('/api/donations', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(req)
});
```

---

### 8. Claim Donation

**Endpoint:** `POST /api/donations/{donationId}/claim`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| donationId | Integer | ID of donation to claim |

**Request Body:**
```json
{
  "ngoId": 2
}
```

**Response (Success - 200):**
```json
{
  "message": "Donation claimed successfully",
  "claimId": 5,
  "mealsFed": 20
}
```

**Response (Error - 400):**
```json
{
  "message": "Donation already claimed or expired"
}
```

**Database Operations (Atomic Transaction):**
1. UPDATE Donation_Pool SET status='CLAIMED'
2. INSERT INTO Claims (donation_id, ngo_id)
3. INSERT INTO Impact_Log (meals_fed estimate)

---

## 🤝 NGO Request Endpoints

### 9. Get Open Requests

**Endpoint:** `GET /api/requests/open`

**Description:** Returns all OPEN (not yet fulfilled) NGO food requests. Used by donor dashboard.

**Response (200):**
```json
[
  {
    "request_id": 1,
    "ngo_name": "Green Earth NGO",
    "ngo_id": 2,
    "item_name": "Rice",
    "quantity_needed": "50 kg",
    "notes": "For shelter distribution",
    "status": "OPEN"
  },
  {
    "request_id": 2,
    "ngo_name": "Helping Hands",
    "ngo_id": 3,
    "item_name": "Vegetables",
    "quantity_needed": "100 kg",
    "notes": "Daily meal prep",
    "status": "OPEN"
  }
]
```

**Frontend Usage (Donor Dashboard):**
```javascript
const res = await fetch('/api/requests/open');
const requests = await res.json();
// Populate dropdown: "Fulfill NGO Request"
// Allow donor to select and match with their donation
```

---

### 10. Create NGO Request

**Endpoint:** `POST /api/ngo/requests`

**Request Body:**
```json
{
  "ngoId": 2,
  "itemName": "Rice",
  "quantityNeeded": "50 kg",
  "notes": "For shelter distribution"
}
```

**Request Fields:**
| Field | Type | Required |
|-------|------|----------|
| ngoId | Integer | ✅ |
| itemName | String | ✅ |
| quantityNeeded | String | ✅ |
| notes | String | ❌ |

**Response (Success - 200):**
```json
{
  "message": "Request created successfully",
  "requestId": 5
}
```

---

## 🏆 Claim & Impact Endpoints

### 11. Get NGO Claims

**Endpoint:** `GET /api/ngo/{ngoId}/claims`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| ngoId | Integer | NGO's user_id |

**Response (200):**
```json
[
  {
    "claim_id": 1,
    "item_name": "Rice",
    "quantity": "10 kg",
    "donor_name": "Restaurant A",
    "claimed_at": "2026-04-08 14:30",
    "meals_fed": 20
  },
  {
    "claim_id": 2,
    "item_name": "Biryani",
    "quantity": "20 portions",
    "donor_name": "Hotel B",
    "claimed_at": "2026-04-08 15:45",
    "meals_fed": 40
  }
]
```

---

## 👨‍💼 Admin Analytics Endpoints

### 12. Get Platform Statistics

**Endpoint:** `GET /api/stats`

**Description:** Real-time platform analytics. Called every 3 seconds by admin dashboard.

**Response (200):**
```json
{
  "total_donations": 45,
  "claimed_count": 18,
  "available_count": 12,
  "expired_count": 15,
  "total_meals_fed": 340,
  "open_requests": 4,
  "total_users": 8,
  "total_ngos": 3,
  "total_donors": 4
}
```

---

### 13. Get All Users (Admin)

**Endpoint:** `GET /api/admin/users`

**Response (200):**
```json
[
  {
    "user_id": 1,
    "name": "Restaurant A",
    "role": "DONOR",
    "phone": "9123456789",
    "email": "donor1@email.com",
    "created_at": "2026-04-01 10:00"
  },
  {
    "user_id": 2,
    "name": "Green Earth NGO",
    "role": "NGO",
    "phone": "9987654321",
    "email": "ngo1@email.com",
    "created_at": "2026-04-01 11:30"
  }
]
```

---

### 14. Get All Donations (Admin)

**Endpoint:** `GET /api/admin/donations`

**Response (200):**
```json
[
  {
    "donation_id": 1,
    "donor_id": 1,
    "donor_name": "Restaurant A",
    "item_name": "Rice",
    "quantity": "10 kg",
    "status": "AVAILABLE",
    "expiry_at": "2026-04-10 18:30",
    "created_at": "2026-04-08 12:00"
  }
]
```

---

### 15. Get All Claims (Admin)

**Endpoint:** `GET /api/admin/claims`

**Response (200):**
```json
[
  {
    "claim_id": 1,
    "donation_id": 1,
    "donor_name": "Restaurant A",
    "ngo_name": "Green Earth NGO",
    "item_name": "Rice",
    "quantity": "10 kg",
    "claim_time": "2026-04-08 14:30",
    "meals_fed": 20
  }
]
```

---

### 16. Get All Requests (Admin)

**Endpoint:** `GET /api/admin/requests`

**Response (200):**
```json
[
  {
    "request_id": 1,
    "ngo_id": 2,
    "ngo_name": "Green Earth NGO",
    "item_name": "Rice",
    "quantity_needed": "50 kg",
    "status": "OPEN",
    "created_at": "2026-04-08 10:00"
  }
]
```

---

### 17. Manually Trigger Expiry (Admin)

**Endpoint:** `POST /api/admin/expire`

**Description:** Force-run auto-expiry logic (normally runs automatically before each SELECT).

**Request Body:**
```json
{}
```

**Response (200):**
```json
{
  "message": "Expired donations updated",
  "expiredCount": 3
}
```

---

## 📊 Error Responses

### **400 Bad Request**
```json
{
  "message": "Invalid request body",
  "details": "Field 'expiryAt' is required"
}
```

### **401 Unauthorized**
```json
{
  "message": "Invalid credentials or session expired",
  "code": "UNAUTHORIZED"
}
```

### **403 Forbidden**
```json
{
  "message": "You don't have permission to perform this action",
  "code": "FORBIDDEN"
}
```

### **404 Not Found**
```json
{
  "message": "Donation not found",
  "code": "NOT_FOUND"
}
```

### **500 Internal Server Error**
```json
{
  "message": "Database error",
  "code": "DATABASE_ERROR"
}
```

---

## 🔄 Complete Example: Donor → NGO Workflow

### **Step 1: Donor Logs In**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "donor1@email.com",
    "password": "password",
    "role": "DONOR"
  }'
```

### **Step 2: Donor Posts Donation**
```bash
curl -X POST http://localhost:8080/api/donations \
  -H "Content-Type: application/json" \
  -d '{
    "donorId": 1,
    "itemName": "Rice",
    "quantity": "10 kg",
    "expiryAt": "2026-04-10 18:30",
    "requestId": null
  }'
```

### **Step 3: NGO Fetches Available Donations**
```bash
curl -X GET http://localhost:8080/api/donations/available
```

### **Step 4: NGO Claims Donation**
```bash
curl -X POST http://localhost:8080/api/donations/1/claim \
  -H "Content-Type: application/json" \
  -d '{ "ngoId": 2 }'
```

### **Step 5: NGO Checks Claim History**
```bash
curl -X GET http://localhost:8080/api/ngo/2/claims
```

### **Step 6: Admin Views Statistics**
```bash
curl -X GET http://localhost:8080/api/stats
```

---

## 🧪 Testing with Postman

1. **Import Collection:** Use the endpoints above
2. **Set Base URL:** `http://localhost:8080/api`
3. **Set Variables:**
   - `donorId` = 1
   - `ngoId` = 2
   - `donationId` = (varies)
4. **Test Flow:**
   - Register → Login → Create Donation → Claim → View Claims

---

## ⚡ Performance Tips

| Endpoint | Typical Time | Notes |
|----------|------------|-------|
| /api/donations | 50-100ms | Single table SELECT |
| /api/donations/available | 100-200ms | Includes auto-expiry + JOIN |
| /api/donations/{id}/claim | 150-300ms | Transaction (3 operations) |
| /api/stats | 100-150ms | Aggregate query |
| /api/admin/donations | 200-300ms | Large result set |

---

## 🔒 Security Headers

In production, add to responses:
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

---

This API documentation is accurate as of **v1.0.0** (April 2026).

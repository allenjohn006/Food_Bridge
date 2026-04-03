# ⚡ Live Updates & Real-Time Polling System

## Overview

FoodBridge uses a **client-side polling mechanism** to provide real-time updates across all dashboards without requiring server-pushed notifications like WebSockets or Server-Sent Events.

**Polling Interval:** 2-3 seconds per dashboard  
**Technology:** Vanilla JavaScript `setInterval()` + Fetch API  
---

## 🔄 How It Works

### 1. **Frontend Polling** (app.js)

Each dashboard continuously calls specific API endpoints at fixed intervals:

```javascript
// Donor Dashboard - polls every 2 seconds
setInterval(loadRequests, 2000);      // /api/requests/open
setInterval(loadDonations, 2000);     // /api/donations/donor/{id}

// NGO Dashboard - polls every 2 seconds  
setInterval(loadAvailable, 2000);     // /api/donations/available
setInterval(loadClaims, 2000);        // /api/ngo/{id}/claims

// Admin Dashboard - polls every 3 seconds
setInterval(loadStats, 3000);         // /api/stats
setInterval(loadUsers, 3000);         // /api/admin/users
setInterval(loadDonations, 3000);     // /api/admin/donations
setInterval(loadRequests, 3000);      // /api/admin/requests
```

### 2. **Backend Auto-Expiry** (FoodBridgeService.java)

Before each read operation, stale donations are automatically expired:

```java
public List<Map<String, Object>> getAvailableDonations() {
    autoExpireDonations();  // ← UPDATE expired items BEFORE reading
    String sql = "SELECT ... WHERE status = 'AVAILABLE' AND expiry_at > NOW()";
    return jdbc.queryForList(sql);
}
```

Auto-expiry is triggered on every read of:
- Available donations (NGO view)
- Donor's donations (Donor page refresh)
- Platform stats (Admin dashboard)
- All donations (Admin table)

### 3. **Frontend Dynamic Updates** (app.js)

When poll response arrives, the UI is immediately updated:

```javascript
async function loadRequests() {
    const res = await api.get('/api/requests/open');
    const wrap = byId('openRequests');
    wrap.innerHTML = '';  // ← Clear old data
    
    res.data.forEach(r => {
        const card = document.createElement('div');
        card.innerHTML = `...`;  // ← Build new HTML
        wrap.appendChild(card);   // ← Append to DOM
    });
}
```

---

## 📊 End-to-End Flow Example: "Fulfill NGO Request"

```
TIME 0s: Donor clicks "Post Donation" button
         ├─→ Payload sent: { itemName, quantity, expiryAt, requestId }
         ├─→ Backend: Saves to Donation_Pool
         ├─→ Backend: Updates NGO_Requests status = 'FULFILLED'
         ├─→ Frontend: loadDonations() refreshes immediately
         └─→ Frontend: loadRequests() refreshes immediately

TIME 1s: NGO Dashboard (in another tab)
         └─→ loadRequests() polls /api/requests/open
             (Returns OPEN requests, no fulfilled ones)

TIME 2s: NGO sees updated list WITHOUT refreshing page
         (Request disappeared from Live NGO Requests)

TIME 3s: Donor Dashboard (first tab)
         └─→ loadDonations() polls /api/donations/donor/{id}
             (Returns donations, fulfilled one shows CLAIMED status)

TIME 4s: Donor sees donation status as CLAIMED instantly

TIME 5s: Admin Dashboard
         └─→ loadStats() polls /api/stats
             (Calculates fresh counts)

TIME 6s: Admin sees updated metrics live
```

---

## 🎯 What Updates in Real-Time

### Donor Dashboard

| Element | Updates When | Poll Interval |
|---------|--------------|---------------|
| Live NGO Requests | New request created OR fulfilled | 2 seconds |
| My Donations table | Donation claimed OR expires | 2 seconds |
| Fulfill dropdown | NGO request list changes | 2 seconds |

### NGO Dashboard

| Element | Updates When | Poll Interval |
|---------|--------------|---------------|
| Available Donations | Another NGO claims OR new donation posted | 2 seconds |
| My Claims table | I successfully claim a donation | 2 seconds |
| Live donation list | Donation expires (auto-status update) | 2 seconds |

### Admin Dashboard

| Element | Updates When | Poll Interval |
|---------|--------------|---------------|
| Stats Cards | Any donation claimed/expired OR meal impact added | 3 seconds |
| Users table | New user registers | 3 seconds |
| Donations table | Any status change (CLAIMED, EXPIRED, etc.) | 3 seconds |
| Recent Requests | New NGO request created | 3 seconds |

---

## 🔧 Backend Endpoints for Polling

### **Donor Endpoints**

```
GET /api/requests/open
├─ Runs: autoExpireDonations()
├─ Returns: Array of open NGO requests
└─ Columns: request_id, ngo_name, item_name, quantity_needed, notes

GET /api/donations/donor/{donorId}
├─ Runs: autoExpireDonations()
├─ Returns: All donations by this donor
└─ Columns: donation_id, item_name, quantity, expiry_at, status
```

### **NGO Endpoints**

```
GET /api/donations/available
├─ Runs: autoExpireDonations()
├─ Returns: Donations with status='AVAILABLE' and not expired
└─ Columns: donation_id, donor_name, item_name, quantity, expiry_at

GET /api/ngo/{ngoId}/claims
├─ Returns: All claims made by this NGO
└─ Columns: claim_id, item_name, quantity, donor_name, claimed_at, meals_fed
```

### **Admin Endpoints**

```
GET /api/stats
├─ Runs: autoExpireDonations()
├─ Returns: Platform-wide statistics
└─ Includes: total_donations, claimed_count, available_count, expired_count, total_meals_fed

GET /api/admin/donations
├─ Runs: autoExpireDonations()
├─ Returns: All donations in database
└─ Columns: donation_id, donor_name, item_name, quantity, status, expiry_at

GET /api/admin/users
├─ Returns: All registered users
└─ Columns: user_id, name, role, phone, email, created_at

GET /api/admin/requests
├─ Returns: All NGO requests (OPEN and FULFILLED)
└─ Columns: request_id, item_name, ngo_name, quantity_needed, status
```

---

## ⏱️ Polling Interval Rationale

| Dashboard | Interval | Reason |
|-----------|----------|--------|
| Donor | 2 seconds | Fast response to NGO claims on donations |
| NGO | 2 seconds | Quick visibility of new donations & claim success |
| Admin | 3 seconds | Slightly less critical; analytics can have small delay |

- **Why not 1 second?** Database/UI update load, unnecessary complexity
- **Why not 5+ seconds?** Would feel slow/non-responsive to users
- **Why not WebSockets?** Overkill for micro-project; polling is simpler

---

## 🐛 Live Update Verification Checklist

### Test 1: NGO Request Fulfillment ✓

1. Open 2 tabs: Donor (Tab A) + NGO (Tab B)
2. In Tab B: Create NGO request ("Rice - 20 portions")
3. Verify: Tab A shows new request in "Live NGO Requests" within 2 seconds
4. In Tab A: Post donation matching the request
5. Verify: Tab B's "Live NGO Requests" no longer shows request within 2 seconds

### Test 2: Donation Claim ✓

1. Open 2 tabs: Donor (Tab A) + NGO (Tab B)
2. In Tab A: Post donation (not linked to request)
3. In Tab B: Wait 2 seconds, see donation appear
4. In Tab B: Click "Claim"
5. Verify: Tab A "My Donations" shows status = "CLAIMED" within 2 seconds
6. Verify: Tab B "My Claims" shows new claim within 2 seconds

### Test 3: Auto-Expiry on Poll ✓

1. In Donor tab: Post donation with expiry = NOW + 1 minute
2. Wait for expiry time to pass (or edit DB expiry_at directly to past)
3. Wait < 2 seconds
4. Refresh poll occurs automatically
5. Verify: Status shows "EXPIRED" without page reload

### Test 4: Admin Real-Time Stats ✓

1. Open 3 tabs: Admin (Tab C), Donor (Tab A), NGO (Tab B)
2. In Tab C: Note current "Meals Fed" count
3. In Tab B: Claim a donation
4. Wait 3 seconds
5. Verify: Tab C "Meals Fed" count increases without refresh

---

## 💾 Database Impact of Live Updates

Each poll = 1 SELECT query to DB:

```
Donor Dashboard (2 queries × 2 second interval):
  - Per minute: 60 queries
  - Per hour: 3,600 queries

NGO Dashboard (2 queries × 2 second interval):  
  - Per minute: 60 queries
  - Per hour: 3,600 queries

Admin Dashboard (4 queries × 3 second interval):
  - Per minute: 80 queries  
  - Per hour: 4,800 queries

Typical setup (5 concurrent users):
  - Total: ~12,000 queries/hour
  - Average DB load: Very low (SELECT queries only)
```

**Note:** All queries include autoExpireDonations() UPDATE, but UPDATE only happens if WHERE condition matches (i.e., stale items exist).

---

## 🚀 Future Improvements

### Short-term
- Add error handling for failed polls (retry logic)
- Show connection status indicator
- Disable polling when user leaves tab (via `visibilitychange` event)

### Long-term
- Implement WebSockets for true real-time (no polling latency)
- Add Server-Sent Events (SSE) for lightweight push
- Cache frequently-requested data
- Add polling "smart retry" (reduce frequency on network errors)

---

## 📝 Code References

- **Frontend polling:** [src/main/resources/static/app.js](src/main/resources/static/app.js#L160)
- **Backend auto-expiry:** [src/main/java/com/foodbridge/web/service/FoodBridgeService.java](src/main/java/com/foodbridge/web/service/FoodBridgeService.java#L52)
- **API endpoints:** [src/main/java/com/foodbridge/web/controller/ApiController.java](src/main/java/com/foodbridge/web/controller/ApiController.java)

---

## ❓ FAQ

**Q: Why do I see a 2-second delay sometimes?**
A: Polling intervals are staggered. Worst-case delay is ~2 seconds between action and UI update.

**Q: Can polling cause performance issues?**
A: With 5-10 concurrent users, no. With 100+ users, yes — would need to move to WebSockets.

**Q: What if network drops during poll?**
A: Current version silently skips that poll cycle. Next poll will try again.

**Q: How do I disable polling (e.g., for testing)?**
A: Edit `app.js` and comment out the `setInterval()` calls, then manually call `loadX()` functions.

**Q: Does polling work on every browser?**
A: Yes. Uses `fetch()` API which is supported in all modern browsers (IE11+ with polyfill).


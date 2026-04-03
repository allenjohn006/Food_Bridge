const api = {
  post: async (url, body) => {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const json = await res.json();
    return { ok: res.ok, data: json };
  },
  get: async (url) => {
    const res = await fetch(url);
    const json = await res.json();
    return { ok: res.ok, data: json };
  }
};

const me = JSON.parse(localStorage.getItem('fb_user') || 'null');

function logout() {
  localStorage.removeItem('fb_user');
  window.location.href = '/index.html';
}

function byId(id) {
  return document.getElementById(id);
}

if (window.location.pathname.endsWith('/index.html') || window.location.pathname === '/') {
  const loginTab = byId('loginTab');
  const registerTab = byId('registerTab');
  const loginForm = byId('loginForm');
  const registerForm = byId('registerForm');
  const authMsg = byId('authMsg');

  loginTab?.addEventListener('click', () => {
    loginTab.classList.add('active');
    registerTab.classList.remove('active');
    loginForm.classList.remove('hidden');
    registerForm.classList.add('hidden');
  });

  registerTab?.addEventListener('click', () => {
    registerTab.classList.add('active');
    loginTab.classList.remove('active');
    registerForm.classList.remove('hidden');
    loginForm.classList.add('hidden');
  });

  loginForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const req = {
      role: byId('loginRole').value,
      email: byId('loginEmail').value,
      password: byId('loginPassword').value
    };

    const res = await api.post('/api/auth/login', req);
    if (!res.ok) {
      authMsg.textContent = res.data.message || 'Login failed';
      return;
    }

    localStorage.setItem('fb_user', JSON.stringify(res.data.user));
    const role = res.data.user.role;
    if (role === 'DONOR') window.location.href = '/donor.html';
    if (role === 'NGO') window.location.href = '/ngo.html';
    if (role === 'ADMIN') window.location.href = '/admin.html';
  });

  registerForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const req = {
      role: byId('regRole').value,
      name: byId('regName').value,
      phone: byId('regPhone').value,
      email: byId('regEmail').value,
      password: byId('regPassword').value
    };
    const res = await api.post('/api/auth/register', req);
    authMsg.textContent = res.data.message;
  });
}

if (window.location.pathname.endsWith('/donor.html')) {
  if (!me || me.role !== 'DONOR') window.location.href = '/index.html';
  byId('whoami').textContent = `${me.name} (${me.email})`;

  const msg = byId('donorMsg');
  const requestSelect = byId('requestId');

  async function loadRequests() {
    try {
      const res = await api.get('/api/requests/open');
      const wrap = byId('openRequests');
      wrap.innerHTML = '';
      requestSelect.innerHTML = '<option value="">None</option>';

      if (!res.ok || !res.data) {
        console.log('Failed to load requests or empty response');
        return;
      }

      if (res.data.length === 0) {
        wrap.innerHTML = '<div class="meta" style="padding: 20px; text-align: center;">No open requests</div>';
        return;
      }

      res.data.forEach(r => {
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `<strong>${r.item_name}</strong> <span class="meta">(${r.quantity_needed})</span><div class="meta">${r.ngo_name} | ${r.notes || ''}</div>`;
        wrap.appendChild(card);

        const opt = document.createElement('option');
        opt.value = r.request_id;
        opt.textContent = `#${r.request_id} ${r.ngo_name} -> ${r.item_name} (${r.quantity_needed})`;
        requestSelect.appendChild(opt);
      });
      console.log('Loaded ' + res.data.length + ' open requests');
    } catch (err) {
      console.error('Error loading requests:', err);
    }
  }

  async function loadDonations() {
    const res = await api.get(`/api/donations/donor/${me.user_id}`);
    const body = byId('donorDonations');
    body.innerHTML = '';
    res.data.forEach(d => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${d.donation_id}</td><td>${d.item_name}</td><td>${d.category}</td><td>${d.quantity}</td><td>${d.expiry_at}</td><td>${d.status}</td>`;
      body.appendChild(tr);
    });
  }

  byId('donationForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const requestIdRaw = requestSelect.value;
    const payload = {
      donorId: me.user_id,
      itemName: byId('itemName').value,
      quantity: byId('quantity').value,
      expiryAt: byId('expiryAt').value,
      requestId: requestIdRaw ? parseInt(requestIdRaw, 10) : null
    };

    const res = await api.post('/api/donations', payload);
    msg.textContent = res.data.message;
    if (res.ok) {
      byId('donationForm').reset();
      await loadDonations();
      await loadRequests();
    }
  });

  loadRequests();
  loadDonations();
  setInterval(loadRequests, 2000);
  setInterval(loadDonations, 2000);
}

if (window.location.pathname.endsWith('/ngo.html')) {
  if (!me || me.role !== 'NGO') window.location.href = '/index.html';
  byId('whoami').textContent = `${me.name} (${me.email})`;

  const msg = byId('ngoMsg');

  async function loadAvailable() {
    try {
      const res = await api.get('/api/donations/available');
      const wrap = byId('availableDonations');
      wrap.innerHTML = '';

      if (!res.ok || !res.data) {
        console.log('Failed to load available donations');
        return;
      }

      if (res.data.length === 0) {
        wrap.innerHTML = '<div class="meta" style="padding: 20px; text-align: center;">No available donations</div>';
        return;
      }

      res.data.forEach(d => {
        const card = document.createElement('div');
        card.className = 'card';
        card.innerHTML = `
          <strong>${d.item_name}</strong>
          <div class="meta">Donor: ${d.donor_name}</div>
          <div class="meta">Qty: ${d.quantity} | Expiry: ${d.expiry_at}</div>
          <button data-id="${d.donation_id}">Claim</button>
        `;
        card.querySelector('button').addEventListener('click', async () => {
          const claimRes = await api.post(`/api/donations/${d.donation_id}/claim`, { ngoId: me.user_id });
          msg.textContent = claimRes.data.message;
          await loadAvailable();
          await loadClaims();
        });
        wrap.appendChild(card);
      });
      console.log('Loaded ' + res.data.length + ' available donations');
    } catch (err) {
      console.error('Error loading available donations:', err);
    }
  }

  async function loadClaims() {
    const res = await api.get(`/api/ngo/${me.user_id}/claims`);
    const body = byId('ngoClaims');
    body.innerHTML = '';
    res.data.forEach(c => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${c.claim_id}</td><td>${c.item_name}</td><td>${c.quantity}</td><td>${c.donor_name}</td><td>${c.claimed_at}</td><td>${c.meals_fed}</td>`;
      body.appendChild(tr);
    });
  }

  byId('needForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
      ngoId: me.user_id,
      itemName: byId('needItem').value,
      quantityNeeded: byId('needQty').value,
      notes: byId('needNotes').value
    };
    const res = await api.post('/api/ngo/requests', payload);
    msg.textContent = res.data.message;
    if (res.ok) {
      byId('needForm').reset();
      await loadAvailable();
      await loadClaims();
    }
  });

  loadAvailable();
  loadClaims();
  setInterval(loadAvailable, 2000);
  setInterval(loadClaims, 2000);
}

if (window.location.pathname.endsWith('/admin.html')) {
  if (!me || me.role !== 'ADMIN') window.location.href = '/index.html';
  byId('whoami').textContent = `${me.name} (${me.email})`;

  const msg = byId('adminMsg');

  async function loadStats() {
    const res = await api.get('/api/stats');
    const stats = res.data;
    const wrap = byId('statsCards');
    wrap.innerHTML = `
      <div class="card"><strong>${stats.total_donations}</strong><div class="meta">Total Donations</div></div>
      <div class="card"><strong>${stats.claimed_count}</strong><div class="meta">Claimed</div></div>
      <div class="card"><strong>${stats.available_count}</strong><div class="meta">Available</div></div>
      <div class="card"><strong>${stats.expired_count}</strong><div class="meta">Expired</div></div>
      <div class="card"><strong>${stats.total_meals_fed}</strong><div class="meta">Meals Fed</div></div>
      <div class="card"><strong>${stats.open_requests}</strong><div class="meta">Open NGO Requests</div></div>
    `;
  }

  async function loadUsers() {
    const res = await api.get('/api/admin/users');
    const body = byId('adminUsers');
    body.innerHTML = '';
    res.data.forEach(u => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${u.user_id}</td><td>${u.name}</td><td>${u.role}</td><td>${u.phone || ''}</td><td>${u.email}</td><td>${u.created_at}</td>`;
      body.appendChild(tr);
    });
  }

  async function loadDonations() {
    const res = await api.get('/api/admin/donations');
    const body = byId('adminDonations');
    body.innerHTML = '';
    res.data.forEach(d => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${d.donation_id}</td><td>${d.donor_name}</td><td>${d.item_name}</td><td>${d.quantity}</td><td>${d.status}</td><td>${d.expiry_at}</td><td>${d.created_at}</td>`;
      body.appendChild(tr);
    });
  }

  async function loadRequests() {
    const res = await api.get('/api/admin/requests');
    const wrap = byId('adminRequests');
    wrap.innerHTML = '';
    res.data.slice(0, 8).forEach(r => {
      const card = document.createElement('div');
      card.className = 'card';
      card.innerHTML = `<strong>#${r.request_id} ${r.item_name}</strong><div class="meta">${r.ngo_name} | ${r.quantity_needed}</div><div class="meta">Status: ${r.status}</div>`;
      wrap.appendChild(card);
    });
  }

  byId('expireNow').addEventListener('click', async () => {
    const res = await api.post('/api/admin/expire', {});
    msg.textContent = res.data.message;
    await loadStats();
    await loadDonations();
  });

  loadStats();
  loadUsers();
  loadDonations();
  loadRequests();
  setInterval(loadStats, 3000);
  setInterval(loadUsers, 3000);
  setInterval(loadDonations, 3000);
  setInterval(loadRequests, 3000);
}

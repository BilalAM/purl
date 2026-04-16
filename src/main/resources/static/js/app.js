let currentUser = null;
let authMode = 'login';

// === CSRF Helper ===
function getCsrfToken() {
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}

function secureHeaders(extra = {}) {
    const headers = {'Content-Type': 'application/json', ...extra};
    const csrf = getCsrfToken();
    if (csrf) headers['X-XSRF-TOKEN'] = csrf;
    return headers;
}

fetch('/api/auth/me', {credentials: 'same-origin'}).then(r => {
    if (r.ok) return r.json(); else throw 'not logged in';
}).then(data => {
    currentUser = data.username;
    onLoginSuccess();
}).catch(() => {});

// === Scroll Animations ===
const scrollObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            entry.target.classList.add('visible');
        }
    });
}, {threshold: 0.1});

document.querySelectorAll('.animate-in').forEach(el => scrollObserver.observe(el));

// === Auth Modal ===
function openModal(mode) {
    authMode = mode;
    switchTab(mode);
    document.getElementById('authModal').classList.add('show');
    document.getElementById('authUsername').value = '';
    document.getElementById('authPassword').value = '';
    hideModalError();
}

function closeModal() {
    document.getElementById('authModal').classList.remove('show');
}

function switchTab(mode) {
    authMode = mode;
    document.getElementById('tabLogin').classList.toggle('active', mode === 'login');
    document.getElementById('tabRegister').classList.toggle('active', mode === 'register');
    document.getElementById('authSubmitBtn').textContent = mode === 'login' ? 'Log In' : 'Sign Up';
}

function showModalError(msg) {
    const el = document.getElementById('modalError');
    el.textContent = msg;
    el.classList.add('show');
}

function hideModalError() {
    document.getElementById('modalError').classList.remove('show');
}

async function submitAuth() {
    const username = document.getElementById('authUsername').value.trim();
    const password = document.getElementById('authPassword').value.trim();
    if (!username || !password) {
        showModalError('Please fill in all fields');
        return;
    }

    const endpoint = authMode === 'login' ? '/api/auth/login' : '/api/auth/register';
    try {
        const res = await fetch(endpoint, {
            method: 'POST', credentials: 'same-origin',
            headers: secureHeaders(),
            body: JSON.stringify({username, password})
        });
        const data = await res.json();
        if (!res.ok) {
            showModalError(data.error || 'Something went wrong');
            return;
        }
        currentUser = data.username;
        onLoginSuccess();
        closeModal();
    } catch (e) {
        showModalError('Network error');
    }
}

function onLoginSuccess() {
    document.getElementById('authBar').style.display = 'none';
    document.getElementById('userBar').style.display = 'flex';
    document.getElementById('displayUsername').textContent = '@' + currentUser;
    document.getElementById('urlTableSection').classList.add('show');
    loadUserUrls();
}

async function logout() {
    await fetch('/api/auth/logout', {method: 'POST', credentials: 'same-origin', headers: secureHeaders()});
    currentUser = null;
    document.getElementById('authBar').style.display = 'flex';
    document.getElementById('userBar').style.display = 'none';
    document.getElementById('urlTableSection').classList.remove('show');
    document.getElementById('urlTableBody').innerHTML = '';
}

// === HTML Escaping ===
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// === URL Table ===
async function loadUserUrls() {
    try {
        const res = await fetch('/api/urls', {credentials: 'same-origin'});
        if (!res.ok) return;
        renderUrlTable(await res.json());
    } catch (_) {}
}

function renderUrlTable(urls) {
    const tbody = document.getElementById('urlTableBody');
    const empty = document.getElementById('emptyState');
    tbody.innerHTML = '';
    if (urls.length === 0) {
        empty.style.display = 'block';
        return;
    }
    empty.style.display = 'none';
    urls.forEach(u => {
        const tr = document.createElement('tr');
        tr.id = 'row-' + u.id;
        const created = u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-';
        tr.innerHTML = `
            <td class="url-cell"><a href="${escapeHtml(u.shortUrl)}" target="_blank">${escapeHtml(u.shortUrl)}</a></td>
            <td class="url-cell" id="long-${u.id}"><a href="${escapeHtml(u.longUrl)}" target="_blank" title="${escapeHtml(u.longUrl)}">${escapeHtml(u.longUrl)}</a></td>
            <td>${escapeHtml(created)}</td>
            <td><div class="actions-cell">
                <button class="btn btn-outline btn-sm" data-edit-id="${u.id}">Edit</button>
                <button class="btn btn-danger btn-sm" data-delete-id="${u.id}">Delete</button>
            </div></td>`;
        tr.querySelector('[data-edit-id]').addEventListener('click', () => startEdit(u.id, u.longUrl));
        tr.querySelector('[data-delete-id]').addEventListener('click', () => deleteUrl(u.id));
        tbody.appendChild(tr);
    });
}

function startEdit(id, currentLongUrl) {
    const cell = document.getElementById('long-' + id);
    cell.innerHTML = '';
    const wrapper = document.createElement('div');
    wrapper.style.cssText = 'display:flex;gap:6px;align-items:center;';

    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'edit-input';
    input.id = 'edit-input-' + id;
    input.value = currentLongUrl;

    const saveBtn = document.createElement('button');
    saveBtn.className = 'btn btn-primary btn-sm';
    saveBtn.textContent = 'Save';
    saveBtn.addEventListener('click', () => saveEdit(id));

    const cancelBtn = document.createElement('button');
    cancelBtn.className = 'btn btn-secondary btn-sm';
    cancelBtn.textContent = '\u2715';
    cancelBtn.addEventListener('click', () => loadUserUrls());

    wrapper.append(input, saveBtn, cancelBtn);
    cell.appendChild(wrapper);
    input.focus();
}

async function saveEdit(id) {
    const newUrl = document.getElementById('edit-input-' + id).value.trim();
    if (!newUrl) return;
    try {
        const res = await fetch('/api/urls/' + id, {
            method: 'PUT', credentials: 'same-origin',
            headers: secureHeaders(),
            body: JSON.stringify({longUrl: newUrl})
        });
        if (res.ok) loadUserUrls(); else alert('Failed to update');
    } catch (_) {
        alert('Network error');
    }
}

async function deleteUrl(id) {
    if (!confirm('Delete this URL?')) return;
    try {
        const res = await fetch('/api/urls/' + id, {
            method: 'DELETE',
            credentials: 'same-origin',
            headers: secureHeaders()
        });
        if (res.ok) loadUserUrls(); else alert('Failed to delete');
    } catch (_) {
        alert('Network error');
    }
}

// === Shorten URL ===
const urlInput = document.getElementById('urlInput');
const shortenBtn = document.getElementById('shortenBtn');
const resultDiv = document.getElementById('result');
const shortUrlLink = document.getElementById('shortUrl');
const errorDiv = document.getElementById('error');

urlInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') shortenUrl();
});

async function shortenUrl() {
    const url = urlInput.value.trim();
    if (!url) {
        showError('Please enter a URL');
        return;
    }
    shortenBtn.disabled = true;
    shortenBtn.textContent = 'Shortening...';
    hideError();
    resultDiv.classList.remove('show');
    try {
        const response = await fetch('/generate', {
            method: 'POST', credentials: 'same-origin',
            headers: secureHeaders(),
            body: JSON.stringify({url: url})
        });
        if (!response.ok) {
            throw new Error(await response.text() || 'Failed to shorten URL');
        }
        const shortUrl = await response.text();
        shortUrlLink.href = shortUrl;
        shortUrlLink.textContent = shortUrl;
        resultDiv.classList.add('show');
        if (currentUser) loadUserUrls();
    } catch (err) {
        showError(err.message);
    } finally {
        shortenBtn.disabled = false;
        shortenBtn.innerHTML = 'Shorten \u2192';
    }
}

function copyUrl() {
    navigator.clipboard.writeText(shortUrlLink.href).then(() => {
        const btn = document.querySelector('.copy-btn');
        btn.textContent = 'Copied!';
        setTimeout(() => btn.textContent = 'Copy', 2000);
    });
}

function showError(msg) {
    errorDiv.textContent = msg;
    errorDiv.classList.add('show');
}

function hideError() {
    errorDiv.classList.remove('show');
}

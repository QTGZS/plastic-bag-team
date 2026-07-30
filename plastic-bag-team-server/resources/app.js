// 塑料袋子Team 管理后台前端逻辑
const API = ""; // same-origin
let adminToken = localStorage.getItem("yhteam_admin_token") || "";

function show(id) { document.getElementById(id).classList.remove("hidden"); }
function hide(id) { document.getElementById(id).classList.add("hidden"); }
function msg(el, text, ok) {
  const e = document.getElementById(el);
  e.textContent = text;
  e.className = "msg " + (ok ? "ok" : "err");
}

async function api(path, opts = {}, isForm = false) {
  opts.headers = opts.headers || {};
  if (adminToken) opts.headers["X-Admin-Token"] = adminToken;
  if (!isForm && opts.body) opts.headers["Content-Type"] = "application/json";
  const res = await fetch(API + path, opts);
  let data = {};
  try { data = await res.json(); } catch (e) {}
  return { status: res.status, data };
}

// ── 登录 ──
document.getElementById("login-btn").onclick = async () => {
  const pw = document.getElementById("admin-password").value;
  const r = await api("/api/v1/admin/login", {
    method: "POST",
    body: JSON.stringify({ password: pw })
  });
  if (r.data.success) {
    adminToken = r.data.adminToken;
    localStorage.setItem("yhteam_admin_token", adminToken);
    enterDash();
  } else {
    msg("login-msg", "❌ " + (r.data.message || "登录失败"), false);
  }
};

function enterDash() {
  hide("login-view"); show("dash-view");
  document.getElementById("admin-info").textContent = "已登录";
  loadProducts();
  loadAccounts();
}

document.getElementById("logout-btn").onclick = () => {
  adminToken = "";
  localStorage.removeItem("yhteam_admin_token");
  hide("dash-view"); show("login-view");
  document.getElementById("admin-info").textContent = "";
};

// ── 产品管理 ──

async function loadProducts() {
  const r = await api("/api/v1/admin/products");
  const sel = document.getElementById("add-client");
  sel.innerHTML = "";
  if (r.data.success && r.data.products.length > 0) {
    r.data.products.forEach(p => {
      const opt = document.createElement("option");
      opt.value = p.id;
      opt.textContent = p.name ? `${p.name} (${p.id})` : p.id;
      sel.appendChild(opt);
    });
  } else {
    const opt = document.createElement("option");
    opt.value = "alienv4";
    opt.textContent = "AlienV4";
    sel.appendChild(opt);
  }
  renderProducts(r.data.products || []);
}

function renderProducts(list) {
  const tbody = document.querySelector("#products-table tbody");
  tbody.innerHTML = "";
  list.forEach(p => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${p.id}</td>
      <td>${p.name || ""}</td>
      <td>${p.description || ""}</td>
      <td class="row-actions"><button onclick="delProd('${p.id}')">删除</button></td>`;
    tbody.appendChild(tr);
  });
}

document.getElementById("add-prod-btn").onclick = async () => {
  const id = document.getElementById("prod-id").value.trim();
  const name = document.getElementById("prod-name").value.trim();
  const desc = document.getElementById("prod-desc").value.trim();
  if (!id) { msg("prod-msg", "产品ID不能为空", false); return; }
  const r = await api("/api/v1/admin/product", {
    method: "POST",
    body: JSON.stringify({ id, name, description: desc })
  });
  if (r.data.success) {
    msg("prod-msg", "✅ " + r.data.message, true);
    document.getElementById("prod-id").value = "";
    document.getElementById("prod-name").value = "";
    document.getElementById("prod-desc").value = "";
    loadProducts();
  } else {
    msg("prod-msg", "❌ " + (r.data.message || "添加失败"), false);
  }
};

window.delProd = async (id) => {
  if (!confirm("确认删除产品 " + id + "？已授权该产品的账号将不受影响，但新账号无法再选它。")) return;
  await api("/api/v1/admin/product/delete", {
    method: "POST", body: JSON.stringify({ id })
  });
  loadProducts();
};

// ── 添加账号 ──
document.getElementById("add-btn").onclick = async () => {
  const username = document.getElementById("add-username").value.trim();
  const password = document.getElementById("add-password").value;
  const clientType = document.getElementById("add-client").value;
  const days = parseInt(document.getElementById("add-days").value || "30", 10);
  if (!username || !password) {
    msg("add-msg", "请填写用户名和密码", false);
    return;
  }
  const r = await api("/api/v1/admin/account", {
    method: "POST",
    body: JSON.stringify({ username, password, clientType, durationDays: days })
  });
  if (r.data.success) {
    msg("add-msg", "✅ " + r.data.message, true);
    document.getElementById("add-username").value = "";
    document.getElementById("add-password").value = "";
    loadAccounts();
  } else {
    msg("add-msg", "❌ " + (r.data.message || "添加失败"), false);
  }
};

// ── 账号列表 ──
document.getElementById("refresh-btn").onclick = () => { loadProducts(); loadAccounts(); };

async function loadAccounts() {
  const r = await api("/api/v1/admin/accounts");
  const tbody = document.querySelector("#accounts-table tbody");
  tbody.innerHTML = "";
  if (!r.data.success) {
    if (r.status === 401) { document.getElementById("logout-btn").onclick(); }
    return;
  }
  (r.data.accounts || []).forEach(a => {
    const tr = document.createElement("tr");
    const expired = a.expired;
    const status = expired ? '<span class="badge bad">已过期</span>'
                           : '<span class="badge ok">有效</span>';
    const machine = a.machineCode ? a.machineCode : "未绑定";
    tr.innerHTML = `
      <td>${a.username}</td>
      <td>${a.clientType}</td>
      <td>${machine}</td>
      <td>${fmtTime(a.expireAt)}</td>
      <td>${status}</td>
      <td class="row-actions">
        <button onclick="renew('${a.username}')">续期</button>
        <button onclick="resetMachine('${a.username}')">解绑</button>
        <button onclick="del('${a.username}')">删除</button>
      </td>`;
    tbody.appendChild(tr);
  });
}

window.renew = async (u) => {
  const days = prompt("续期天数：", "30");
  if (!days) return;
  const r = await api("/api/v1/admin/account/renew", {
    method: "POST",
    body: JSON.stringify({ username: u, durationDays: parseInt(days, 10) })
  });
  alert(r.data.message || "");
  loadAccounts();
};
window.resetMachine = async (u) => {
  if (!confirm("确认解绑 " + u + " 的机器码？")) return;
  await api("/api/v1/admin/account/resetmachine", {
    method: "POST", body: JSON.stringify({ username: u })
  });
  loadAccounts();
};
window.del = async (u) => {
  if (!confirm("确认删除账号 " + u + "？")) return;
  await api("/api/v1/admin/account/delete", {
    method: "POST", body: JSON.stringify({ username: u })
  });
  loadAccounts();
};

function fmtTime(ts) {
  if (!ts) return "-";
  const d = new Date(ts);
  return d.toLocaleString("zh-CN");
}

// ── 启动 ──
if (adminToken) {
  api("/api/v1/admin/accounts").then(r => {
    if (r.data.success) enterDash();
  });
}

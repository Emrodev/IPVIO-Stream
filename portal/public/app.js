let devices = [];
let selectedDeviceId = null;
let isBroadcast = false;
let currentPlaylistType = 'xtream';

function getAdminToken() {
  return localStorage.getItem('ipvio_admin_token') || '';
}

function authHeaders() {
  return {
    'Content-Type': 'application/json',
    'x-admin-token': getAdminToken()
  };
}

// 1. Autenticação e Login
async function handleLogin(e) {
  e.preventDefault();
  const username = document.getElementById('login-user').value.trim();
  const password = document.getElementById('login-pass').value.trim();
  const btn = document.getElementById('login-btn');
  const errBox = document.getElementById('login-error');

  errBox.classList.add('hidden');
  btn.disabled = true;
  btn.innerText = 'Verificando...';

  try {
    const res = await fetch('/api/admin/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.error || 'Usuário ou senha incorretos.');
    }

    localStorage.setItem('ipvio_admin_token', data.token);
    localStorage.setItem('ipvio_admin_username', data.username);

    showApp();
    loadDevices();
    loadOtaUpdates();
  } catch (err) {
    errBox.innerText = err.message;
    errBox.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<span>Acessar Painel</span><svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>`;
  }
}

function handleLogout() {
  localStorage.removeItem('ipvio_admin_token');
  localStorage.removeItem('ipvio_admin_username');
  showLogin();
}

function showLogin() {
  document.getElementById('login-screen').classList.remove('hidden');
  document.getElementById('app-container').classList.add('hidden');
}

function showApp() {
  document.getElementById('login-screen').classList.add('hidden');
  document.getElementById('app-container').classList.remove('hidden');
  const user = localStorage.getItem('ipvio_admin_username') || 'admin';
  document.getElementById('user-display').innerText = user;
}

// Alternar abas
function switchTab(tab) {
  const devSec = document.getElementById('section-devices');
  const autSec = document.getElementById('section-automations');
  const otaSec = document.getElementById('section-ota');
  const devBtn = document.getElementById('tab-devices-btn');
  const autBtn = document.getElementById('tab-automations-btn');
  const otaBtn = document.getElementById('tab-ota-btn');

  devSec.classList.add('hidden');
  autSec.classList.add('hidden');
  otaSec.classList.add('hidden');

  devBtn.className = 'px-4 py-1.5 text-sm font-medium rounded-lg transition-all text-gray-400 hover:text-white';
  autBtn.className = 'px-4 py-1.5 text-sm font-medium rounded-lg transition-all text-gray-400 hover:text-white';
  otaBtn.className = 'px-4 py-1.5 text-sm font-medium rounded-lg transition-all text-gray-400 hover:text-white';

  if (tab === 'devices') {
    devSec.classList.remove('hidden');
    devBtn.className = 'px-4 py-1.5 text-sm font-medium rounded-lg transition-all bg-brand-600 text-white shadow';
    loadDevices();
  } else if (tab === 'automations') {
    autSec.classList.remove('hidden');
    autBtn.className = 'px-4 py-1.5 text-sm font-medium rounded-lg transition-all bg-brand-600 text-white shadow';
    loadSettings();
  } else {
    otaSec.classList.remove('hidden');
    otaBtn.className = 'px-4 py-1.5 text-sm font-medium rounded-lg transition-all bg-brand-600 text-white shadow';
    loadOtaUpdates();
  }
}

// Formatar tempo relativo
function formatTimeAgo(ts) {
  if (!ts) return 'Nunca';
  const diffSec = Math.floor((Date.now() - Number(ts)) / 1000);
  if (diffSec < 60) return 'Agora mesmo';
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)} min atrás`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} h atrás`;
  return `${Math.floor(diffSec / 86400)} d atrás`;
}

// Carregar lista de dispositivos
async function loadDevices() {
  try {
    const res = await fetch('/api/admin/devices', { headers: authHeaders() });
    if (res.status === 401) {
      showLogin();
      return;
    }
    const data = await res.json();
    devices = data.devices || [];
    renderDevices(devices);
    updateStats();
  } catch (err) {
    console.error('Erro ao carregar dispositivos:', err);
  }
}

function updateStats() {
  const total = devices.length;
  const online = devices.filter(d => d.isOnline).length;
  const withPlaylist = devices.filter(d => d.playlist_type && d.playlist_type !== 'none').length;

  document.getElementById('stat-total').innerText = total;
  document.getElementById('stat-online').innerText = online;
  document.getElementById('stat-playlists').innerText = withPlaylist;
}

function getPairingCode(d) {
  if (d.pairing_code) return d.pairing_code;
  const src = (d.mac || '').replace(/[^a-zA-Z0-9]/g, '') || (d.id || '').replace(/[^a-zA-Z0-9]/g, '');
  const hex = src.slice(-6).toUpperCase();
  return hex.length === 6 ? `${hex.slice(0, 3)}-${hex.slice(3)}` : (hex || '-');
}

// Renderizar tabela de dispositivos
function renderDevices(list) {
  const tbody = document.getElementById('devices-tbody');
  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" class="text-center py-12 text-gray-500">Nenhum dispositivo registrado ainda.<br><span class="text-xs text-gray-600">Abra o app IPVIO no Android conectado a este portal.</span></td></tr>`;
    return;
  }

  tbody.innerHTML = list.map(d => {
    const isOnline = d.isOnline;
    const isBlocked = d.status === 'blocked';
    const pairingCode = getPairingCode(d);

    let statusBadge = '';
    if (isBlocked) {
      statusBadge = `<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-red-500/10 text-red-400 border border-red-500/20"><span class="w-2 h-2 rounded-full bg-red-500"></span> Bloqueado</span>`;
    } else if (isOnline) {
      statusBadge = `<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"><span class="w-2 h-2 rounded-full bg-emerald-500 badge-pulse"></span> Online</span>`;
    } else {
      statusBadge = `<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-gray-500/10 text-gray-400 border border-gray-500/20"><span class="w-2 h-2 rounded-full bg-gray-500"></span> Offline</span>`;
    }

    let plBadge = '<span class="px-2 py-0.5 rounded bg-amber-500/15 text-amber-300 text-xs font-semibold border border-amber-500/20">Sem Lista</span>';
    if (d.playlist_type === 'xtream') {
      plBadge = `<span class="px-2 py-0.5 rounded bg-blue-500/20 text-blue-400 text-xs font-mono font-bold" title="${d.xtream_host || ''}">Xtream</span>`;
    } else if (d.playlist_type === 'm3u') {
      plBadge = `<span class="px-2 py-0.5 rounded bg-purple-500/20 text-purple-400 text-xs font-mono font-bold" title="${d.playlist_url || ''}">M3U</span>`;
    }

    // Assistindo Agora (Now Playing)
    let nowPlayingBadge = '<span class="text-xs text-gray-500 font-mono">-</span>';
    if (d.isPlaying && d.currentPlaying) {
      let typeIcon = '🔴';
      if (d.currentPlayingType === 'movie') typeIcon = '🎬';
      else if (d.currentPlayingType === 'series') typeIcon = '🍿';
      else if (d.currentPlayingType === 'live') typeIcon = '🔴';

      nowPlayingBadge = `
        <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-emerald-950/60 border border-emerald-500/40 text-emerald-300 text-xs font-medium max-w-[200px]" title="${escapeHtml(d.currentPlaying)}">
          <span>${typeIcon}</span>
          <span class="truncate font-semibold">${escapeHtml(d.currentPlaying)}</span>
        </div>
      `;
    } else if (d.now_playing && !d.isPlaying) {
      nowPlayingBadge = `<span class="text-[11px] text-gray-500 truncate max-w-[150px] inline-block" title="Último reproduzido: ${escapeHtml(d.now_playing)}">Pausado (${escapeHtml(d.now_playing.slice(0, 16))}...)</span>`;
    }

    // Badge de Validade
    let expBadge = '<span class="text-gray-500 text-xs">-</span>';
    if (d.playlist_type && d.playlist_type !== 'none') {
      const expStr = d.playlist_exp_date || 'Não consultada';
      if (expStr.includes('/')) {
        // Formato DD/MM/AAAA
        const parts = expStr.split('/');
        const expDate = new Date(parts[2], parts[1] - 1, parts[0]);
        const diffDays = Math.ceil((expDate - new Date()) / (1000 * 60 * 60 * 24));

        if (diffDays <= 0) {
          expBadge = `<span class="px-2 py-0.5 rounded bg-red-500/20 text-red-400 text-xs font-bold font-mono" title="Vencido">🔴 ${expStr} (Vencida)</span>`;
        } else if (diffDays <= 5) {
          expBadge = `<span class="px-2 py-0.5 rounded bg-yellow-500/20 text-yellow-300 text-xs font-bold font-mono" title="Vence em breve">🟡 ${expStr} (${diffDays}d)</span>`;
        } else {
          expBadge = `<span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-xs font-bold font-mono" title="Válida">🟢 ${expStr} (${diffDays}d)</span>`;
        }
      } else if (expStr.toLowerCase().includes('ilimitado')) {
        expBadge = `<span class="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-xs font-bold font-mono">🟢 Ilimitada</span>`;
      } else {
        expBadge = `<span class="text-xs text-gray-400 font-mono">${escapeHtml(expStr)}</span>`;
      }

      expBadge += `
        <button onclick="refreshValidity('${d.id}')" class="ml-1 text-gray-500 hover:text-cyan-400 inline" title="Verificar validade no provedor agora">
          <svg class="w-3.5 h-3.5 inline" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
        </button>
      `;
    }

    let phoneHtml = '';
    if (d.client_phone) {
      const cleanPhone = String(d.client_phone).replace(/\D/g, '');
      phoneHtml = `
        <div class="mt-0.5">
          <a href="https://wa.me/${cleanPhone}" target="_blank" class="inline-flex items-center gap-1 text-[11px] text-emerald-400 hover:text-emerald-300 font-mono" title="Abrir conversa no WhatsApp">
            <span>💬</span> ${escapeHtml(d.client_phone)}
          </a>
        </div>
      `;
    }

    return `
      <tr class="hover:bg-surface-800/40 transition">
        <td class="py-4 px-5">${statusBadge}</td>
        <td class="py-4 px-5">
          <div class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-cyan-950/70 border border-cyan-500/40 text-cyan-300 font-mono font-black text-sm tracking-wider shadow-sm">
            <span>${escapeHtml(pairingCode)}</span>
            <button onclick="navigator.clipboard.writeText('${pairingCode}'); alert('Código ${pairingCode} copiado!')" class="text-cyan-500 hover:text-cyan-200 ml-1" title="Copiar Código">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>
            </button>
          </div>
        </td>
        <td class="py-4 px-5">
          <div class="font-bold text-white flex items-center gap-2">
            ${escapeHtml(d.name || 'Sem nome')}
            <button onclick="openEditDeviceModal('${d.id}')" class="text-gray-500 hover:text-brand-400" title="Editar Nome e Telefone WhatsApp">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" /></svg>
            </button>
          </div>
          ${phoneHtml}
          <div class="text-xs text-gray-400">${escapeHtml(d.model || 'Android TV')}</div>
        </td>
        <td class="py-4 px-5">
          <div class="font-mono text-xs text-gray-200 flex items-center gap-1.5">
            ${escapeHtml(d.mac || d.id)}
            <button onclick="navigator.clipboard.writeText('${d.mac || d.id}'); alert('Copiado!')" class="text-gray-500 hover:text-white" title="Copiar">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" /></svg>
            </button>
          </div>
          <div class="text-[10px] text-gray-500 font-mono">${escapeHtml(d.id.slice(0, 16))}...</div>
        </td>
        <td class="py-4 px-5">
          <div class="text-xs text-gray-300 font-mono">${escapeHtml(d.ip || '0.0.0.0')}</div>
          <div class="text-[11px] text-brand-400 font-medium">v${escapeHtml(d.app_version || '1.0')}</div>
        </td>
        <td class="py-4 px-5">${plBadge}</td>
        <td class="py-4 px-5">${nowPlayingBadge}</td>
        <td class="py-4 px-5 whitespace-nowrap">${expBadge}</td>
        <td class="py-4 px-5 text-xs text-gray-400">${formatTimeAgo(d.last_seen)}</td>
        <td class="py-4 px-5 text-right space-x-1 whitespace-nowrap">
          <button onclick="openLinkCodeModal('${escapeHtml(pairingCode)}')" class="px-2.5 py-1 text-xs font-semibold rounded-lg bg-cyan-700/60 hover:bg-cyan-600 text-cyan-100 hover:text-white transition" title="Vincular Lista por Código">
            ⚡ Vincular
          </button>
          <button onclick="openPlaylistModal('${d.id}')" class="px-2.5 py-1 text-xs font-semibold rounded-lg bg-surface-700 hover:bg-brand-600 text-gray-200 hover:text-white transition" title="Configurar Playlist">
            Playlist
          </button>
          <button onclick="openMessageModal('${d.id}')" class="px-2.5 py-1 text-xs font-semibold rounded-lg bg-surface-700 hover:bg-yellow-600 text-gray-200 hover:text-white transition" title="Enviar Mensagem">
            Mensagem
          </button>
          <button onclick="toggleBlock('${d.id}', '${d.status}')" class="px-2.5 py-1 text-xs font-semibold rounded-lg ${isBlocked ? 'bg-emerald-600 hover:bg-emerald-500 text-white' : 'bg-red-900/40 hover:bg-red-800 text-red-300'} transition" title="${isBlocked ? 'Desbloquear' : 'Bloquear'}">
            ${isBlocked ? 'Desbloquear' : 'Bloquear'}
          </button>
          <button onclick="deleteDevice('${d.id}')" class="p-1 text-xs text-gray-500 hover:text-red-400 transition" title="Excluir">
            <svg class="w-4 h-4 inline" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

// Filtro em tempo real
function filterDevices() {
  const q = document.getElementById('search-input').value.toLowerCase();
  const st = document.getElementById('status-filter').value;

  const filtered = devices.filter(d => {
    const matchQuery = !q || (d.name && d.name.toLowerCase().includes(q)) ||
      (d.mac && d.mac.toLowerCase().includes(q)) ||
      (d.id && d.id.toLowerCase().includes(q)) ||
      (d.ip && d.ip.includes(q)) ||
      (d.model && d.model.toLowerCase().includes(q));

    let matchStatus = true;
    if (st === 'online') matchStatus = d.isOnline;
    else if (st === 'offline') matchStatus = !d.isOnline && d.status !== 'blocked';
    else if (st === 'blocked') matchStatus = d.status === 'blocked';

    return matchQuery && matchStatus;
  });

  renderDevices(filtered);
}

// Reconsultar validade da lista no servidor
async function refreshValidity(id) {
  try {
    const res = await fetch(`/api/admin/devices/${id}/check-validity`, {
      method: 'POST',
      headers: authHeaders()
    });
    const data = await res.json();
    if (res.ok && data.success) {
      alert(`Validade atualizada com sucesso!\n\nStatus: ${data.status}\nData de Expiração: ${data.expDate}\nDias Restantes: ${data.daysRemaining !== null ? data.daysRemaining + ' dias' : 'Ilimitado'}`);
      loadDevices();
    } else {
      alert(`Não foi possível consultar a validade: ${data.error || 'Erro desconhecido'}`);
    }
  } catch (err) {
    alert('Erro ao consultar validade: ' + err.message);
  }
}

// Modal de Edição de Cliente (Nome e WhatsApp)
let editingDeviceId = null;
function openEditDeviceModal(id) {
  editingDeviceId = id;
  const dev = devices.find(d => d.id === id);
  document.getElementById('edit-device-subtitle').innerText = `Aparelho: ${dev?.model || id} (${dev?.mac || ''})`;
  document.getElementById('edit-device-name').value = dev?.name || '';
  document.getElementById('edit-device-phone').value = dev?.client_phone || '';
  document.getElementById('edit-device-modal').classList.remove('hidden');
}

async function saveDeviceEdit() {
  if (!editingDeviceId) return;
  const name = document.getElementById('edit-device-name').value.trim();
  const phone = document.getElementById('edit-device-phone').value.trim();

  await fetch(`/api/admin/devices/${editingDeviceId}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify({ name, clientPhone: phone })
  });

  closeModal('edit-device-modal');
  loadDevices();
}

// Renomear Dispositivo (compatibilidade)
async function renameDevice(id, currentName) {
  openEditDeviceModal(id);
}

// Bloquear / Desbloquear Dispositivo
async function toggleBlock(id, currentStatus) {
  const next = currentStatus === 'blocked' ? 'active' : 'blocked';
  if (!confirm(`Deseja realmente ${next === 'blocked' ? 'BLOQUEAR' : 'DESBLOQUEAR'} este aparelho?`)) return;

  await fetch(`/api/admin/devices/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify({ status: next })
  });
  loadDevices();
}

// Excluir Dispositivo
async function deleteDevice(id) {
  if (!confirm('Excluir este dispositivo do portal? Ele será reinserido se conectar novamente.')) return;
  await fetch(`/api/admin/devices/${id}`, {
    method: 'DELETE',
    headers: authHeaders()
  });
  loadDevices();
}

// Modal de Playlist
function openPlaylistModal(id) {
  selectedDeviceId = id;
  const dev = devices.find(d => d.id === id);
  document.getElementById('modal-device-subtitle').innerText = `Dispositivo: ${dev?.name || id} (${dev?.mac || ''})`;

  currentPlaylistType = dev?.playlist_type || 'xtream';
  setPlaylistType(currentPlaylistType);

  document.getElementById('xtream-host').value = dev?.xtream_host || '';
  document.getElementById('xtream-user').value = dev?.xtream_user || '';
  document.getElementById('xtream-pass').value = dev?.xtream_pass || '';
  document.getElementById('m3u-url').value = dev?.playlist_url || '';

  document.getElementById('playlist-modal').classList.remove('hidden');
}

function setPlaylistType(type) {
  currentPlaylistType = type;
  const btnX = document.getElementById('btn-type-xtream');
  const btnM = document.getElementById('btn-type-m3u');
  const btnN = document.getElementById('btn-type-none');
  const fX = document.getElementById('fields-xtream');
  const fM = document.getElementById('fields-m3u');

  btnX.className = type === 'xtream' ? 'flex-1 py-1.5 rounded-lg bg-brand-600 text-white' : 'flex-1 py-1.5 rounded-lg text-gray-400 hover:text-white';
  btnM.className = type === 'm3u' ? 'flex-1 py-1.5 rounded-lg bg-brand-600 text-white' : 'flex-1 py-1.5 rounded-lg text-gray-400 hover:text-white';
  btnN.className = type === 'none' ? 'flex-1 py-1.5 rounded-lg bg-brand-600 text-white' : 'flex-1 py-1.5 rounded-lg text-gray-400 hover:text-white';

  fX.classList.toggle('hidden', type !== 'xtream');
  fM.classList.toggle('hidden', type !== 'm3u');
}

async function savePlaylist() {
  if (!selectedDeviceId) return;

  const payload = {
    type: currentPlaylistType,
    url: document.getElementById('m3u-url').value.trim(),
    host: document.getElementById('xtream-host').value.trim(),
    user: document.getElementById('xtream-user').value.trim(),
    pass: document.getElementById('xtream-pass').value.trim(),
  };

  const res = await fetch(`/api/admin/devices/${selectedDeviceId}/playlist`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(payload)
  });

  if (res.ok) {
    const data = await res.json();
    let msg = 'Playlist enviada para o dispositivo com sucesso!';
    if (data.validity?.expDate) {
      msg += `\n\nValidade detectada no provedor: ${data.validity.expDate} (Status: ${data.validity.status})`;
    }
    alert(msg);
    closeModal('playlist-modal');
    loadDevices();
  } else {
    alert('Erro ao salvar playlist.');
  }
}

// Modal de Mensagem Push
function openMessageModal(id) {
  selectedDeviceId = id;
  isBroadcast = false;
  const dev = devices.find(d => d.id === id);
  document.getElementById('modal-message-subtitle').innerText = `Destinatário: ${dev?.name || id} (${dev?.mac || ''})`;
  document.getElementById('msg-text').value = '';
  document.getElementById('message-modal').classList.remove('hidden');
}

function openBroadcastModal() {
  selectedDeviceId = null;
  isBroadcast = true;
  document.getElementById('modal-message-subtitle').innerText = 'Destinatário: TODOS os dispositivos ativos';
  document.getElementById('msg-text').value = '';
  document.getElementById('message-modal').classList.remove('hidden');
}

async function sendMessage() {
  const text = document.getElementById('msg-text').value.trim();
  const type = document.getElementById('msg-type').value;
  if (!text) return alert('Digite a mensagem.');

  const endpoint = isBroadcast ? '/api/admin/devices/broadcast-message' : `/api/admin/devices/${selectedDeviceId}/message`;

  const res = await fetch(endpoint, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ message: text, type })
  });

  if (res.ok) {
    alert('Mensagem enviada! Aparecerá na tela do aparelho.');
    closeModal('message-modal');
  } else {
    alert('Erro ao enviar mensagem.');
  }
}

function closeModal(id) {
  document.getElementById(id).classList.add('hidden');
}

// Modal de Vincular Aparelho por Código
function openLinkCodeModal(prefillCode = '') {
  document.getElementById('link-code-input').value = prefillCode || '';
  document.getElementById('link-xtream-host').value = '';
  document.getElementById('link-xtream-user').value = '';
  document.getElementById('link-xtream-pass').value = '';
  document.getElementById('link-modal').classList.remove('hidden');
  if (!prefillCode) {
    document.getElementById('link-code-input').focus();
  } else {
    document.getElementById('link-xtream-host').focus();
  }
}

async function submitLinkByCode() {
  const code = document.getElementById('link-code-input').value.trim();
  const host = document.getElementById('link-xtream-host').value.trim();
  const user = document.getElementById('link-xtream-user').value.trim();
  const pass = document.getElementById('link-xtream-pass').value.trim();
  const phone = document.getElementById('link-client-phone')?.value.trim() || '';

  if (!code) return alert('Por favor, informe o Código de Ativação do aparelho.');
  if (!host || !user || !pass) return alert('Por favor, preencha o Servidor, Usuário e Senha.');

  const btn = document.getElementById('btn-submit-link');
  btn.disabled = true;
  btn.innerText = 'Vinculando e Verificando...';

  try {
    const res = await fetch('/api/admin/devices/link-by-code', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({
        code,
        type: 'xtream',
        host,
        user,
        pass,
        phone
      })
    });

    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.error || 'Não foi possível vincular o aparelho.');
    }

    let msg = `Aparelho "${data.deviceName || code}" vinculado com sucesso!\nAs credenciais foram enviadas para o aparelho.`;
    if (data.validity?.expDate) {
      msg += `\n\nValidade detectada no provedor: ${data.validity.expDate} (Status: ${data.validity.status})`;
    }
    alert(msg);
    closeModal('link-modal');
    loadDevices();
  } catch (err) {
    alert('Erro: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.innerText = 'Vincular & Enviar para o Aparelho';
  }
}

// OTA: Carregar e Submeter
async function loadOtaUpdates() {
  try {
    const res = await fetch('/api/admin/ota', { headers: authHeaders() });
    if (res.status === 401) {
      showLogin();
      return;
    }
    const data = await res.json();
    const list = data.updates || [];

    if (list.length > 0) {
      document.getElementById('stat-ota-version').innerText = `${list[0].version_name} (${list[0].version_code})`;
    } else {
      document.getElementById('stat-ota-version').innerText = '-';
    }

    const tbody = document.getElementById('ota-history-tbody');
    if (list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" class="py-8 text-center text-gray-500">Nenhuma versão publicada ainda.</td></tr>';
      return;
    }

    tbody.innerHTML = list.map(u => `
      <tr class="hover:bg-surface-800/40">
        <td class="py-3 px-3 font-bold text-white">${escapeHtml(u.version_name)}</td>
        <td class="py-3 px-3 font-mono text-xs text-gray-400">${u.version_code}</td>
        <td class="py-3 px-3">${u.mandatory ? '<span class="text-xs px-2 py-0.5 rounded bg-red-500/20 text-red-400 font-bold">Sim</span>' : '<span class="text-xs text-gray-500">Não</span>'}</td>
        <td class="py-3 px-3 text-xs text-gray-400">${new Date(Number(u.released_at)).toLocaleDateString('pt-BR')}</td>
        <td class="py-3 px-3">
          <a href="${escapeHtml(u.download_url)}" target="_blank" class="text-xs text-brand-400 hover:underline font-mono">Download APK</a>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Erro ao carregar OTA:', err);
  }
}

async function submitOta(e) {
  e.preventDefault();
  const btn = document.getElementById('ota-submit-btn');
  btn.disabled = true;
  btn.innerText = 'Enviando APK...';

  try {
    const vName = document.getElementById('ota-version-name').value.trim();
    const vCode = document.getElementById('ota-version-code').value.trim();
    const changelog = document.getElementById('ota-changelog').value.trim();
    const mandatory = document.getElementById('ota-mandatory').checked;
    const apkFile = document.getElementById('ota-apk-file').files[0];
    const directUrl = document.getElementById('ota-download-url').value.trim();

    if (apkFile) {
      const formData = new FormData();
      formData.append('apk', apkFile);
      formData.append('versionName', vName);
      formData.append('versionCode', vCode);
      formData.append('changelog', changelog);
      formData.append('mandatory', mandatory ? '1' : '0');

      const res = await fetch('/api/admin/ota/upload', {
        method: 'POST',
        headers: { 'x-admin-token': getAdminToken() },
        body: formData
      });
      const data = await res.json();
      if (!res.ok || !data.success) throw new Error(data.error || 'Falha no upload do APK.');
      alert(`Atualização OTA publicada com sucesso!\nVersão detectada: v${data.versionName} (Code ${data.versionCode})`);
    } else if (directUrl) {
      if (!vName || !vCode) {
        alert('Ao usar link externo de download, preencha o Nome e o Código da Versão.');
        btn.disabled = false;
        btn.innerText = 'Publicar Versão';
        return;
      }
      const res = await fetch('/api/admin/ota', {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify({
          versionName: vName,
          versionCode: vCode,
          downloadUrl: directUrl,
          changelog,
          mandatory
        })
      });
      const data = await res.json();
      if (!res.ok || !data.success) throw new Error(data.error || 'Falha ao registrar versão.');
      alert('Atualização OTA publicada com sucesso! Os aparelhos clientes já podem baixar essa versão.');
    } else {
      alert('Selecione um arquivo APK ou informe uma URL de download.');
      btn.disabled = false;
      btn.innerText = 'Publicar Versão';
      return;
    }

    document.getElementById('ota-form').reset();
    loadOtaUpdates();
  } catch (err) {
    alert('Erro: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.innerText = 'Publicar Versão';
  }
}

function escapeHtml(str) {
  return String(str || '').replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[m]);
}

// ==============================================================================
// AUTOMAÇÕES, WHATSAPP & CONFIGURAÇÕES IPTV
// ==============================================================================

async function loadSettings() {
  try {
    const res = await fetch('/api/admin/settings', { headers: authHeaders() });
    if (!res.ok) return;
    const data = await res.json();
    const s = data.settings || {};

    if (document.getElementById('iptv-default-url')) {
      document.getElementById('iptv-default-url').value = s.default_iptv_url || '';
    }
    if (document.getElementById('wa-api-url')) {
      document.getElementById('wa-api-url').value = s.whatsapp_api_url || '';
    }
    if (document.getElementById('wa-api-token')) {
      document.getElementById('wa-api-token').value = s.whatsapp_api_token || '';
    }
    if (document.getElementById('auto-expiry-enabled')) {
      document.getElementById('auto-expiry-enabled').checked = Boolean(s.auto_expiry_enabled);
    }
    if (document.getElementById('auto-expiry-days')) {
      document.getElementById('auto-expiry-days').value = s.auto_expiry_days || 3;
    }
    if (document.getElementById('auto-expiry-notify-tv')) {
      document.getElementById('auto-expiry-notify-tv').checked = Boolean(s.auto_expiry_notify_tv);
    }
    if (document.getElementById('auto-expiry-notify-wa')) {
      document.getElementById('auto-expiry-notify-wa').checked = Boolean(s.auto_expiry_notify_wa);
    }
    if (document.getElementById('auto-expiry-message')) {
      document.getElementById('auto-expiry-message').value = s.auto_expiry_message || '';
    }
  } catch (err) {
    console.error('Erro ao carregar configurações:', err);
  }
}

async function saveDefaultIptvUrl() {
  const url = document.getElementById('iptv-default-url').value.trim();
  if (!url) return alert('Por favor, informe a URL do servidor IPTV.');

  try {
    const res = await fetch('/api/admin/settings', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ default_iptv_url: url })
    });
    if (res.ok) alert('URL do IPTV padrão salva com sucesso!');
    else alert('Erro ao salvar URL.');
  } catch (err) {
    alert('Erro: ' + err.message);
  }
}

async function migrateAllIptvUrl() {
  const url = document.getElementById('iptv-default-url').value.trim();
  if (!url) return alert('Por favor, informe a URL do servidor IPTV.');

  const conf = confirm(
    `ATENÇÃO: Deseja alterar o servidor IPTV de TODOS os clientes ativos para:\n\n${url}\n\nTodos os aparelhos conectados receberão essa nova URL no próximo sinal.`
  );
  if (!conf) return;

  try {
    const res = await fetch('/api/admin/iptv/migrate-url', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ newUrl: url })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      alert(`Migração em massa concluída com sucesso!\n${data.affected} dispositivo(s) atualizado(s) para o novo servidor.`);
      loadDevices();
    } else {
      alert('Erro na migração: ' + (data.error || 'Erro desconhecido'));
    }
  } catch (err) {
    alert('Erro ao migrar clientes: ' + err.message);
  }
}

async function saveWhatsAppApiSettings() {
  const url = document.getElementById('wa-api-url').value.trim();
  const token = document.getElementById('wa-api-token').value.trim();

  try {
    const res = await fetch('/api/admin/settings', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({
        whatsapp_api_url: url,
        whatsapp_api_token: token
      })
    });
    if (res.ok) alert('Credenciais da API do WhatsApp salvas com sucesso!');
    else alert('Erro ao salvar credenciais.');
  } catch (err) {
    alert('Erro: ' + err.message);
  }
}

async function testWhatsAppMessage() {
  const phone = document.getElementById('wa-test-phone').value.trim();
  const url = document.getElementById('wa-api-url').value.trim();
  const token = document.getElementById('wa-api-token').value.trim();

  if (!phone) return alert('Digite o número de WhatsApp com DDD para o teste (ex: 5511999999999).');

  const btn = document.getElementById('btn-test-wa');
  btn.disabled = true;
  btn.innerText = 'Enviando...';

  try {
    const res = await fetch('/api/admin/whatsapp/test', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({
        phone,
        apiUrl: url,
        apiToken: token,
        message: '✅ Mensagem de teste oficial enviada com sucesso pelo Portal IPVIO Stream!'
      })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      alert(`Mensagem enviada com sucesso para ${phone}! Verifique o WhatsApp.`);
    } else {
      alert(`Erro no envio da mensagem: ${data.error || 'Verifique a URL e o Token da API'}`);
    }
  } catch (err) {
    alert('Erro: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.innerText = 'Testar';
  }
}

async function saveAutomationRules() {
  const enabled = document.getElementById('auto-expiry-enabled').checked;
  const days = document.getElementById('auto-expiry-days').value;
  const notifyTv = document.getElementById('auto-expiry-notify-tv').checked;
  const notifyWa = document.getElementById('auto-expiry-notify-wa').checked;
  const message = document.getElementById('auto-expiry-message').value;

  try {
    const res = await fetch('/api/admin/settings', {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({
        auto_expiry_enabled: enabled,
        auto_expiry_days: days,
        auto_expiry_notify_tv: notifyTv,
        auto_expiry_notify_wa: notifyWa,
        auto_expiry_message: message
      })
    });
    if (res.ok) alert('Regras de automação salvas com sucesso!');
    else alert('Erro ao salvar regras.');
  } catch (err) {
    alert('Erro: ' + err.message);
  }
}

async function runAutomationsNow() {
  const btn = document.getElementById('btn-run-automations');
  btn.disabled = true;
  btn.innerText = 'Verificando...';

  try {
    const res = await fetch('/api/admin/automations/run-expiry', {
      method: 'POST',
      headers: authHeaders()
    });
    const data = await res.json();
    if (res.ok && data.success) {
      let msg = `Verificação concluída!\n\nDispositivos escaneados: ${data.scanned || 0}`;
      msg += `\nAvisos enviados na TV: ${data.notifiedTv || 0}`;
      msg += `\nMensagens enviadas no WhatsApp: ${data.notifiedWa || 0}`;
      if (data.errors && data.errors.length > 0) {
        msg += `\n\nErros:\n${data.errors.join('\n')}`;
      }
      alert(msg);
      loadDevices();
    } else {
      alert(`Erro: ${data.error || data.reason || 'Falha ao executar automações'}`);
    }
  } catch (err) {
    alert('Erro ao executar: ' + err.message);
  } finally {
    btn.disabled = false;
    btn.innerText = '⚡ Executar Agora';
  }
}

// Inicialização
document.addEventListener('DOMContentLoaded', () => {
  const token = getAdminToken();
  if (token) {
    showApp();
    loadDevices();
    loadOtaUpdates();
    setInterval(loadDevices, 15000);
  } else {
    showLogin();
  }
});

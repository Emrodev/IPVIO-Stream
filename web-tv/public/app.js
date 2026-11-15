// Korp TV — Web & Smart TV Engine (Samsung Tizen, LG webOS, Browsers)

let config = {
  portalUrl: 'https://korp.kabaweb.in',
  localPortalUrl: 'http://localhost:3000'
};

let deviceState = {
  id: '',
  pairingCode: '',
  playlist: null,
  currentTab: 'home',
  activeStream: null,
  nowPlaying: '',
  nowPlayingType: 'live',
  aspectRatioIndex: 0,
  aspectRatios: ['contain', 'cover', 'fill'],
  aspectLabels: ['Ajustar', 'Preencher (Zoom)', 'Esticar (16:9)'],
  liveCategories: [],
  liveChannels: [],
  vodCategories: [],
  vodItems: [],
  favorites: [],
  recent: []
};

let hlsPlayer = null;
let osdTimer = null;
let heartbeatInterval = null;

// ==============================================================================
// 1. INICIALIZAÇÃO E IDENTIDADE DO DISPOSITIVO
// ==============================================================================

async function init() {
  loadConfig();
  loadDeviceCredentials();
  loadLocalStoredData();
  setupDpadNavigation();
  setupEventListeners();

  await registerDevice();
  startHeartbeat();
}

function loadConfig() {
  fetch('/api/config')
    .then(r => r.json())
    .then(data => {
      config.portalUrl = data.portalUrl || config.portalUrl;
      config.localPortalUrl = data.localPortalUrl || config.localPortalUrl;
      document.getElementById('portal-url-display').innerText = config.portalUrl;
    })
    .catch(() => {});
}

function loadDeviceCredentials() {
  let devId = localStorage.getItem('korp_tv_device_id');
  if (!devId) {
    devId = 'webtv_' + Math.random().toString(36).substring(2, 10) + Date.now().toString(36).substring(4);
    localStorage.setItem('korp_tv_device_id', devId);
  }
  deviceState.id = devId;

  let code = localStorage.getItem('korp_tv_pairing_code');
  if (!code) {
    const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
    let c = '';
    for (let i = 0; i < 6; i++) c += chars.charAt(Math.floor(Math.random() * chars.length));
    code = `${c.slice(0, 3)}-${c.slice(3)}`;
    localStorage.setItem('korp_tv_pairing_code', code);
  }
  deviceState.pairingCode = code;

  document.getElementById('pairing-code-display').innerText = code;
  document.getElementById('info-device-id').innerText = devId;
  document.getElementById('info-pairing-code').innerText = code;

  // Gera o QR Code de ativação na tela da TV
  const qrContainer = document.getElementById('pairing-qrcode');
  if (qrContainer && window.QRCode) {
    qrContainer.innerHTML = '';
    new QRCode(qrContainer, {
      text: `${config.portalUrl}`,
      width: 140,
      height: 140,
      colorDark: "#000000",
      colorLight: "#ffffff"
    });
  }
}

function loadLocalStoredData() {
  try {
    deviceState.favorites = JSON.parse(localStorage.getItem('korp_tv_favs') || '[]');
    deviceState.recent = JSON.parse(localStorage.getItem('korp_tv_recent') || '[]');
    const savedPlaylist = localStorage.getItem('korp_tv_manual_playlist');
    if (savedPlaylist) {
      deviceState.playlist = JSON.parse(savedPlaylist);
    }
  } catch (_) {}
}

// ==============================================================================
// 2. COMUNICAÇÃO COM O PORTAL KORP TV (REGISTER & HEARTBEAT)
// ==============================================================================

async function registerDevice() {
  try {
    const payload = {
      deviceId: deviceState.id,
      pairingCode: deviceState.pairingCode,
      model: 'Smart TV Web (Samsung/LG/Browser)',
      appVersion: '1.0.0-web',
      versionCode: 100
    };

    // Tenta primeiro o portal local se estiver na mesma rede, com fallback para o público
    const portalBase = config.localPortalUrl || config.portalUrl;
    const res = await fetch(`${portalBase}/api/device/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).catch(() => fetch(`${config.portalUrl}/api/device/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }));

    if (!res.ok) return;
    const data = await res.json();
    handlePortalResponse(data);
  } catch (err) {
    console.warn('[Portal Register Warning]', err.message);
  }
}

function startHeartbeat() {
  if (heartbeatInterval) clearInterval(heartbeatInterval);
  heartbeatInterval = setInterval(async () => {
    try {
      const portalBase = config.localPortalUrl || config.portalUrl;
      const res = await fetch(`${portalBase}/api/device/heartbeat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          deviceId: deviceState.id,
          nowPlaying: deviceState.nowPlaying,
          nowPlayingType: deviceState.nowPlayingType
        })
      }).catch(() => fetch(`${config.portalUrl}/api/device/heartbeat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          deviceId: deviceState.id,
          nowPlaying: deviceState.nowPlaying,
          nowPlayingType: deviceState.nowPlayingType
        })
      }));

      if (!res.ok) return;
      const data = await res.json();
      handlePortalResponse(data);
    } catch (_) {}
  }, 5000);
}

function handlePortalResponse(data) {
  if (!data || !data.success) return;

  // Recebeu playlist do portal (Xtream ou M3U)
  if (data.playlist && data.playlist.type !== 'none') {
    const p = data.playlist;
    const isNew = !deviceState.playlist || 
                  deviceState.playlist.host !== p.host || 
                  deviceState.playlist.user !== p.user;

    deviceState.playlist = p;
    document.getElementById('info-iptv-host').innerText = p.host || '-';
    document.getElementById('info-iptv-user').innerText = p.user || '-';
    document.getElementById('info-iptv-type').innerText = p.type || '-';

    if (isNew) {
      showMainScreen();
      loadXtreamContent();
    }
  } else if (!deviceState.playlist) {
    showPairingScreen();
  }
}

function showPairingScreen() {
  document.getElementById('screen-pairing').classList.remove('hidden');
  document.getElementById('screen-main').classList.add('hidden');
  document.getElementById('player-container').classList.add('hidden');
  setFocus(document.getElementById('btn-check-pairing'));
}

function showMainScreen() {
  document.getElementById('screen-pairing').classList.add('hidden');
  document.getElementById('screen-main').classList.remove('hidden');
  document.getElementById('player-container').classList.add('hidden');
  switchTab('home');
}

// ==============================================================================
// 3. CARREGAMENTO DE CONTEÚDO XTREAM CODES
// ==============================================================================

async function fetchXtream(action, extraParams = '') {
  if (!deviceState.playlist) return null;
  const { host, user, pass } = deviceState.playlist;
  if (!host || !user || !pass) return null;

  const cleanHost = host.replace(/\/+$/, '');
  const targetUrl = `${cleanHost}/player_api.php?username=${encodeURIComponent(user)}&password=${encodeURIComponent(pass)}&action=${action}${extraParams}`;

  // Tenta direto e, se falhar por CORS, usa o proxy do servidor
  try {
    const res = await fetch(targetUrl);
    if (res.ok) return await res.json();
  } catch (_) {}

  // Fallback transparente via proxy local
  const proxyUrl = `/api/proxy?url=${encodeURIComponent(targetUrl)}`;
  const proxyRes = await fetch(proxyUrl);
  return await proxyRes.json();
}

async function loadXtreamContent() {
  try {
    // 1. Categorias de TV ao Vivo
    const liveCats = await fetchXtream('get_live_categories');
    if (Array.isArray(liveCats)) {
      deviceState.liveCategories = liveCats;
      renderLiveCategories(liveCats);
    }

    // 2. Canais ao Vivo
    const liveStreams = await fetchXtream('get_live_streams');
    if (Array.isArray(liveStreams)) {
      deviceState.liveChannels = liveStreams;
      renderLiveChannels(liveStreams);
      updateHeroBillboard(liveStreams[0]);
    }

    // 3. Filmes (VOD)
    const vodCats = await fetchXtream('get_vod_categories');
    if (Array.isArray(vodCats)) {
      deviceState.vodCategories = vodCats;
      renderVodCategories(vodCats);
    }

    const vodStreams = await fetchXtream('get_vod_streams');
    if (Array.isArray(vodStreams)) {
      deviceState.vodItems = vodStreams;
      renderVodItems(vodStreams);
      renderHomeMoviesRow(vodStreams.slice(0, 15));
    }

    renderHomeFavsRow();
  } catch (err) {
    console.error('[Load Xtream Content Error]', err);
  }
}

// ==============================================================================
// 4. RENDERIZAÇÃO DA INTERFACE (CANVAS DA TV)
// ==============================================================================

function updateHeroBillboard(item) {
  if (!item) return;
  document.getElementById('hero-title').innerText = item.name || 'Canais em Alta Definição';
  const heroCard = document.getElementById('hero-billboard');
  if (item.stream_icon) {
    heroCard.style.backgroundImage = `url('${item.stream_icon}')`;
    heroCard.style.backgroundSize = 'cover';
    heroCard.style.backgroundPosition = 'center';
  }
  document.getElementById('btn-hero-play').onclick = () => playStream(item, 'live');
}

function renderLiveCategories(cats) {
  const container = document.getElementById('live-categories-list');
  document.getElementById('live-cat-count').innerText = cats.length;
  container.innerHTML = `
    <div class="category-item focusable active" data-cat-id="all">
      <span>Todos os Canais</span>
    </div>
  ` + cats.map(c => `
    <div class="category-item focusable" data-cat-id="${c.category_id}">
      <span>${escapeHtml(c.category_name)}</span>
    </div>
  `).join('');

  container.querySelectorAll('.category-item').forEach(el => {
    el.onclick = () => {
      container.querySelectorAll('.category-item').forEach(x => x.classList.remove('active'));
      el.classList.add('active');
      const catId = el.getAttribute('data-cat-id');
      document.getElementById('current-category-title').innerText = el.innerText.trim();
      filterLiveChannelsByCategory(catId);
    };
  });
}

function filterLiveChannelsByCategory(catId) {
  if (catId === 'all') {
    renderLiveChannels(deviceState.liveChannels);
  } else {
    const filtered = deviceState.liveChannels.filter(c => String(c.category_id) === String(catId));
    renderLiveChannels(filtered);
  }
}

function renderLiveChannels(channels) {
  const container = document.getElementById('live-channels-grid');
  document.getElementById('channel-count').innerText = `${channels.length} canais`;
  
  if (channels.length === 0) {
    container.innerHTML = '<p class="text-sm text-gray-500 col-span-full">Nenhum canal encontrado nesta categoria.</p>';
    return;
  }

  container.innerHTML = channels.slice(0, 100).map(c => `
    <div class="channel-card focusable" data-stream-id="${c.stream_id}">
      <img src="${c.stream_icon || '/logo-icon.svg'}" class="channel-logo" onerror="this.src='/logo-icon.svg'">
      <div class="channel-info truncate">
        <h4 class="truncate">${escapeHtml(c.name)}</h4>
        <p class="truncate">Canal ${c.num || ''} • Ao Vivo</p>
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.channel-card').forEach(card => {
    card.onclick = () => {
      const id = card.getAttribute('data-stream-id');
      const stream = channels.find(x => String(x.stream_id) === String(id));
      if (stream) playStream(stream, 'live');
    };
  });
}

function renderVodCategories(cats) {
  const container = document.getElementById('vod-categories-list');
  container.innerHTML = cats.map(c => `
    <div class="category-item focusable" data-cat-id="${c.category_id}">
      <span>${escapeHtml(c.category_name)}</span>
    </div>
  `).join('');
}

function renderVodItems(items) {
  const container = document.getElementById('vod-items-grid');
  document.getElementById('vod-count').innerText = `${items.length} filmes`;
  container.innerHTML = items.slice(0, 60).map(m => `
    <div class="channel-card focusable" data-stream-id="${m.stream_id}">
      <img src="${m.stream_icon || '/logo-icon.svg'}" class="channel-logo" onerror="this.src='/logo-icon.svg'">
      <div class="channel-info truncate">
        <h4 class="truncate">${escapeHtml(m.name)}</h4>
        <p class="truncate">Avaliação: ⭐ ${m.rating || '8.0'}</p>
      </div>
    </div>
  `).join('');
}

function renderHomeFavsRow() {
  const container = document.getElementById('home-favs-row');
  const items = deviceState.favorites.length > 0 ? deviceState.favorites : deviceState.liveChannels.slice(0, 10);
  container.innerHTML = items.map(c => `
    <div class="channel-card focusable min-w-[240px]" data-stream-id="${c.stream_id}">
      <img src="${c.stream_icon || '/logo-icon.svg'}" class="channel-logo" onerror="this.src='/logo-icon.svg'">
      <div class="channel-info truncate">
        <h4 class="truncate">${escapeHtml(c.name)}</h4>
        <p class="truncate">Ao Vivo</p>
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.channel-card').forEach(card => {
    card.onclick = () => {
      const id = card.getAttribute('data-stream-id');
      const stream = items.find(x => String(x.stream_id) === String(id));
      if (stream) playStream(stream, 'live');
    };
  });
}

function renderHomeMoviesRow(movies) {
  const container = document.getElementById('home-movies-row');
  container.innerHTML = movies.map(m => `
    <div class="channel-card focusable min-w-[240px]" data-stream-id="${m.stream_id}">
      <img src="${m.stream_icon || '/logo-icon.svg'}" class="channel-logo" onerror="this.src='/logo-icon.svg'">
      <div class="channel-info truncate">
        <h4 class="truncate">${escapeHtml(m.name)}</h4>
        <p class="truncate">⭐ ${m.rating || '8.5'}</p>
      </div>
    </div>
  `).join('');
}

// ==============================================================================
// 5. REPRODUTOR DE VÍDEO & OSD (HLS / MP4)
// ==============================================================================

function playStream(stream, type = 'live') {
  if (!deviceState.playlist) return;
  const { host, user, pass } = deviceState.playlist;
  const cleanHost = host.replace(/\/+$/, '');
  
  deviceState.activeStream = stream;
  deviceState.nowPlaying = stream.name || 'Canal';
  deviceState.nowPlayingType = type;

  // Monta a URL de streaming Xtream
  let streamUrl = '';
  if (type === 'live') {
    streamUrl = `${cleanHost}/live/${user}/${pass}/${stream.stream_id}.m3u8`;
  } else {
    const ext = stream.container_extension || 'mp4';
    streamUrl = `${cleanHost}/movie/${user}/${pass}/${stream.stream_id}.${ext}`;
  }

  // Preenche dados do OSD
  document.getElementById('osd-channel-name').innerText = stream.name || 'Canal ao Vivo';
  document.getElementById('osd-channel-logo').src = stream.stream_icon || '/logo-icon.svg';
  document.getElementById('player-container').classList.remove('hidden');

  const video = document.getElementById('main-video');
  video.style.objectFit = deviceState.aspectRatios[deviceState.aspectRatioIndex];

  if (Hls.isSupported() && (streamUrl.endsWith('.m3u8') || type === 'live')) {
    if (hlsPlayer) hlsPlayer.destroy();
    hlsPlayer = new Hls({
      xhrSetup: (xhr, url) => {
        // Redireciona requisições de fragmentos com proxy se necessário
      }
    });

    // Se o servidor IPTV não suportar CORS no browser, usa o proxy
    hlsPlayer.loadSource(`/api/proxy?url=${encodeURIComponent(streamUrl)}`);
    hlsPlayer.attachMedia(video);
    hlsPlayer.on(Hls.Events.MANIFEST_PARSED, () => video.play());
    hlsPlayer.on(Hls.Events.ERROR, (event, data) => {
      if (data.fatal) {
        // Fallback: tenta URL direta nativa
        video.src = streamUrl;
        video.play().catch(() => {});
      }
    });
  } else {
    video.src = streamUrl;
    video.play().catch(() => {});
  }

  showOsd();
  setFocus(document.getElementById('btn-osd-back'));
}

function showOsd() {
  const osd = document.getElementById('player-osd');
  osd.style.opacity = '1';
  if (osdTimer) clearTimeout(osdTimer);
  osdTimer = setTimeout(() => {
    osd.style.opacity = '0';
  }, 4000);
}

function closePlayer() {
  const video = document.getElementById('main-video');
  video.pause();
  if (hlsPlayer) {
    hlsPlayer.destroy();
    hlsPlayer = null;
  }
  document.getElementById('player-container').classList.add('hidden');
  deviceState.nowPlaying = '';
  deviceState.activeStream = null;
  setFocus(document.getElementById('side-home'));
}

function toggleAspectRatio() {
  deviceState.aspectRatioIndex = (deviceState.aspectRatioIndex + 1) % deviceState.aspectRatios.length;
  const ratio = deviceState.aspectRatios[deviceState.aspectRatioIndex];
  const label = deviceState.aspectLabels[deviceState.aspectRatioIndex];
  document.getElementById('main-video').style.objectFit = ratio;
  document.getElementById('osd-aspect-label').innerText = label;
  showOsd();
}

// ==============================================================================
// 6. NAVEGAÇÃO ESPACIAL PARA CONTROLE REMOTO D-PAD (SAMSUNG / LG / TV BOX)
// ==============================================================================

function setupDpadNavigation() {
  window.addEventListener('keydown', (e) => {
    // Teclas de controle remoto (Samsung Tizen, LG webOS e Teclado PC)
    const isUp = (e.key === 'ArrowUp' || e.keyCode === 38);
    const isDown = (e.key === 'ArrowDown' || e.keyCode === 40);
    const isLeft = (e.key === 'ArrowLeft' || e.keyCode === 37);
    const isRight = (e.key === 'ArrowRight' || e.keyCode === 39);
    const isEnter = (e.key === 'Enter' || e.keyCode === 13);
    const isBack = (e.key === 'Escape' || e.key === 'Backspace' || e.keyCode === 27 || e.keyCode === 10009 || e.keyCode === 461);

    // Se o player estiver aberto
    if (!document.getElementById('player-container').classList.contains('hidden')) {
      showOsd();
      if (isBack) {
        closePlayer();
        e.preventDefault();
        return;
      }
    }

    if (isBack) {
      if (deviceState.currentTab !== 'home') {
        switchTab('home');
        e.preventDefault();
      }
      return;
    }

    const currentFocus = document.activeElement;
    if (!currentFocus || !currentFocus.classList.contains('focusable')) {
      const first = document.querySelector('.focusable:not(.hidden)');
      if (first) setFocus(first);
      return;
    }

    if (isEnter) {
      currentFocus.click();
      e.preventDefault();
      return;
    }

    if (isUp || isDown || isLeft || isRight) {
      navigateSpatial(currentFocus, isUp, isDown, isLeft, isRight);
      e.preventDefault();
    }
  });
}

function navigateSpatial(current, up, down, left, right) {
  const focusables = Array.from(document.querySelectorAll('.focusable:not(.hidden)')).filter(el => {
    const rect = el.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && el.offsetParent !== null;
  });

  const currRect = current.getBoundingClientRect();
  const currCenter = { x: currRect.left + currRect.width / 2, y: currRect.top + currRect.height / 2 };

  let bestNext = null;
  let minDistance = Infinity;

  focusables.forEach(target => {
    if (target === current) return;
    const tRect = target.getBoundingClientRect();
    const tCenter = { x: tRect.left + tRect.width / 2, y: tRect.top + tRect.height / 2 };

    const dx = tCenter.x - currCenter.x;
    const dy = tCenter.y - currCenter.y;

    let isValidDirection = false;
    if (up && dy < -5 && Math.abs(dx) < Math.abs(dy) * 2) isValidDirection = true;
    if (down && dy > 5 && Math.abs(dx) < Math.abs(dy) * 2) isValidDirection = true;
    if (left && dx < -5 && Math.abs(dy) < Math.abs(dx) * 2) isValidDirection = true;
    if (right && dx > 5 && Math.abs(dy) < Math.abs(dx) * 2) isValidDirection = true;

    if (isValidDirection) {
      const dist = Math.hypot(dx, dy);
      if (dist < minDistance) {
        minDistance = dist;
        bestNext = target;
      }
    }
  });

  if (bestNext) {
    setFocus(bestNext);
  }
}

function setFocus(element) {
  if (!element) return;
  document.querySelectorAll('.focused').forEach(x => x.classList.remove('focused'));
  element.focus();
  element.classList.add('focused');
  element.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
}

// ==============================================================================
// 7. EVENTOS E NAVEGAÇÃO ENTRE ABAS
// ==============================================================================

function setupEventListeners() {
  // Botões da sidebar
  document.querySelectorAll('.sidebar-btn').forEach(btn => {
    btn.onclick = () => {
      const tab = btn.getAttribute('data-tab');
      switchTab(tab);
    };
  });

  // Botões do Player OSD
  document.getElementById('btn-osd-back').onclick = closePlayer;
  document.getElementById('btn-osd-aspect').onclick = toggleAspectRatio;
  document.getElementById('btn-osd-reload').onclick = () => {
    if (deviceState.activeStream) playStream(deviceState.activeStream, deviceState.nowPlayingType);
  };

  // Botão de verificar emparelhamento manual
  document.getElementById('btn-check-pairing').onclick = registerDevice;
  
  // Login Manual Xtream
  document.getElementById('btn-manual-login').onclick = () => {
    document.getElementById('modal-manual-login').classList.remove('hidden');
    setFocus(document.getElementById('manual-host'));
  };
  document.getElementById('btn-cancel-manual').onclick = () => {
    document.getElementById('modal-manual-login').classList.add('hidden');
    setFocus(document.getElementById('btn-manual-login'));
  };
  document.getElementById('btn-save-manual').onclick = () => {
    const host = document.getElementById('manual-host').value.trim();
    const user = document.getElementById('manual-user').value.trim();
    const pass = document.getElementById('manual-pass').value.trim();
    if (!host || !user || !pass) return alert('Preencha todos os campos!');

    deviceState.playlist = { type: 'xtream', host, user, pass };
    localStorage.setItem('korp_tv_manual_playlist', JSON.stringify(deviceState.playlist));
    document.getElementById('modal-manual-login').classList.add('hidden');
    showMainScreen();
    loadXtreamContent();
  };

  // Desconectar da Smart TV
  document.getElementById('btn-disconnect').onclick = () => {
    if (confirm('Deseja realmente desconectar esta Smart TV do portal?')) {
      localStorage.removeItem('korp_tv_manual_playlist');
      deviceState.playlist = null;
      showPairingScreen();
    }
  };
}

function switchTab(tabId) {
  deviceState.currentTab = tabId;
  document.querySelectorAll('.sidebar-btn').forEach(b => {
    b.classList.toggle('active', b.getAttribute('data-tab') === tabId);
  });
  document.querySelectorAll('.tab-pane').forEach(p => {
    p.classList.toggle('hidden', p.id !== `tab-${tabId}`);
    p.classList.toggle('active', p.id !== `tab-${tabId}`);
  });

  const firstFocus = document.querySelector(`#tab-${tabId} .focusable`);
  if (firstFocus) setFocus(firstFocus);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

window.onload = init;

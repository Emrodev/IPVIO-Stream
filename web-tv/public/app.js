// Korp TV — Smart TV & Web Engine (Samsung Tizen, LG webOS, Browsers)

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
  vodCache: {},
  seriesCategories: [],
  seriesCache: {},
  favorites: [],
  recent: []
};

let hlsPlayer = null;
let mpegtsPlayer = null;
let osdTimer = null;
let heartbeatInterval = null;

// =============================================================================
// 1. INICIALIZAÇÃO E IDENTIDADE DA SMART TV
// =============================================================================

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
      const display = document.getElementById('portal-url-display');
      if (display) display.innerText = config.portalUrl;
    })
    .catch(() => {});
}

function loadDeviceCredentials() {
  let devId = localStorage.getItem('korp_tv_device_id');
  if (!devId) {
    devId = 'webtv_' + Math.random().toString(36).substring(2, 9) + Date.now().toString(36).substring(4);
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
  document.getElementById('info-portal-url').innerText = config.portalUrl;

  const qrContainer = document.getElementById('pairing-qrcode');
  if (qrContainer && window.QRCode) {
    qrContainer.innerHTML = '';
    new QRCode(qrContainer, {
      text: config.portalUrl,
      width: 140,
      height: 140,
      colorDark: '#000000',
      colorLight: '#ffffff'
    });
  }
}

function loadLocalStoredData() {
  try {
    deviceState.favorites = JSON.parse(localStorage.getItem('korp_tv_favs') || '[]');
    deviceState.recent = JSON.parse(localStorage.getItem('korp_tv_recent') || '[]');
    const saved = localStorage.getItem('korp_tv_manual_playlist');
    if (saved) deviceState.playlist = JSON.parse(saved);
  } catch (_) {}
}

// =============================================================================
// 2. SINCRONIZAÇÃO COM O PORTAL (REGISTER & HEARTBEAT)
// =============================================================================

async function registerDevice() {
  try {
    const payload = {
      deviceId: deviceState.id,
      pairingCode: deviceState.pairingCode,
      model: 'Smart TV Web (Samsung/LG/Chromium)',
      appVersion: '1.0.0-web',
      versionCode: 100
    };

    const portalBase = config.localPortalUrl || config.portalUrl;
    let res = await fetch(`${portalBase}/api/device/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).catch(() => null);

    if (!res || !res.ok) {
      res = await fetch(`${config.portalUrl}/api/device/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      }).catch(() => null);
    }

    if (!res || !res.ok) return;
    const data = await res.json();
    handlePortalResponse(data);
  } catch (err) {
    console.warn('[Register Error]', err.message);
  }
}

function startHeartbeat() {
  if (heartbeatInterval) clearInterval(heartbeatInterval);
  heartbeatInterval = setInterval(async () => {
    try {
      const portalBase = config.localPortalUrl || config.portalUrl;
      const payload = {
        deviceId: deviceState.id,
        nowPlaying: deviceState.nowPlaying,
        nowPlayingType: deviceState.nowPlayingType
      };

      let res = await fetch(`${portalBase}/api/device/heartbeat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      }).catch(() => null);

      if (!res || !res.ok) {
        res = await fetch(`${config.portalUrl}/api/device/heartbeat`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        }).catch(() => null);
      }

      if (!res || !res.ok) return;
      const data = await res.json();
      handlePortalResponse(data);
    } catch (_) {}
  }, 5000);
}

function handlePortalResponse(data) {
  if (!data || !data.success) return;

  if (data.playlist && data.playlist.type !== 'none' && data.playlist.host) {
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
      loadAllContent();
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

// =============================================================================
// 3. CARREGAMENTO ROBUSTO DE DADOS XTREAM CODES
// =============================================================================

async function fetchXtream(action, extraParams = '') {
  if (!deviceState.playlist) return null;
  const { host, user, pass } = deviceState.playlist;
  if (!host || !user || !pass) return null;

  const cleanHost = host.replace(/\/+$/, '');
  const targetUrl = `${cleanHost}/player_api.php?username=${encodeURIComponent(user)}&password=${encodeURIComponent(pass)}&action=${action}${extraParams}`;

  try {
    const proxyUrl = `/api/proxy?url=${encodeURIComponent(targetUrl)}`;
    const res = await fetch(proxyUrl);
    if (res.ok) {
      return await res.json();
    }
  } catch (err) {
    console.warn(`[Xtream fetch failed: ${action}]`, err.message);
  }
  return null;
}

async function loadAllContent() {
  loadLiveContent();
  loadVodCategories();
  loadSeriesCategories();
}

// 3.1 Canais ao Vivo
async function loadLiveContent() {
  try {
    const cats = await fetchXtream('get_live_categories');
    if (Array.isArray(cats)) {
      deviceState.liveCategories = cats;
      renderLiveCategories(cats);
    }

    const streams = await fetchXtream('get_live_streams');
    if (Array.isArray(streams)) {
      deviceState.liveChannels = streams;
      renderLiveChannels(streams);
      renderHomeChannelsRow(streams.slice(0, 15));
      if (streams[0]) updateHeroBillboard(streams[0], 'live');
    }
  } catch (err) {
    console.error('[Live Content Error]', err);
  }
}

// 3.2 Filmes (VOD)
async function loadVodCategories() {
  try {
    const cats = await fetchXtream('get_vod_categories');
    if (Array.isArray(cats)) {
      deviceState.vodCategories = cats;
      renderVodCategories(cats);
      // Carrega primeiros filmes para o Hero e a primeira categoria
      if (cats.length > 0) {
        selectVodCategory(cats[0].category_id);
      }
    }
  } catch (err) {
    console.error('[VOD Error]', err);
  }
}

async function selectVodCategory(catId) {
  if (deviceState.vodCache[catId]) {
    renderVodItems(deviceState.vodCache[catId]);
    return;
  }
  const items = await fetchXtream('get_vod_streams', `&category_id=${catId}`);
  if (Array.isArray(items)) {
    deviceState.vodCache[catId] = items;
    renderVodItems(items);
    if (!deviceState.homeMoviesLoaded && items.length > 0) {
      renderHomeMoviesRow(items.slice(0, 15));
      deviceState.homeMoviesLoaded = true;
    }
  }
}

// 3.3 Séries
async function loadSeriesCategories() {
  try {
    const cats = await fetchXtream('get_series_categories');
    if (Array.isArray(cats)) {
      deviceState.seriesCategories = cats;
      renderSeriesCategories(cats);
      if (cats.length > 0) {
        selectSeriesCategory(cats[0].category_id);
      }
    }
  } catch (err) {
    console.error('[Series Error]', err);
  }
}

async function selectSeriesCategory(catId) {
  if (deviceState.seriesCache[catId]) {
    renderSeriesItems(deviceState.seriesCache[catId]);
    return;
  }
  const items = await fetchXtream('get_series', `&category_id=${catId}`);
  if (Array.isArray(items)) {
    deviceState.seriesCache[catId] = items;
    renderSeriesItems(items);
    if (!deviceState.homeSeriesLoaded && items.length > 0) {
      renderHomeSeriesRow(items.slice(0, 15));
      deviceState.homeSeriesLoaded = true;
    }
  }
}

// =============================================================================
// 4. RENDERIZAÇÃO DA INTERFACE (CANVAS DA SMART TV)
// =============================================================================

function updateHeroBillboard(item, type = 'live') {
  if (!item) return;
  document.getElementById('hero-title').innerText = item.name || 'Destaques Korp TV';
  const heroCard = document.getElementById('hero-billboard');
  const bgImg = item.stream_icon || item.cover;
  if (bgImg) {
    heroCard.style.backgroundImage = `url('${bgImg}')`;
  }
  document.getElementById('btn-hero-play').onclick = () => playStream(item, type);
}

// 4.1 Renderizar Categorias e Canais de TV
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
      if (catId === 'all') {
        renderLiveChannels(deviceState.liveChannels);
      } else {
        renderLiveChannels(deviceState.liveChannels.filter(c => String(c.category_id) === String(catId)));
      }
    };
  });
}

function renderLiveChannels(channels) {
  const container = document.getElementById('live-channels-grid');
  document.getElementById('channel-count').innerText = `${channels.length} canais`;

  if (channels.length === 0) {
    container.innerHTML = '<p class="text-sm text-gray-500 col-span-full">Nenhum canal encontrado nesta categoria.</p>';
    return;
  }

  container.innerHTML = channels.slice(0, 150).map(c => `
    <div class="channel-card focusable" data-stream-id="${c.stream_id}">
      <img src="${c.stream_icon || '/logo-icon.svg'}" class="channel-logo" onerror="this.src='/logo-icon.svg'">
      <div class="channel-info">
        <h4>${escapeHtml(c.name)}</h4>
        <p>Canal ${c.num || ''} • Ao Vivo</p>
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

// 4.2 Renderizar Filmes (VOD com Pôsteres 2:3)
function renderVodCategories(cats) {
  const container = document.getElementById('vod-categories-list');
  document.getElementById('vod-cat-count').innerText = cats.length;
  container.innerHTML = cats.map((c, i) => `
    <div class="category-item focusable ${i === 0 ? 'active' : ''}" data-cat-id="${c.category_id}">
      <span>${escapeHtml(c.category_name)}</span>
    </div>
  `).join('');

  container.querySelectorAll('.category-item').forEach(el => {
    el.onclick = () => {
      container.querySelectorAll('.category-item').forEach(x => x.classList.remove('active'));
      el.classList.add('active');
      const catId = el.getAttribute('data-cat-id');
      document.getElementById('vod-category-title').innerText = el.innerText.trim();
      selectVodCategory(catId);
    };
  });
}

function renderVodItems(items) {
  const container = document.getElementById('vod-items-grid');
  document.getElementById('vod-count').innerText = `${items.length} filmes`;

  if (items.length === 0) {
    container.innerHTML = '<p class="text-sm text-gray-500 col-span-full">Nenhum filme disponível nesta categoria.</p>';
    return;
  }

  container.innerHTML = items.slice(0, 80).map(m => `
    <div class="poster-card focusable" data-stream-id="${m.stream_id}">
      <div class="poster-cover-box">
        <img src="${m.stream_icon || '/logo-icon.svg'}" class="poster-cover" loading="lazy" onerror="this.src='/logo-icon.svg'">
        <span class="poster-badge">★ ${m.rating || '8.2'}</span>
      </div>
      <div class="poster-info">
        <h4>${escapeHtml(m.name)}</h4>
        <p>${m.release_date || m.year || 'Filme'}</p>
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.poster-card').forEach(card => {
    card.onclick = () => {
      const id = card.getAttribute('data-stream-id');
      const stream = items.find(x => String(x.stream_id) === String(id));
      if (stream) playStream(stream, 'movie');
    };
  });
}

// 4.3 Renderizar Séries (Com Pôsteres 2:3 e Modal de Episódios)
function renderSeriesCategories(cats) {
  const container = document.getElementById('series-categories-list');
  document.getElementById('series-cat-count').innerText = cats.length;
  container.innerHTML = cats.map((c, i) => `
    <div class="category-item focusable ${i === 0 ? 'active' : ''}" data-cat-id="${c.category_id}">
      <span>${escapeHtml(c.category_name)}</span>
    </div>
  `).join('');

  container.querySelectorAll('.category-item').forEach(el => {
    el.onclick = () => {
      container.querySelectorAll('.category-item').forEach(x => x.classList.remove('active'));
      el.classList.add('active');
      const catId = el.getAttribute('data-cat-id');
      document.getElementById('series-category-title').innerText = el.innerText.trim();
      selectSeriesCategory(catId);
    };
  });
}

function renderSeriesItems(items) {
  const container = document.getElementById('series-items-grid');
  document.getElementById('series-count').innerText = `${items.length} séries`;

  if (items.length === 0) {
    container.innerHTML = '<p class="text-sm text-gray-500 col-span-full">Nenhuma série disponível nesta categoria.</p>';
    return;
  }

  container.innerHTML = items.slice(0, 80).map(s => `
    <div class="poster-card focusable" data-series-id="${s.series_id}">
      <div class="poster-cover-box">
        <img src="${s.cover || '/logo-icon.svg'}" class="poster-cover" loading="lazy" onerror="this.src='/logo-icon.svg'">
        <span class="poster-badge">★ ${s.rating || '8.5'}</span>
      </div>
      <div class="poster-info">
        <h4>${escapeHtml(s.name)}</h4>
        <p>Série Completa</p>
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.poster-card').forEach(card => {
    card.onclick = () => {
      const id = card.getAttribute('data-series-id');
      const series = items.find(x => String(x.series_id) === String(id));
      if (series) openSeriesModal(series);
    };
  });
}

// 4.4 Fileiras da Home (Canais, Filmes, Séries)
function renderHomeChannelsRow(channels) {
  const container = document.getElementById('home-channels-row');
  container.innerHTML = channels.map(c => `
    <div class="channel-card focusable min-w-[240px]" data-stream-id="${c.stream_id}">
      <img src="${c.stream_icon || '/logo-icon.svg'}" class="channel-logo" onerror="this.src='/logo-icon.svg'">
      <div class="channel-info">
        <h4>${escapeHtml(c.name)}</h4>
        <p>Ao Vivo</p>
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

function renderHomeMoviesRow(movies) {
  const container = document.getElementById('home-movies-row');
  container.innerHTML = movies.map(m => `
    <div class="poster-card focusable min-w-[160px] max-w-[160px]" data-stream-id="${m.stream_id}">
      <div class="poster-cover-box">
        <img src="${m.stream_icon || '/logo-icon.svg'}" class="poster-cover" loading="lazy" onerror="this.src='/logo-icon.svg'">
        <span class="poster-badge">★ ${m.rating || '8.0'}</span>
      </div>
      <div class="poster-info">
        <h4>${escapeHtml(m.name)}</h4>
        <p>${m.release_date || m.year || 'Filme'}</p>
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.poster-card').forEach(card => {
    card.onclick = () => {
      const id = card.getAttribute('data-stream-id');
      const stream = movies.find(x => String(x.stream_id) === String(id));
      if (stream) playStream(stream, 'movie');
    };
  });
}

function renderHomeSeriesRow(series) {
  const container = document.getElementById('home-series-row');
  container.innerHTML = series.map(s => `
    <div class="poster-card focusable min-w-[160px] max-w-[160px]" data-series-id="${s.series_id}">
      <div class="poster-cover-box">
        <img src="${s.cover || '/logo-icon.svg'}" class="poster-cover" loading="lazy" onerror="this.src='/logo-icon.svg'">
        <span class="poster-badge">★ ${s.rating || '8.5'}</span>
      </div>
      <div class="poster-info">
        <h4>${escapeHtml(s.name)}</h4>
        <p>Série</p>
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.poster-card').forEach(card => {
    card.onclick = () => {
      const id = card.getAttribute('data-series-id');
      const item = series.find(x => String(x.series_id) === String(id));
      if (item) openSeriesModal(item);
    };
  });
}

// =============================================================================
// 5. MODAL DE SÉRIES (TEMPORADAS E EPISÓDIOS)
// =============================================================================

async function openSeriesModal(series) {
  const modal = document.getElementById('modal-series-details');
  document.getElementById('series-modal-title').innerText = series.name || 'Série';
  document.getElementById('series-modal-cover').src = series.cover || '/logo-icon.svg';
  document.getElementById('series-modal-desc').innerText = series.plot || 'Selecione uma temporada para ver os episódios disponíveis.';
  modal.classList.remove('hidden');

  setFocus(document.getElementById('btn-close-series-modal'));

  const seasonsContainer = document.getElementById('series-seasons-list');
  const episodesContainer = document.getElementById('series-episodes-list');
  seasonsContainer.innerHTML = '<p class="text-xs text-gray-500">Carregando...</p>';
  episodesContainer.innerHTML = '<p class="text-xs text-gray-500">Carregando episódios...</p>';

  const details = await fetchXtream('get_series_info', `&series_id=${series.series_id}`);
  if (!details || !details.episodes) {
    episodesContainer.innerHTML = '<p class="text-xs text-red-400">Nenhum episódio encontrado para esta série.</p>';
    return;
  }

  const seasonKeys = Object.keys(details.episodes).sort((a, b) => Number(a) - Number(b));
  seasonsContainer.innerHTML = seasonKeys.map((s, idx) => `
    <button class="focusable px-3 py-2 rounded-xl text-left text-xs font-bold border border-gray-800 ${idx === 0 ? 'bg-cyan-600 text-white' : 'bg-surface-800 text-gray-300'}" data-season="${s}">
      Temporada ${s}
    </button>
  `).join('');

  const renderEpisodesForSeason = (seasonKey) => {
    const eps = details.episodes[seasonKey] || [];
    episodesContainer.innerHTML = eps.map(ep => `
      <div class="episode-card focusable" data-ep-id="${ep.id}">
        <div class="flex items-center gap-3">
          <span class="w-8 h-8 rounded-lg bg-surface-900 border border-gray-700 flex items-center justify-center font-bold text-xs text-cyan-400">
            ${ep.episode_num || 'E'}
          </span>
          <div>
            <h5 class="text-xs font-bold text-white">${escapeHtml(ep.title || `Episódio ${ep.episode_num}`)}</h5>
            <p class="text-[10px] text-gray-400">${ep.info?.duration || ''}</p>
          </div>
        </div>
        <button class="px-3 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white text-[11px] font-bold">
          ▶ Assistir
        </button>
      </div>
    `).join('');

    episodesContainer.querySelectorAll('.episode-card').forEach(card => {
      card.onclick = () => {
        const epId = card.getAttribute('data-ep-id');
        const ep = eps.find(x => String(x.id) === String(epId));
        if (ep) {
          modal.classList.add('hidden');
          playSeriesEpisode(ep, series);
        }
      };
    });
  };

  if (seasonKeys.length > 0) {
    renderEpisodesForSeason(seasonKeys[0]);
  }

  seasonsContainer.querySelectorAll('button').forEach(btn => {
    btn.onclick = () => {
      seasonsContainer.querySelectorAll('button').forEach(b => {
        b.classList.remove('bg-cyan-600', 'text-white');
        b.classList.add('bg-surface-800', 'text-gray-300');
      });
      btn.classList.add('bg-cyan-600', 'text-white');
      btn.classList.remove('bg-surface-800', 'text-gray-300');
      renderEpisodesForSeason(btn.getAttribute('data-season'));
    };
  });
}

function playSeriesEpisode(episode, series) {
  if (!deviceState.playlist) return;
  const { host, user, pass } = deviceState.playlist;
  const cleanHost = host.replace(/\/+$/, '');
  const ext = episode.container_extension || 'mp4';
  const streamUrl = `${cleanHost}/series/${user}/${pass}/${episode.id}.${ext}`;

  deviceState.activeStream = episode;
  deviceState.nowPlaying = `${series.name} — Ep. ${episode.episode_num || ''}`;
  deviceState.nowPlayingType = 'series';

  destroyActivePlayers();

  document.getElementById('osd-channel-name').innerText = deviceState.nowPlaying;
  document.getElementById('osd-channel-logo').src = series.cover || '/logo-icon.svg';
  document.getElementById('osd-program-name').innerText = 'Carregando episódio...';
  document.getElementById('player-container').classList.remove('hidden');

  const video = document.getElementById('main-video');
  video.style.objectFit = deviceState.aspectRatios[deviceState.aspectRatioIndex];

  video.onplaying = () => {
    document.getElementById('osd-program-name').innerText = 'Reproduzindo Episódio';
  };

  video.src = `/api/proxy?url=${encodeURIComponent(streamUrl)}`;
  video.play().catch(() => {
    video.src = streamUrl;
    video.play().catch(() => {});
  });

  showOsd();
  setFocus(document.getElementById('btn-osd-back'));
}

// =============================================================================
// 6. REPRODUTOR DE VÍDEO & CONTROLES DO PLAYER (OSD)
// =============================================================================

function playStream(stream, type = 'live') {
  if (!deviceState.playlist) return;
  const { host, user, pass } = deviceState.playlist;
  const cleanHost = host.replace(/\/+$/, '');

  deviceState.activeStream = stream;
  deviceState.nowPlaying = stream.name || 'Canal';
  deviceState.nowPlayingType = type;

  // Limpa instâncias anteriores de players
  destroyActivePlayers();

  document.getElementById('osd-channel-name').innerText = stream.name || 'Canal';
  document.getElementById('osd-channel-logo').src = stream.stream_icon || '/logo-icon.svg';
  document.getElementById('osd-program-name').innerText = 'Conectando ao canal...';
  document.getElementById('player-container').classList.remove('hidden');

  const video = document.getElementById('main-video');
  video.style.objectFit = deviceState.aspectRatios[deviceState.aspectRatioIndex];

  video.onplaying = () => {
    document.getElementById('osd-program-name').innerText = (type === 'live' ? 'Ao Vivo • Reproduzindo' : 'Reproduzindo Conteúdo');
  };

  video.onerror = () => {
    if (type === 'live') {
      tryPlayMpegtsFallback(cleanHost, user, pass, stream.stream_id);
    }
  };

  if (type === 'live') {
    playLiveChannel(cleanHost, user, pass, stream.stream_id);
  } else {
    const ext = stream.container_extension || 'mp4';
    const movieUrl = `${cleanHost}/movie/${user}/${pass}/${stream.stream_id}.${ext}`;
    video.src = `/api/proxy?url=${encodeURIComponent(movieUrl)}`;
    video.play().catch(() => {
      video.src = movieUrl;
      video.play().catch(() => {});
    });
  }

  showOsd();
  setFocus(document.getElementById('btn-osd-back'));
}

// Reprodução de canais ao vivo (HLS com fallback automático para MPEG-TS)
function playLiveChannel(host, user, pass, streamId) {
  const video = document.getElementById('main-video');
  const hlsUrl = `${host}/live/${user}/${pass}/${streamId}.m3u8`;
  const proxiedHlsUrl = `/api/proxy?url=${encodeURIComponent(hlsUrl)}`;

  if (Hls.isSupported()) {
    hlsPlayer = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      manifestLoadingTimeOut: 8000,
      manifestLoadingMaxRetry: 1
    });

    hlsPlayer.loadSource(proxiedHlsUrl);
    hlsPlayer.attachMedia(video);

    hlsPlayer.on(Hls.Events.MANIFEST_PARSED, () => {
      video.play().catch(() => {});
    });

    hlsPlayer.on(Hls.Events.ERROR, (event, data) => {
      if (data.fatal) {
        console.warn('[HLS Fatal Error, tentando MPEG-TS]', data.type);
        destroyActivePlayers();
        tryPlayMpegtsFallback(host, user, pass, streamId);
      }
    });
  } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
    // Safari / iOS / Mac onde HLS é nativo
    video.src = proxiedHlsUrl;
    video.play().catch(() => tryPlayMpegtsFallback(host, user, pass, streamId));
  } else {
    tryPlayMpegtsFallback(host, user, pass, streamId);
  }
}

// Fallback de canal via fluxo MPEG-TS contínuo (compatível com 100% dos painéis Xtream)
function tryPlayMpegtsFallback(host, user, pass, streamId) {
  const video = document.getElementById('main-video');
  const tsUrl = `${host}/live/${user}/${pass}/${streamId}.ts`;
  const proxiedTsUrl = `/api/proxy?url=${encodeURIComponent(tsUrl)}`;
  document.getElementById('osd-program-name').innerText = 'Sincronizando fluxo direto...';

  if (window.mpegts && mpegts.isSupported()) {
    try {
      mpegtsPlayer = mpegts.createPlayer({
        type: 'mse',
        isLive: true,
        url: proxiedTsUrl
      }, {
        enableWorker: true,
        lazyLoadMaxDuration: 3 * 60,
        seekType: 'range'
      });

      mpegtsPlayer.attachMediaElement(video);
      mpegtsPlayer.load();
      mpegtsPlayer.play().catch(err => {
        console.warn('[MPEG-TS Play Error]', err);
      });
    } catch (err) {
      console.warn('[MPEG-TS init error]', err);
      video.src = proxiedTsUrl;
      video.play().catch(() => {});
    }
  } else {
    video.src = proxiedTsUrl;
    video.play().catch(() => {});
  }
}

function destroyActivePlayers() {
  const video = document.getElementById('main-video');
  if (hlsPlayer) {
    try { hlsPlayer.destroy(); } catch (_) {}
    hlsPlayer = null;
  }
  if (mpegtsPlayer) {
    try { mpegtsPlayer.destroy(); } catch (_) {}
    mpegtsPlayer = null;
  }
  if (video) {
    video.pause();
    video.removeAttribute('src');
    video.load();
  }
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
  destroyActivePlayers();
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

// =============================================================================
// 7. NAVEGAÇÃO ESPACIAL PARA CONTROLE REMOTO D-PAD
// =============================================================================

function setupDpadNavigation() {
  window.addEventListener('keydown', (e) => {
    const isUp = (e.key === 'ArrowUp' || e.keyCode === 38);
    const isDown = (e.key === 'ArrowDown' || e.keyCode === 40);
    const isLeft = (e.key === 'ArrowLeft' || e.keyCode === 37);
    const isRight = (e.key === 'ArrowRight' || e.keyCode === 39);
    const isEnter = (e.key === 'Enter' || e.keyCode === 13);
    const isBack = (e.key === 'Escape' || e.key === 'Backspace' || e.keyCode === 27 || e.keyCode === 10009 || e.keyCode === 461);

    // Se o player estiver ativo
    if (!document.getElementById('player-container').classList.contains('hidden')) {
      showOsd();
      if (isBack) {
        closePlayer();
        e.preventDefault();
        return;
      }
    }

    // Se o modal de séries estiver ativo
    const seriesModal = document.getElementById('modal-series-details');
    if (!seriesModal.classList.contains('hidden') && isBack) {
      seriesModal.classList.add('hidden');
      e.preventDefault();
      return;
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

    let isValid = false;
    if (up && dy < -5 && Math.abs(dx) < Math.abs(dy) * 2.2) isValid = true;
    if (down && dy > 5 && Math.abs(dx) < Math.abs(dy) * 2.2) isValid = true;
    if (left && dx < -5 && Math.abs(dy) < Math.abs(dx) * 2.2) isValid = true;
    if (right && dx > 5 && Math.abs(dy) < Math.abs(dx) * 2.2) isValid = true;

    if (isValid) {
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

// =============================================================================
// 8. EVENTOS DE CLIQUE E NAVEGAÇÃO DE ABAS
// =============================================================================

function setupEventListeners() {
  document.querySelectorAll('.sidebar-btn').forEach(btn => {
    btn.onclick = () => {
      const tab = btn.getAttribute('data-tab');
      switchTab(tab);
    };
  });

  document.getElementById('btn-osd-back').onclick = closePlayer;
  document.getElementById('btn-osd-aspect').onclick = toggleAspectRatio;
  document.getElementById('btn-osd-reload').onclick = () => {
    if (deviceState.activeStream) playStream(deviceState.activeStream, deviceState.nowPlayingType);
  };

  document.getElementById('btn-check-pairing').onclick = registerDevice;
  document.getElementById('btn-close-series-modal').onclick = () => {
    document.getElementById('modal-series-details').classList.add('hidden');
  };

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
    loadAllContent();
  };

  document.getElementById('btn-reload-playlist').onclick = () => {
    deviceState.vodCache = {};
    deviceState.seriesCache = {};
    loadAllContent();
  };

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

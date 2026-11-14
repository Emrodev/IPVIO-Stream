/**
 * Consulta a validade da lista diretamente no servidor (Xtream Codes ou M3U).
 */

function formatTimestamp(timestampSec) {
  if (!timestampSec || timestampSec === 'null' || timestampSec === '0') {
    return { formatted: 'Ilimitado', daysRemaining: 9999, isExpired: false, raw: null };
  }

  const sec = parseInt(timestampSec, 10);
  if (isNaN(sec) || sec <= 0) {
    return { formatted: 'Ilimitado', daysRemaining: 9999, isExpired: false, raw: null };
  }

  const expDate = new Date(sec * 1000);
  const now = new Date();
  const diffDays = Math.ceil((expDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

  const d = String(expDate.getDate()).padStart(2, '0');
  const m = String(expDate.getMonth() + 1).padStart(2, '0');
  const y = expDate.getFullYear();

  return {
    formatted: `${d}/${m}/${y}`,
    daysRemaining: diffDays,
    isExpired: diffDays <= 0,
    raw: sec
  };
}

async function checkXtream(host, user, pass) {
  try {
    let cleanHost = host.trim().replace(/\/+$/, '');
    if (!cleanHost.startsWith('http://') && !cleanHost.startsWith('https://')) {
      cleanHost = 'http://' + cleanHost;
    }

    const apiUrl = `${cleanHost}/player_api.php?username=${encodeURIComponent(user)}&password=${encodeURIComponent(pass)}`;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);

    const res = await fetch(apiUrl, { signal: controller.signal });
    clearTimeout(timeout);

    if (!res.ok) {
      return { success: false, error: `Servidor retornou status HTTP ${res.status}` };
    }

    const data = await res.json();
    const userInfo = data.user_info || {};

    if (data.user_info === undefined && data.auth === 0) {
      return { success: false, error: 'Usuário ou senha incorretos' };
    }

    const exp = formatTimestamp(userInfo.exp_date);

    return {
      success: true,
      status: userInfo.status || 'Active',
      expDateFormatted: exp.formatted,
      daysRemaining: exp.daysRemaining,
      isExpired: exp.isExpired,
      rawExpDate: exp.raw,
      maxConnections: parseInt(userInfo.max_connections || '1', 10),
      activeConnections: parseInt(userInfo.active_cons || '0', 10)
    };
  } catch (err) {
    return { success: false, error: err.message || 'Erro de conexão com o servidor da lista' };
  }
}

// Extrai host, user e pass de uma URL M3U estilo get.php se existirem
function parseM3uUrl(url) {
  try {
    const u = new URL(url);
    const username = u.searchParams.get('username') || u.searchParams.get('user');
    const password = u.searchParams.get('password') || u.searchParams.get('pass');
    if (username && password) {
      const host = `${u.protocol}//${u.host}`;
      return { host, user: username, pass: password };
    }
  } catch (_) {}
  return null;
}

async function checkPlaylistValidity({ type, host, user, pass, url }) {
  if (type === 'xtream' && host && user && pass) {
    return await checkXtream(host, user, pass);
  }

  if (type === 'm3u' && url) {
    const parsed = parseM3uUrl(url);
    if (parsed) {
      return await checkXtream(parsed.host, parsed.user, parsed.pass);
    }
    // Lista M3U genérica sem credenciais Xtream na query
    return {
      success: true,
      status: 'Active',
      expDateFormatted: 'M3U (Sem API)',
      daysRemaining: null,
      isExpired: false
    };
  }

  return { success: false, error: 'Parâmetros de playlist incompletos' };
}

module.exports = {
  checkPlaylistValidity,
  formatTimestamp
};

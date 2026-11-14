const express = require('express');
const router = express.Router();
const db = require('../db');

function getClientIp(req) {
  const forwarded = req.headers['x-forwarded-for'];
  if (forwarded) return forwarded.split(',')[0].trim();
  return req.socket.remoteAddress || req.ip || '';
}

async function getLatestOta(currentCode = 0) {
  const res = await db.query('SELECT * FROM ota_updates ORDER BY version_code DESC LIMIT 1');
  if (res.rows.length === 0) return null;
  const latest = res.rows[0];
  const hasUpdate = latest.version_code > currentCode;
  return {
    hasUpdate,
    versionName: latest.version_name,
    versionCode: latest.version_code,
    url: latest.download_url,
    changelog: latest.changelog,
    mandatory: Boolean(latest.mandatory)
  };
}

// Resposta unificada para register e heartbeat
async function buildDeviceResponse(device, currentCode = 0) {
  const ota = await getLatestOta(currentCode);
  return {
    success: true,
    status: device.status || 'active',
    name: device.name || 'Dispositivo sem nome',
    playlist: {
      type: device.playlist_type || 'none',
      url: device.playlist_url || '',
      host: device.xtream_host || '',
      user: device.xtream_user || '',
      pass: device.xtream_pass || '',
      updatedAt: Number(device.playlist_updated_at || 0)
    },
    message: device.message ? {
      id: device.message_id || 'msg_default',
      text: device.message,
      type: device.message_type || 'info'
    } : null,
    ota
  };
}

function derivePairingCode(mac, deviceId) {
  const source = (mac || '').replace(/[^a-zA-Z0-9]/g, '') || (deviceId || '').replace(/[^a-zA-Z0-9]/g, '');
  const hex = source.slice(-6).toUpperCase();
  if (hex.length === 6) {
    return `${hex.slice(0, 3)}-${hex.slice(3)}`;
  }
  return hex || '000-000';
}

// 1. Registro inicial do dispositivo
router.post('/register', async (req, res) => {
  try {
    const { deviceId, mac, model, appVersion, versionCode, pairingCode } = req.body;
    if (!deviceId) {
      return res.status(400).json({ success: false, error: 'deviceId obrigatório' });
    }

    const ip = getClientIp(req);
    const now = Date.now();
    const vCode = parseInt(versionCode || '0', 10);
    const code = pairingCode || derivePairingCode(mac, deviceId);

    const existing = await db.query('SELECT * FROM devices WHERE id = ?', [deviceId]);
    if (existing.rows.length === 0) {
      // Novo dispositivo
      await db.query(
        `INSERT INTO devices (id, mac, pairing_code, name, model, app_version, ip, status, created_at, last_seen)
         VALUES (?, ?, ?, ?, ?, ?, ?, 'active', ?, ?)`,
        [deviceId, mac || '', code, model || 'Dispositivo Desconhecido', model || 'Dispositivo Desconhecido', appVersion || '', ip, now, now]
      );
      const dev = (await db.query('SELECT * FROM devices WHERE id = ?', [deviceId])).rows[0];
      return res.json(await buildDeviceResponse(dev, vCode));
    } else {
      // Dispositivo existente -> atualiza último acesso e dados
      await db.query(
        `UPDATE devices SET model = ?, app_version = ?, ip = ?, last_seen = ?, pairing_code = COALESCE(?, pairing_code) WHERE id = ?`,
        [model || existing.rows[0].model, appVersion || existing.rows[0].app_version, ip, now, code, deviceId]
      );
      const dev = (await db.query('SELECT * FROM devices WHERE id = ?', [deviceId])).rows[0];
      return res.json(await buildDeviceResponse(dev, vCode));
    }
  } catch (err) {
    console.error('[Device Register Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 2. Heartbeat periódico
router.post('/heartbeat', async (req, res) => {
  try {
    const { deviceId, versionCode, nowPlaying, nowPlayingType } = req.body;
    if (!deviceId) {
      return res.status(400).json({ success: false, error: 'deviceId obrigatório' });
    }

    const ip = getClientIp(req);
    const now = Date.now();
    const vCode = parseInt(versionCode || '0', 10);

    const existing = await db.query('SELECT * FROM devices WHERE id = ?', [deviceId]);
    if (existing.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'Dispositivo não encontrado. Registre-se primeiro.' });
    }

    if (nowPlaying !== undefined) {
      await db.query(
        'UPDATE devices SET ip = ?, last_seen = ?, now_playing = ?, now_playing_type = ?, now_playing_at = ? WHERE id = ?',
        [ip, now, nowPlaying || null, nowPlayingType || null, nowPlaying ? now : 0, deviceId]
      );
    } else {
      await db.query('UPDATE devices SET ip = ?, last_seen = ? WHERE id = ?', [ip, now, deviceId]);
    }

    const dev = (await db.query('SELECT * FROM devices WHERE id = ?', [deviceId])).rows[0];
    return res.json(await buildDeviceResponse(dev, vCode));
  } catch (err) {
    console.error('[Device Heartbeat Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 2.1 Atualização imediata do conteúdo em reprodução (Now Playing)
router.post('/now-playing', async (req, res) => {
  try {
    const { deviceId, title, type } = req.body;
    if (!deviceId) {
      return res.status(400).json({ success: false, error: 'deviceId obrigatório' });
    }
    const now = Date.now();
    await db.query(
      'UPDATE devices SET now_playing = ?, now_playing_type = ?, now_playing_at = ?, last_seen = ? WHERE id = ?',
      [title || null, type || null, title ? now : 0, now, deviceId]
    );
    res.json({ success: true });
  } catch (err) {
    console.error('[Now Playing Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 2.2 Armazenamento de Backup do Dispositivo
router.post('/backup', async (req, res) => {
  try {
    const { deviceId, accountKey, backupData, summary } = req.body;
    if (!deviceId || !backupData) {
      return res.status(400).json({ success: false, error: 'deviceId e backupData são obrigatórios' });
    }
    const now = Date.now();
    const summaryStr = typeof summary === 'object' ? JSON.stringify(summary) : (summary || '');

    const existing = await db.query('SELECT device_id FROM device_backups WHERE device_id = ?', [deviceId]);
    if (existing.rows.length === 0) {
      await db.query(
        'INSERT INTO device_backups (device_id, account_key, summary, backup_data, updated_at) VALUES (?, ?, ?, ?, ?)',
        [deviceId, accountKey || '', summaryStr, backupData, now]
      );
    } else {
      await db.query(
        'UPDATE device_backups SET account_key = ?, summary = ?, backup_data = ?, updated_at = ? WHERE device_id = ?',
        [accountKey || '', summaryStr, backupData, now, deviceId]
      );
    }
    res.json({ success: true, updatedAt: now });
  } catch (err) {
    console.error('[Device Backup Save Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 2.3 Recuperação do Backup do Dispositivo (ou por conta compartilhada)
router.get('/backup', async (req, res) => {
  try {
    const { deviceId, accountKey } = req.query;
    let query = 'SELECT * FROM device_backups WHERE device_id = ?';
    let params = [deviceId];

    if (!deviceId && accountKey) {
      query = 'SELECT * FROM device_backups WHERE account_key = ? ORDER BY updated_at DESC LIMIT 1';
      params = [accountKey];
    } else if (deviceId && accountKey) {
      query = 'SELECT * FROM device_backups WHERE device_id = ? OR account_key = ? ORDER BY updated_at DESC LIMIT 1';
      params = [deviceId, accountKey];
    }

    const result = await db.query(query, params);
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'Nenhum backup encontrado' });
    }
    const row = result.rows[0];
    let summary = null;
    try { summary = JSON.parse(row.summary); } catch (_) { summary = row.summary; }

    res.json({
      success: true,
      deviceId: row.device_id,
      accountKey: row.account_key,
      summary,
      backupData: row.backup_data,
      updatedAt: Number(row.updated_at)
    });
  } catch (err) {
    console.error('[Device Backup Load Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 2.4 Sincronização de Histórico e Progresso de Episódios / Séries / Filmes (Cross-Device)
router.post('/sync/progress', async (req, res) => {
  try {
    const { accountKey, items } = req.body;
    if (!accountKey || !Array.isArray(items) || items.length === 0) {
      return res.status(400).json({ success: false, error: 'accountKey e lista items são obrigatórios' });
    }

    const now = Date.now();
    for (const item of items) {
      const { itemId, syncType, data, updatedAt } = item;
      if (!itemId || !syncType || !data) continue;
      const dataStr = typeof data === 'object' ? JSON.stringify(data) : String(data);
      const itemTime = updatedAt || now;

      // Upsert
      const existing = await db.query(
        'SELECT updated_at FROM account_sync WHERE account_key = ? AND sync_type = ? AND item_id = ?',
        [accountKey, syncType, itemId]
      );

      if (existing.rows.length === 0) {
        await db.query(
          'INSERT INTO account_sync (account_key, sync_type, item_id, data_json, updated_at) VALUES (?, ?, ?, ?, ?)',
          [accountKey, syncType, itemId, dataStr, itemTime]
        );
      } else if (itemTime >= Number(existing.rows[0].updated_at || 0)) {
        await db.query(
          'UPDATE account_sync SET data_json = ?, updated_at = ? WHERE account_key = ? AND sync_type = ? AND item_id = ?',
          [dataStr, itemTime, accountKey, syncType, itemId]
        );
      }
    }

    res.json({ success: true, count: items.length });
  } catch (err) {
    console.error('[Sync Progress Save Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 2.5 Obtenção do Progresso Sincronizado para outros aparelhos da mesma conta
router.get('/sync/progress', async (req, res) => {
  try {
    const { accountKey, since } = req.query;
    if (!accountKey) {
      return res.status(400).json({ success: false, error: 'accountKey obrigatório' });
    }
    const sinceTime = parseInt(since || '0', 10);
    const rows = (await db.query(
      'SELECT sync_type, item_id, data_json, updated_at FROM account_sync WHERE account_key = ? AND updated_at > ? ORDER BY updated_at ASC',
      [accountKey, sinceTime]
    )).rows;

    const items = rows.map(r => {
      let data = r.data_json;
      try { data = JSON.parse(r.data_json); } catch (_) {}
      return {
        syncType: r.sync_type,
        itemId: r.item_id,
        data,
        updatedAt: Number(r.updated_at)
      };
    });

    res.json({ success: true, items });
  } catch (err) {
    console.error('[Sync Progress Get Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 3. Confirmação de exibição de mensagem (Acknowledge)
router.post('/message/ack', async (req, res) => {
  try {
    const { deviceId, messageId } = req.body;
    if (!deviceId) {
      return res.status(400).json({ success: false, error: 'deviceId obrigatório' });
    }
    await db.query('UPDATE devices SET message = NULL, message_id = NULL WHERE id = ?', [deviceId]);
    res.json({ success: true });
  } catch (err) {
    console.error('[Device Ack Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

// 4. Verificação de atualização OTA
router.get('/ota/check', async (req, res) => {
  try {
    const currentCode = parseInt(req.query.versionCode || '0', 10);
    const ota = await getLatestOta(currentCode);
    res.json({ success: true, ota });
  } catch (err) {
    console.error('[OTA Check Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;

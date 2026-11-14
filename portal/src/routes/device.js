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

// 1. Registro inicial do dispositivo
router.post('/register', async (req, res) => {
  try {
    const { deviceId, mac, model, appVersion, versionCode } = req.body;
    if (!deviceId) {
      return res.status(400).json({ success: false, error: 'deviceId obrigatório' });
    }

    const ip = getClientIp(req);
    const now = Date.now();
    const vCode = parseInt(versionCode || '0', 10);

    const existing = await db.query('SELECT * FROM devices WHERE id = ?', [deviceId]);
    if (existing.rows.length === 0) {
      // Novo dispositivo
      await db.query(
        `INSERT INTO devices (id, mac, name, model, app_version, ip, status, created_at, last_seen)
         VALUES (?, ?, ?, ?, ?, ?, 'active', ?, ?)`,
        [deviceId, mac || '', model || 'Dispositivo Desconhecido', model || 'Dispositivo Desconhecido', appVersion || '', ip, now, now]
      );
      const dev = (await db.query('SELECT * FROM devices WHERE id = ?', [deviceId])).rows[0];
      return res.json(await buildDeviceResponse(dev, vCode));
    } else {
      // Dispositivo existente -> atualiza último acesso e dados
      await db.query(
        `UPDATE devices SET model = ?, app_version = ?, ip = ?, last_seen = ? WHERE id = ?`,
        [model || existing.rows[0].model, appVersion || existing.rows[0].app_version, ip, now, deviceId]
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
    const { deviceId, versionCode } = req.body;
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

    await db.query('UPDATE devices SET ip = ?, last_seen = ? WHERE id = ?', [ip, now, deviceId]);
    const dev = (await db.query('SELECT * FROM devices WHERE id = ?', [deviceId])).rows[0];
    return res.json(await buildDeviceResponse(dev, vCode));
  } catch (err) {
    console.error('[Device Heartbeat Error]', err);
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

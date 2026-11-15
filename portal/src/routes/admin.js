const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const multer = require('multer');
const AppInfoParser = require('app-info-parser');
const db = require('../db');
const config = require('../config');
const { checkPlaylistValidity } = require('../services/playlistChecker');
const { getSettings, saveSettings, migrateAllClientsUrl, runExpiryAutomations } = require('../services/automationService');
const { sendWhatsAppMessage } = require('../services/whatsappService');

// Sessões ativas em memória e tokens assinados
const activeSessions = new Map();

function generateToken(username = 'admin') {
  const expiresAt = Date.now() + (30 * 24 * 60 * 60 * 1000); // 30 dias
  const data = `${username}:${expiresAt}`;
  const sig = crypto.createHmac('sha256', config.sessionSecret || 'ipvio-stream-secret-key').update(data).digest('hex');
  return `${Buffer.from(data).toString('base64url')}.${sig}`;
}

function verifyToken(token) {
  if (!token) return null;
  if (config.adminKey && token === config.adminKey) {
    return { username: 'admin' };
  }

  // 1. Tenta validar token assinado (resiste a reinicializações)
  try {
    const parts = token.split('.');
    if (parts.length === 2) {
      const [payloadB64, sig] = parts;
      const data = Buffer.from(payloadB64, 'base64url').toString('utf8');
      const expectedSig = crypto.createHmac('sha256', config.sessionSecret || 'ipvio-stream-secret-key').update(data).digest('hex');
      if (crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(expectedSig))) {
        const [username, expStr] = data.split(':');
        const expiresAt = parseInt(expStr, 10);
        if (expiresAt > Date.now()) {
          return { username, expiresAt };
        }
      }
    }
  } catch (_) {}

  // 2. Fallback para mapa em memória
  const session = activeSessions.get(token);
  if (session && session.expiresAt > Date.now()) {
    return session;
  }

  return null;
}

// Configuração do multer para upload de APKs
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    if (!fs.existsSync(config.apkDir)) {
      fs.mkdirSync(config.apkDir, { recursive: true });
    }
    cb(null, config.apkDir);
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || '.apk';
    const base = path.basename(file.originalname, ext).replace(/[^a-zA-Z0-9_-]/g, '_');
    cb(null, `${base}_${Date.now()}${ext}`);
  }
});
const upload = multer({
  storage,
  limits: { fileSize: 150 * 1024 * 1024 } // Até 150MB
});

// 1. Rota de Login (Pública)
router.post('/login', (req, res) => {
  const { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ success: false, error: 'Usuário e senha são obrigatórios.' });
  }

  const validUser = (username === config.adminUser);
  const validPass = (password === config.adminPass);

  if (validUser && validPass) {
    const token = generateToken(username);
    const expiresAt = Date.now() + (30 * 24 * 60 * 60 * 1000);
    activeSessions.set(token, { username, expiresAt });
    return res.json({ success: true, token, username, expiresAt });
  }

  return res.status(401).json({ success: false, error: 'Usuário ou senha incorretos.' });
});

// Middleware de autenticação para as rotas abaixo
function authMiddleware(req, res, next) {
  const token = req.headers['x-admin-token'] || 
                req.headers['x-admin-key'] || 
                req.query.token || 
                (req.headers['authorization']?.replace(/^Bearer\s+/i, ''));

  if (!token) {
    return res.status(401).json({ success: false, error: 'Acesso não autorizado. Faça login.' });
  }

  const session = verifyToken(token);
  if (session) {
    req.user = session.username;
    return next();
  }

  return res.status(401).json({ success: false, error: 'Sessão expirada. Faça login novamente.' });
}

router.use(authMiddleware);

// 2. Listar dispositivos
router.get('/devices', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM devices ORDER BY last_seen DESC');
    const now = Date.now();
    const devices = result.rows.map(d => {
      let code = d.pairing_code;
      if (!code) {
        const src = (d.mac || '').replace(/[^a-zA-Z0-9]/g, '') || (d.id || '').replace(/[^a-zA-Z0-9]/g, '');
        const hex = src.slice(-6).toUpperCase();
        code = hex.length === 6 ? `${hex.slice(0, 3)}-${hex.slice(3)}` : (hex || '-');
      }
      const isOnline = (now - Number(d.last_seen || 0)) < 3 * 60 * 1000;
      const isPlaying = Boolean(isOnline && d.now_playing && (now - Number(d.now_playing_at || 0)) < 6 * 60 * 1000);
      return {
        ...d,
        pairing_code: code,
        isOnline,
        isPlaying,
        currentPlaying: isPlaying ? d.now_playing : null,
        currentPlayingType: isPlaying ? d.now_playing_type : null
      };
    });
    res.json({ success: true, devices });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 3. Detalhes de um dispositivo
router.get('/devices/:id', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM devices WHERE id = ?', [req.params.id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'Dispositivo não encontrado' });
    }
    res.json({ success: true, device: result.rows[0] });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 4. Atualizar dados básicos, telefone ou status do dispositivo
router.put('/devices/:id', async (req, res) => {
  try {
    const { name, status, clientPhone, clientName, notes, phone } = req.body;
    const finalPhone = clientPhone !== undefined ? clientPhone : (phone !== undefined ? phone : null);
    const finalName = clientName !== undefined ? clientName : (name !== undefined ? name : null);

    await db.query(
      `UPDATE devices SET 
        name = COALESCE(?, name), 
        status = COALESCE(?, status), 
        client_phone = COALESCE(?, client_phone),
        client_name = COALESCE(?, client_name),
        notes = COALESCE(?, notes)
       WHERE id = ?`,
      [
        name !== undefined ? name : null,
        status !== undefined ? status : null,
        finalPhone,
        finalName,
        notes !== undefined ? notes : null,
        req.params.id
      ]
    );
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 4.1 Configurações globais e automações
router.get('/settings', async (req, res) => {
  try {
    const settings = await getSettings();
    res.json({ success: true, settings });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/settings', async (req, res) => {
  try {
    const updated = await saveSettings(req.body);
    res.json({ success: true, settings: updated });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 4.2 Migração em massa da URL/Host IPTV para TODOS os clientes conectados
router.post('/iptv/migrate-url', async (req, res) => {
  try {
    const { newUrl } = req.body;
    if (!newUrl) {
      return res.status(400).json({ success: false, error: 'Nova URL do IPTV é obrigatória' });
    }
    const result = await migrateAllClientsUrl(newUrl);
    res.json(result);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 4.3 Teste de envio de WhatsApp oficial
router.post('/whatsapp/test', async (req, res) => {
  try {
    const { apiUrl, apiToken, phone, message } = req.body;
    const settings = await getSettings();
    const finalUrl = apiUrl || settings.whatsapp_api_url;
    const finalToken = apiToken || settings.whatsapp_api_token;

    const result = await sendWhatsAppMessage({
      apiUrl: finalUrl,
      apiToken: finalToken,
      phone,
      message: message || '✅ Teste de conexão com a API do WhatsApp via Portal IPVIO Stream!'
    });
    res.json(result);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 4.4 Disparo manual/imediato das automações de vencimento
router.post('/automations/run-expiry', async (req, res) => {
  try {
    const result = await runExpiryAutomations();
    res.json(result);
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 5. Atribuir ou atualizar playlist remotamente (com checagem de validade)
router.post('/devices/link-by-code', async (req, res) => {
  try {
    const { code, type, url, host, user, pass, phone, name, notes } = req.body;
    if (!code) {
      return res.status(400).json({ success: false, error: 'Código de pareamento ou MAC é obrigatório.' });
    }

    const cleanInput = code.trim().toUpperCase().replace(/[^A-Z0-9]/g, '');

    const result = await db.query(
      `SELECT * FROM devices 
       WHERE REPLACE(UPPER(COALESCE(pairing_code, '')), '-', '') = ?
          OR REPLACE(UPPER(COALESCE(mac, '')), ':', '') LIKE ?
          OR UPPER(id) LIKE ?
       ORDER BY last_seen DESC LIMIT 1`,
      [cleanInput, `%${cleanInput}`, `%${cleanInput}`]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, error: `Nenhum aparelho encontrado com o código "${code}". Certifique-se de que o aplicativo está aberto.` });
    }

    const dev = result.rows[0];
    const now = Date.now();

    const settings = await getSettings();
    const effectiveHost = (host && host.trim()) || (type === 'xtream' ? (settings.default_iptv_url || '') : '');

    let expDate = null;
    let plStatus = 'Active';
    let maxCons = 1;

    if (type !== 'none') {
      const checkRes = await checkPlaylistValidity({ type, url, host: effectiveHost, user, pass });
      if (checkRes.success) {
        expDate = checkRes.expDateFormatted;
        plStatus = checkRes.status;
        maxCons = checkRes.maxConnections || 1;
      } else {
        expDate = 'Erro ao verificar';
      }
    }

    await db.query(
      `UPDATE devices SET 
        name = COALESCE(?, name),
        client_name = COALESCE(?, client_name),
        playlist_type = ?, 
        playlist_url = ?, 
        xtream_host = ?, 
        xtream_user = ?, 
        xtream_pass = ?, 
        playlist_updated_at = ?,
        playlist_exp_date = ?,
        playlist_status = ?,
        playlist_max_cons = ?,
        client_phone = COALESCE(?, client_phone),
        notes = COALESCE(?, notes)
       WHERE id = ?`,
      [name || null, name || null, type || 'none', url || '', effectiveHost || '', user || '', pass || '', now, expDate, plStatus, maxCons, phone || null, notes || null, dev.id]
    );

    res.json({
      success: true,
      deviceId: dev.id,
      deviceName: dev.name,
      pairingCode: dev.pairing_code,
      updatedAt: now,
      validity: { expDate, status: plStatus, maxCons }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/devices/:id/playlist', async (req, res) => {
  try {
    const { type, url, host, user, pass, name, clientName, clientPhone, phone, notes } = req.body;
    const now = Date.now();

    const settings = await getSettings();
    const effectiveHost = (host && host.trim()) || (type === 'xtream' ? (settings.default_iptv_url || '') : '');

    let expDate = null;
    let plStatus = 'Active';
    let maxCons = 1;

    // Consulta validade da lista no servidor do provedor
    if (type !== 'none') {
      const checkRes = await checkPlaylistValidity({ type, url, host: effectiveHost, user, pass });
      if (checkRes.success) {
        expDate = checkRes.expDateFormatted;
        plStatus = checkRes.status;
        maxCons = checkRes.maxConnections || 1;
      } else {
        expDate = 'Erro ao verificar';
      }
    }

    const finalPhone = clientPhone !== undefined ? clientPhone : (phone !== undefined ? phone : null);
    const finalName = clientName !== undefined ? clientName : (name !== undefined ? name : null);

    await db.query(
      `UPDATE devices SET 
        name = COALESCE(?, name),
        client_name = COALESCE(?, client_name),
        client_phone = COALESCE(?, client_phone),
        notes = COALESCE(?, notes),
        playlist_type = ?, 
        playlist_url = ?, 
        xtream_host = ?, 
        xtream_user = ?, 
        xtream_pass = ?, 
        playlist_updated_at = ?,
        playlist_exp_date = ?,
        playlist_status = ?,
        playlist_max_cons = ?
       WHERE id = ?`,
      [
        finalName,
        finalName,
        finalPhone,
        notes !== undefined ? notes : null,
        type || 'none',
        url || '',
        effectiveHost || '',
        user || '',
        pass || '',
        now,
        expDate,
        plStatus,
        maxCons,
        req.params.id
      ]
    );

    res.json({
      success: true,
      updatedAt: now,
      validity: {
        expDate,
        status: plStatus,
        maxCons
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 6. Consultar / Atualizar validade da playlist sob demanda
router.post('/devices/:id/check-validity', async (req, res) => {
  try {
    const devRes = await db.query('SELECT * FROM devices WHERE id = ?', [req.params.id]);
    if (devRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'Dispositivo não encontrado' });
    }
    const dev = devRes.rows[0];
    if (!dev.playlist_type || dev.playlist_type === 'none') {
      return res.status(400).json({ success: false, error: 'Dispositivo não tem playlist atribuída' });
    }

    const settings = await getSettings();
    const effectiveHost = dev.xtream_host || (dev.playlist_type === 'xtream' ? (settings.default_iptv_url || '') : '');

    const checkRes = await checkPlaylistValidity({
      type: dev.playlist_type,
      url: dev.playlist_url,
      host: effectiveHost,
      user: dev.xtream_user,
      pass: dev.xtream_pass
    });

    if (checkRes.success) {
      await db.query(
        'UPDATE devices SET xtream_host = COALESCE(NULLIF(xtream_host, ""), ?), playlist_exp_date = ?, playlist_status = ?, playlist_max_cons = ? WHERE id = ?',
        [effectiveHost, checkRes.expDateFormatted, checkRes.status, checkRes.maxConnections || 1, req.params.id]
      );
      return res.json({
        success: true,
        expDate: checkRes.expDateFormatted,
        daysRemaining: checkRes.daysRemaining,
        isExpired: checkRes.isExpired,
        status: checkRes.status,
        maxCons: checkRes.maxConnections
      });
    } else {
      return res.status(400).json({ success: false, error: checkRes.error });
    }
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 7. Enviar mensagem push para tela do dispositivo
router.post('/devices/:id/message', async (req, res) => {
  try {
    const { message, type } = req.body;
    const msgId = 'msg_' + Date.now();
    await db.query(
      'UPDATE devices SET message = ?, message_id = ?, message_type = ? WHERE id = ?',
      [message || null, message ? msgId : null, type || 'info', req.params.id]
    );
    res.json({ success: true, messageId: msgId });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 8. Enviar mensagem em massa (Broadcast)
router.post('/devices/broadcast-message', async (req, res) => {
  try {
    const { message, type } = req.body;
    const msgId = 'broadcast_' + Date.now();
    await db.query(
      'UPDATE devices SET message = ?, message_id = ?, message_type = ? WHERE status = ?',
      [message, msgId, type || 'info', 'active']
    );
    res.json({ success: true, messageId: msgId });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 9. Remover dispositivo
router.delete('/devices/:id', async (req, res) => {
  try {
    await db.query('DELETE FROM devices WHERE id = ?', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 10. Obter configurações de OTA
router.get('/ota', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM ota_updates ORDER BY version_code DESC');
    res.json({ success: true, updates: result.rows });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 11. Registrar nova versão OTA manualmente
router.post('/ota', async (req, res) => {
  try {
    const { versionName, versionCode, downloadUrl, changelog, mandatory } = req.body;
    if (!versionName || !versionCode || !downloadUrl) {
      return res.status(400).json({ success: false, error: 'versionName, versionCode e downloadUrl são obrigatórios' });
    }
    const now = Date.now();
    await db.query(
      `INSERT INTO ota_updates (version_name, version_code, download_url, changelog, mandatory, released_at)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [versionName, parseInt(versionCode, 10), downloadUrl, changelog || '', mandatory ? 1 : 0, now]
    );
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 12. Upload direto do APK para servir via OTA (com extração automática de versão)
router.post('/ota/upload', upload.single('apk'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, error: 'Nenhum arquivo APK enviado.' });
    }
    const { changelog, mandatory } = req.body;
    let finalVersionName = (req.body.versionName || '').trim();
    let finalVersionCode = req.body.versionCode ? parseInt(req.body.versionCode, 10) : null;

    // Extrai automaticamente versionCode e versionName do binário do APK
    try {
      const parser = new AppInfoParser(req.file.path);
      const parsed = await parser.parse();
      if (parsed) {
        if (!finalVersionName && parsed.versionName) finalVersionName = String(parsed.versionName);
        if (!finalVersionCode && parsed.versionCode) finalVersionCode = parseInt(parsed.versionCode, 10);
      }
    } catch (parseErr) {
      console.warn('[APK Parser Auto-detect Warning]', parseErr.message);
    }

    finalVersionName = finalVersionName || '1.0.0';
    finalVersionCode = finalVersionCode || 1;

    // Constrói URL relativa para ser resolvida dinamicamente com o domínio público configurado
    const downloadUrl = `/apks/${req.file.filename}`;
    const now = Date.now();

    await db.query(
      `INSERT INTO ota_updates (version_name, version_code, download_url, changelog, mandatory, released_at)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [
        finalVersionName,
        finalVersionCode,
        downloadUrl,
        changelog || '',
        mandatory ? 1 : 0,
        now
      ]
    );

    res.json({
      success: true,
      downloadUrl,
      filename: req.file.filename,
      versionName: finalVersionName,
      versionCode: finalVersionCode
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 13. Excluir versão OTA e arquivo físico se existir
router.delete('/ota/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const findRes = await db.query('SELECT * FROM ota_updates WHERE id = ?', [id]);
    if (findRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'Versão OTA não encontrada.' });
    }
    const update = findRes.rows[0];

    // Se o downloadUrl for um APK local dentro de /apks/, remove o arquivo físico
    if (update.download_url) {
      try {
        const urlMatch = update.download_url.match(/\/apks\/([^/?#]+)/);
        if (urlMatch && urlMatch[1]) {
          const filePath = path.join(config.apkDir, urlMatch[1]);
          if (fs.existsSync(filePath)) {
            fs.unlinkSync(filePath);
          }
        }
      } catch (fErr) {
        console.warn('[Delete OTA File Warning]', fErr.message);
      }
    }

    await db.query('DELETE FROM ota_updates WHERE id = ?', [id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 14. Listar solicitações de teste geradas
router.get('/tests', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM test_requests ORDER BY created_at DESC LIMIT 150');
    res.json({ success: true, tests: result.rows });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 15. Excluir registro de teste
router.delete('/tests/:id', async (req, res) => {
  try {
    await db.query('DELETE FROM test_requests WHERE id = ?', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;

const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const multer = require('multer');
const db = require('../db');
const config = require('../config');
const { checkPlaylistValidity } = require('../services/playlistChecker');

// Sessões ativas em memória (token -> { username, expiresAt })
const activeSessions = new Map();

function generateToken() {
  return crypto.randomBytes(32).toString('hex');
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
    const token = generateToken();
    const expiresAt = Date.now() + (7 * 24 * 60 * 60 * 1000); // 7 dias
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

  // Permite acesso se for o token da sessão ou a chave mestra
  const session = activeSessions.get(token);
  if (session && session.expiresAt > Date.now()) {
    req.user = session.username;
    return next();
  }

  if (config.adminKey && token === config.adminKey) {
    req.user = 'admin';
    return next();
  }

  return res.status(401).json({ success: false, error: 'Sessão expirada ou chave inválida. Faça login novamente.' });
}

router.use(authMiddleware);

// 2. Listar dispositivos
router.get('/devices', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM devices ORDER BY last_seen DESC');
    const now = Date.now();
    const devices = result.rows.map(d => ({
      ...d,
      isOnline: (now - Number(d.last_seen || 0)) < 2 * 60 * 1000 // visto nos últimos 2 min
    }));
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

// 4. Atualizar dados básicos / status do dispositivo
router.put('/devices/:id', async (req, res) => {
  try {
    const { name, status } = req.body;
    await db.query(
      'UPDATE devices SET name = COALESCE(?, name), status = COALESCE(?, status) WHERE id = ?',
      [name, status, req.params.id]
    );
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// 5. Atribuir ou atualizar playlist remotamente (com checagem de validade)
router.post('/devices/:id/playlist', async (req, res) => {
  try {
    const { type, url, host, user, pass } = req.body;
    const now = Date.now();

    let expDate = null;
    let plStatus = 'Active';
    let maxCons = 1;

    // Consulta validade da lista no servidor do provedor
    if (type !== 'none') {
      const checkRes = await checkPlaylistValidity({ type, url, host, user, pass });
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
      [type || 'none', url || '', host || '', user || '', pass || '', now, expDate, plStatus, maxCons, req.params.id]
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

    const checkRes = await checkPlaylistValidity({
      type: dev.playlist_type,
      url: dev.playlist_url,
      host: dev.xtream_host,
      user: dev.xtream_user,
      pass: dev.xtream_pass
    });

    if (checkRes.success) {
      await db.query(
        'UPDATE devices SET playlist_exp_date = ?, playlist_status = ?, playlist_max_cons = ? WHERE id = ?',
        [checkRes.expDateFormatted, checkRes.status, checkRes.maxConnections || 1, req.params.id]
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

// 12. Upload direto do APK para servir via OTA
router.post('/ota/upload', upload.single('apk'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, error: 'Nenhum arquivo APK enviado.' });
    }
    const { versionName, versionCode, changelog, mandatory } = req.body;
    
    // Constrói URL absoluta com o domínio do portal para a TV conseguir baixar
    const protocol = req.protocol || 'http';
    const host = req.get('host') || `localhost:${config.port}`;
    const downloadUrl = `${protocol}://${host}/apks/${req.file.filename}`;
    const now = Date.now();
    
    await db.query(
      `INSERT INTO ota_updates (version_name, version_code, download_url, changelog, mandatory, released_at)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [
        versionName || 'Versão Atualizada',
        parseInt(versionCode || '8', 10),
        downloadUrl,
        changelog || '',
        mandatory ? 1 : 0,
        now
      ]
    );
    res.json({ success: true, downloadUrl, filename: req.file.filename });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;

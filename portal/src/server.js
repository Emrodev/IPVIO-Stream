const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const path = require('path');
const fs = require('fs');
const config = require('./config');
const db = require('./db');

const deviceRoutes = require('./routes/device');
const adminRoutes = require('./routes/admin');
const publicRoutes = require('./routes/public');

const { runExpiryAutomations } = require('./services/automationService');

const app = express();

// Confia no primeiro proxy reverso (Cloudflare / Nginx / Traefik) para obter o IP real
app.set('trust proxy', 1);

// Proteções de cabeçalhos HTTP
app.use(helmet({
  contentSecurityPolicy: false,
  crossOriginEmbedderPolicy: false
}));
app.disable('x-powered-by');

app.use(cors());
app.use(express.json({ limit: '2mb' }));
app.use(express.urlencoded({ extended: true, limit: '2mb' }));

// Garante que as pastas de uploads/apks existam
if (!fs.existsSync(config.apkDir)) {
  fs.mkdirSync(config.apkDir, { recursive: true });
}

// Arquivos estáticos (painel web e downloads de APK)
app.use(express.static(path.join(__dirname, '..', 'public')));

// Página pública de solicitação de teste
app.get(['/teste', '/test', '/solicitar-teste'], (req, res) => {
  res.sendFile(path.join(__dirname, '..', 'public', 'teste.html'));
});

// Link permanente para primeira instalação (sempre serve o APK mais recente)
app.get(['/app.apk', '/download', '/baixar', '/instalador.apk', '/instalar'], async (req, res) => {
  try {
    const latest = await db.query('SELECT * FROM ota_updates ORDER BY version_code DESC, released_at DESC LIMIT 1');
    if (latest.rows.length > 0) {
      const update = latest.rows[0];
      const urlMatch = (update.download_url || '').match(/\/apks\/([^/?#]+)/);
      if (urlMatch && urlMatch[1]) {
        const filePath = path.join(config.apkDir, urlMatch[1]);
        if (fs.existsSync(filePath)) {
          return res.download(filePath, `Korp-TV-v${update.version_name || 'latest'}.apk`);
        }
      }
      if (update.download_url) {
        return res.redirect(update.download_url);
      }
    }

    // Fallback 1: arquivo APK mais recente na pasta public/apks/
    if (fs.existsSync(config.apkDir)) {
      const files = fs.readdirSync(config.apkDir).filter(f => f.endsWith('.apk'));
      if (files.length > 0) {
        const newest = files
          .map(f => ({ name: f, time: fs.statSync(path.join(config.apkDir, f)).mtimeMs }))
          .sort((a, b) => b.time - a.time)[0];
        return res.download(path.join(config.apkDir, newest.name), 'Korp-TV.apk');
      }
    }

    // Fallback 2: APK gerado na raiz do projeto (se disponível)
    const rootApk = path.join(__dirname, '..', '..', 'IPVIO-Stream-ptBR.apk');
    if (fs.existsSync(rootApk)) {
      return res.download(rootApk, 'Korp-TV.apk');
    }

    res.status(404).send('Nenhum APK publicado no momento. Faça o upload pelo painel do portal.');
  } catch (err) {
    res.status(500).send('Erro ao buscar APK: ' + err.message);
  }
});

// Rotas da API
app.use('/api/device', deviceRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/public', publicRoutes);

// Informações básicas da API
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    app: 'Korp TV Management Portal',
    db: config.dbType,
    time: new Date().toISOString()
  });
});

async function start() {
  try {
    await db.init();
    app.listen(config.port, '0.0.0.0', () => {
      console.log(`====================================================`);
      console.log(`🚀 Korp TV Portal rodando em http://localhost:${config.port}`);
      console.log(`📡 Gerenciamento de dispositivos e OTA ativo`);
      console.log(`💾 Banco de dados: ${config.dbType.toUpperCase()}`);
      console.log(`🔑 Chave de Admin: ${config.adminKey}`);
      console.log(`====================================================`);

      // Verificação periódica de renovações (a cada 1 hora)
      setInterval(() => {
        runExpiryAutomations().catch(err => console.error('[Automação Vencimento]', err.message));
      }, 60 * 60 * 1000);
    });
  } catch (err) {
    console.error('Falha ao iniciar o portal:', err);
    process.exit(1);
  }
}

if (require.main === module) {
  start();
}

module.exports = { app, start };

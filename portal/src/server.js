const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const config = require('./config');
const db = require('./db');

const deviceRoutes = require('./routes/device');
const adminRoutes = require('./routes/admin');

const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Garante que as pastas de uploads/apks existam
if (!fs.existsSync(config.apkDir)) {
  fs.mkdirSync(config.apkDir, { recursive: true });
}

// Arquivos estáticos (painel web e downloads de APK)
app.use(express.static(path.join(__dirname, '..', 'public')));

// Rotas da API
app.use('/api/device', deviceRoutes);
app.use('/api/admin', adminRoutes);

// Informações básicas da API
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    app: 'IPVIO Stream Management Portal',
    db: config.dbType,
    time: new Date().toISOString()
  });
});

async function start() {
  try {
    await db.init();
    app.listen(config.port, '0.0.0.0', () => {
      console.log(`====================================================`);
      console.log(`🚀 IPVIO Portal rodando em http://localhost:${config.port}`);
      console.log(`📡 Gerenciamento de dispositivos e OTA ativo`);
      console.log(`💾 Banco de dados: ${config.dbType.toUpperCase()}`);
      console.log(`🔑 Chave de Admin: ${config.adminKey}`);
      console.log(`====================================================`);
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

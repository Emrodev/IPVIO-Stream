const express = require('express');
const cors = require('cors');
const path = require('path');
const http = require('http');
const https = require('https');

const app = express();
const PORT = parseInt(process.env.PORT || '3001', 10);
const PORTAL_URL = process.env.PORTAL_URL || 'http://localhost:3000';
const PUBLIC_PORTAL_URL = process.env.PUBLIC_PORTAL_URL || process.env.PORTAL_PUBLIC_DOMAIN || 'https://korp.kabaweb.in';

app.use(cors());
app.use(express.json());

// Arquivos estáticos da interface Smart TV
app.use(express.static(path.join(__dirname, '..', 'public')));

// Configurações básicas para a interface
app.get('/api/config', (req, res) => {
  res.json({
    portalUrl: PUBLIC_PORTAL_URL,
    localPortalUrl: PORTAL_URL,
    appName: 'Korp TV',
    version: '1.0.0-web'
  });
});

// Proxy leve de streaming e APIs para contornar restrições de CORS em servidores de IPTV
app.all('/api/proxy', (req, res) => {
  const targetUrl = req.query.url;
  if (!targetUrl) {
    return res.status(400).json({ error: 'Parâmetro url é obrigatório' });
  }

  try {
    const parsed = new URL(targetUrl);
    const client = parsed.protocol === 'https:' ? https : http;

    const options = {
      method: req.method,
      headers: {
        'User-Agent': 'IPTVSmarters/1.0.0 (Linux; Android TV 12)',
        'Accept': '*/*'
      }
    };

    if (req.headers['range']) {
      options.headers['range'] = req.headers['range'];
    }

    const proxyReq = client.request(parsed, options, (proxyRes) => {
      res.setHeader('Access-Control-Allow-Origin', '*');
      res.setHeader('Access-Control-Allow-Headers', '*');
      res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');

      // Propaga cabeçalhos importantes de vídeo
      if (proxyRes.headers['content-type']) res.setHeader('Content-Type', proxyRes.headers['content-type']);
      if (proxyRes.headers['content-length']) res.setHeader('Content-Length', proxyRes.headers['content-length']);
      if (proxyRes.headers['content-range']) res.setHeader('Content-Range', proxyRes.headers['content-range']);
      if (proxyRes.headers['accept-ranges']) res.setHeader('Accept-Ranges', proxyRes.headers['accept-ranges']);

      res.status(proxyRes.statusCode);
      proxyRes.pipe(res);
    });

    proxyReq.on('error', (err) => {
      res.status(502).json({ error: 'Falha ao conectar no servidor de IPTV: ' + err.message });
    });

    req.pipe(proxyReq);
  } catch (err) {
    res.status(400).json({ error: 'URL inválida: ' + err.message });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`====================================================`);
  console.log(`📺 Korp TV Web & Smart TV App rodando na porta ${PORT}`);
  console.log(`🌐 Acesse em: http://localhost:${PORT}`);
  console.log(`📡 Conectado ao Portal: ${PUBLIC_PORTAL_URL}`);
  console.log(`====================================================`);
});

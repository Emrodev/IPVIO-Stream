const express = require('express');
const cors = require('cors');
const path = require('path');
const http = require('http');
const https = require('https');

const app = express();
const PORT = parseInt(process.env.PORT || '3001', 10);
const PORTAL_URL = process.env.PORTAL_URL || 'http://localhost:3000';
const PUBLIC_PORTAL_URL = process.env.PUBLIC_PORTAL_URL || process.env.PORTAL_PUBLIC_DOMAIN || 'https://korp.kabaweb.in';
const PUBLIC_WEBTV_URL = process.env.PUBLIC_WEBTV_URL || 'https://playkorp.kabaweb.in';

app.use(cors());
app.use(express.json());

// Arquivos estáticos da interface Smart TV
app.use(express.static(path.join(__dirname, '..', 'public')));

// Configurações básicas para a interface
app.get('/api/config', (req, res) => {
  res.json({
    portalUrl: PUBLIC_PORTAL_URL,
    localPortalUrl: PORTAL_URL,
    webtvUrl: PUBLIC_WEBTV_URL,
    appName: 'Korp TV',
    version: '1.0.0-web'
  });
});

// Helper para requisições seguindo redirecionamentos (301, 302, 307, 308)
function requestWithRedirects(targetUrl, options, callback, maxRedirects = 5) {
  if (maxRedirects <= 0) {
    return callback(new Error('Muitos redirecionamentos'));
  }

  const parsed = new URL(targetUrl);
  const client = parsed.protocol === 'https:' ? https : http;

  const req = client.request(parsed, options, (res) => {
    // Seguir redirecionamento
    if ([301, 302, 303, 307, 308].includes(res.statusCode) && res.headers.location) {
      const redirectUrl = new URL(res.headers.location, targetUrl).href;
      return requestWithRedirects(redirectUrl, options, callback, maxRedirects - 1);
    }
    callback(null, res);
  });

  return req;
}

// Proxy de dados e streaming de vídeo com suporte a Range e Redirecionamentos
app.all('/api/proxy', (req, res) => {
  const targetUrl = req.query.url;
  if (!targetUrl) {
    return res.status(400).json({ error: 'Parâmetro url é obrigatório' });
  }

  try {
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

    const proxyReq = requestWithRedirects(targetUrl, options, (err, proxyRes) => {
      if (err) {
        return res.status(502).json({ error: 'Falha no servidor de IPTV: ' + err.message });
      }

      res.setHeader('Access-Control-Allow-Origin', '*');
      res.setHeader('Access-Control-Allow-Headers', '*');
      res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');

      if (proxyRes.headers['content-type']) res.setHeader('Content-Type', proxyRes.headers['content-type']);
      if (proxyRes.headers['content-length']) res.setHeader('Content-Length', proxyRes.headers['content-length']);
      if (proxyRes.headers['content-range']) res.setHeader('Content-Range', proxyRes.headers['content-range']);
      if (proxyRes.headers['accept-ranges']) res.setHeader('Accept-Ranges', proxyRes.headers['accept-ranges']);

      res.status(proxyRes.statusCode);
      proxyRes.pipe(res);
    });

    proxyReq.on('error', (err) => {
      if (!res.headersSent) {
        res.status(502).json({ error: 'Erro de conexão: ' + err.message });
      }
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

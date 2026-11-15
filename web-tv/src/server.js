const express = require('express');
const cors = require('cors');
const path = require('path');
const { Readable } = require('stream');

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

// Proxy Universal com suporte a HLS Rewrite, MPEG-TS, Range e Redirecionamentos
app.all('/api/proxy', async (req, res) => {
  const targetUrl = req.query.url;
  if (!targetUrl) {
    return res.status(400).json({ error: 'Parâmetro url é obrigatório' });
  }

  const controller = new AbortController();
  req.on('close', () => controller.abort());

  try {
    const headers = {
      'User-Agent': 'IPTVSmarters/1.0.0 (Linux; Android TV 12)',
      'Accept': '*/*'
    };

    if (req.headers['range']) {
      headers['Range'] = req.headers['range'];
    }

    const response = await fetch(targetUrl, {
      method: req.method === 'OPTIONS' ? 'GET' : req.method,
      headers,
      redirect: 'follow',
      signal: controller.signal
    });

    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');

    const contentType = response.headers.get('content-type') || '';
    const contentLength = response.headers.get('content-length');
    const contentRange = response.headers.get('content-range');
    const acceptRanges = response.headers.get('accept-ranges');

    if (contentType) res.setHeader('Content-Type', contentType);
    if (contentLength) res.setHeader('Content-Length', contentLength);
    if (contentRange) res.setHeader('Content-Range', contentRange);
    if (acceptRanges) res.setHeader('Accept-Ranges', acceptRanges);

    // Se for manifesto HLS (.m3u8), reescreve cada linha para passar pelo proxy seguro
    // Isso evita Mixed Content (HTTPS -> HTTP) e bloqueios de CORS nos chunks de vídeo (.ts)
    const isHls = contentType.includes('vnd.apple.mpegurl') ||
                  contentType.includes('x-mpegurl') ||
                  targetUrl.toLowerCase().includes('.m3u8');

    if (isHls) {
      const text = await response.text();
      const finalBaseUrl = response.url || targetUrl;
      const modified = text.split('\n').map(line => {
        const trimmed = line.trim();
        if (!trimmed) return line;
        if (trimmed.startsWith('#')) {
          // Trata URI dentro de tags como #EXT-X-KEY:METHOD=...,URI="http..."
          if (trimmed.includes('URI="')) {
            return trimmed.replace(/URI="([^"]+)"/, (match, uri) => {
              const fullKeyUrl = new URL(uri, finalBaseUrl).href;
              return `URI="/api/proxy?url=${encodeURIComponent(fullKeyUrl)}"`;
            });
          }
          return line;
        }
        // Segmento de vídeo
        const segmentAbsoluteUrl = new URL(trimmed, finalBaseUrl).href;
        return `/api/proxy?url=${encodeURIComponent(segmentAbsoluteUrl)}`;
      }).join('\n');

      res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
      return res.status(response.status).send(modified);
    }

    res.status(response.status);
    if (response.body) {
      Readable.fromWeb(response.body).pipe(res);
    } else {
      res.end();
    }
  } catch (err) {
    if (err.name === 'AbortError') return;
    if (!res.headersSent) {
      res.status(502).json({ error: 'Erro no stream de vídeo: ' + err.message });
    }
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`====================================================`);
  console.log(`📺 Korp TV Web & Smart TV App rodando na porta ${PORT}`);
  console.log(`🌐 Acesse em: http://localhost:${PORT}`);
  console.log(`📡 Conectado ao Portal: ${PUBLIC_PORTAL_URL}`);
  console.log(`📡 Domínio WebTV: ${PUBLIC_WEBTV_URL}`);
  console.log(`====================================================`);
});

const express = require('express');
const router = express.Router();
const db = require('../db');
const { getSettings } = require('../services/automationService');
const { sendWhatsAppMessage, checkWhatsAppNumber } = require('../services/whatsappService');

function getClientIp(req) {
  const forwarded = req.headers['x-forwarded-for'];
  if (forwarded) return forwarded.split(',')[0].trim();
  return req.socket.remoteAddress || req.ip || '';
}

function normalizePhone(raw) {
  let cleaned = String(raw || '').replace(/\D/g, '');
  if (cleaned.length === 10 || cleaned.length === 11) {
    cleaned = '55' + cleaned;
  }
  return cleaned;
}

function resolveAppDownloadUrl(req, settings) {
  if (settings.test_app_url && settings.test_app_url.trim()) {
    return settings.test_app_url.trim();
  }
  const baseDomain = (settings.portal_public_domain || '').trim().replace(/\/+$/, '');
  if (baseDomain) {
    return `${baseDomain}/app.apk`;
  }
  const proto = req.headers['x-forwarded-proto'] || req.protocol || 'http';
  const host = req.headers['x-forwarded-host'] || req.get('host') || '';
  return `${proto}://${host}/app.apk`;
}

// Configurações públicas para a página de teste
router.get('/teste-info', async (req, res) => {
  try {
    const settings = await getSettings();
    const appUrl = resolveAppDownloadUrl(req, settings);

    res.json({
      success: true,
      hasAdultOption: Boolean(settings.test_api_with_adults && settings.test_api_with_adults.trim()),
      appUrl
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Solicitar Teste Grátis (Painel Slim)
router.post('/solicitar-teste', async (req, res) => {
  try {
    const { phone, name, type } = req.body;
    if (!phone) {
      return res.status(400).json({ success: false, error: 'Por favor, informe seu número de WhatsApp com DDD.' });
    }

    const cleanPhone = normalizePhone(phone);
    if (cleanPhone.length < 10) {
      return res.status(400).json({ success: false, error: 'Número de WhatsApp inválido. Digite DDD + Número.' });
    }

    const settings = await getSettings();
    const appUrl = resolveAppDownloadUrl(req, settings);

    // 1. Verificação oficial na API do WhatsApp antes de gerar o teste
    let waCheckResult = { isValid: true, lid: null };
    if (settings.whatsapp_api_url && settings.whatsapp_api_token) {
      waCheckResult = await checkWhatsAppNumber({
        apiUrl: settings.whatsapp_api_url,
        apiToken: settings.whatsapp_api_token,
        phone: cleanPhone
      });

      // Se a verificação atestou com certeza que não possui WhatsApp
      if (waCheckResult.isValid === false) {
        return res.status(400).json({
          success: false,
          error: 'O número informado não foi localizado no WhatsApp. Por favor, verifique o DDD e o número digitado.'
        });
      }
    }

    // Escolhe a URL da API da Painel Slim (com ou sem adultos)
    const isAdult = type === 'with_adults';
    let targetApiUrl = (settings.test_api_without_adults || process.env.TEST_API_WITHOUT_ADULTS || '').trim();
    if (isAdult && settings.test_api_with_adults && settings.test_api_with_adults.trim()) {
      targetApiUrl = settings.test_api_with_adults.trim();
    }

    if (!targetApiUrl) {
      return res.status(400).json({ success: false, error: 'Serviço de testes automáticos não configurado no portal.' });
    }

    console.log(`[Solicitação Teste] Telefone: ${cleanPhone} | Tipo: ${isAdult ? 'Com Adultos' : 'Sem Adultos'} | Alvo: ${targetApiUrl}`);

    // Dispara POST para a API do Painel Slim
    const postPayload = {
      phone: cleanPhone,
      number: cleanPhone,
      whatsapp: cleanPhone,
      name: (name || 'Cliente').trim()
    };

    let slimRes;
    try {
      slimRes = await fetch(targetApiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'User-Agent': 'IPVIO-Stream-Portal/1.0'
        },
        body: JSON.stringify(postPayload)
      });
    } catch (netErr) {
      console.error('[Erro Conexão Painel Slim]', netErr);
      return res.status(502).json({ success: false, error: 'Falha de comunicação com o servidor de testes. Tente novamente em instantes.' });
    }

    const rawText = await slimRes.text();
    let slimData = null;
    try {
      slimData = JSON.parse(rawText);
    } catch (_) {
      console.error('[Resposta Painel Slim não é JSON]:', rawText.slice(0, 300));
      return res.status(502).json({ success: false, error: 'Servidor de testes indisponível no momento. Tente novamente mais tarde.' });
    }

    if (!slimRes.ok || (!slimData.username && !slimData.data)) {
      const errMsg = slimData.message || slimData.error || 'Não foi possível gerar seu teste agora. Verifique se já não possui um teste ativo.';
      return res.status(400).json({ success: false, error: errMsg });
    }

    const username = slimData.username || (slimData.data && slimData.data[0]?.username) || '';
    const password = slimData.password || (slimData.data && slimData.data[0]?.password) || '';
    const dns = slimData.dns || 'http://poyla.lat';
    const packageName = slimData.package || (isAdult ? 'Com Adultos' : 'Sem Adultos');
    const expiresAt = slimData.expiresAtFormatted || slimData.expiresAt || '';
    const payUrl = slimData.payUrl || '';
    const rawReply = slimData.reply || (slimData.data && slimData.data[0]?.message) || '';
    const now = Date.now();
    const ip = getClientIp(req);

    // Registra a solicitação no banco de dados local com status do WhatsApp e LID
    try {
      await db.query(
        `INSERT INTO test_requests (phone, name, type, username, password, dns, package_name, expires_at_formatted, pay_url, raw_reply, ip, is_valid_wa, wa_lid, created_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          cleanPhone,
          name || 'Cliente',
          isAdult ? 'with_adults' : 'without_adults',
          username,
          password,
          dns,
          packageName,
          expiresAt,
          payUrl,
          rawReply,
          ip,
          waCheckResult.isValid ? 1 : 0,
          waCheckResult.lid || null,
          now
        ]
      );
    } catch (dbErr) {
      console.warn('[DB Test Request Log Warning]', dbErr.message);
    }

    // Envia WhatsApp automaticamente com os dados e o link de download do APK
    let whatsappSent = false;
    let waError = null;
    if (settings.test_auto_whatsapp && settings.whatsapp_api_url && settings.whatsapp_api_token) {
      let msg = settings.test_whatsapp_message || 'Olá {{nome}}! Seu teste do Korp TV foi liberado com sucesso 🎉\n\n📲 *Baixar Aplicativo:* {{app_url}}\n\n👤 *Usuário:* {{usuario}}\n🔑 *Senha:* {{senha}}\n📅 *Vencimento:* {{vencimento}}\n\nAbra o aplicativo e assista no seu celular, TV Box ou Smart TV!';
      msg = msg.replace(/{{nome}}/g, (name || 'Amigo(a)').trim());
      msg = msg.replace(/{{usuario}}/g, username);
      msg = msg.replace(/{{senha}}/g, password);
      msg = msg.replace(/{{vencimento}}/g, expiresAt);
      msg = msg.replace(/{{app_url}}/g, appUrl);
      msg = msg.replace(/{{dns}}/g, dns);

      const waRes = await sendWhatsAppMessage({
        apiUrl: settings.whatsapp_api_url,
        apiToken: settings.whatsapp_api_token,
        phone: cleanPhone,
        message: msg
      });
      whatsappSent = waRes.success;
      if (!waRes.success) waError = waRes.error;
    }

    return res.json({
      success: true,
      username,
      password,
      dns,
      expiresAt,
      packageName,
      appUrl,
      payUrl,
      whatsappSent,
      whatsappError: waError
    });
  } catch (err) {
    console.error('[Solicitar Teste Error]', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;

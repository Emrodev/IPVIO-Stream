const db = require('../db');
const { sendWhatsAppMessage } = require('./whatsappService');

// Carrega todas as configurações administrativas
async function getSettings() {
  const rows = (await db.query('SELECT key, value FROM admin_settings')).rows;
  const map = {};
  for (const r of rows) {
    map[r.key] = r.value;
  }
  return {
    default_iptv_url: map.default_iptv_url || '',
    whatsapp_api_url: map.whatsapp_api_url || '',
    whatsapp_api_token: map.whatsapp_api_token || '',
    auto_expiry_enabled: map.auto_expiry_enabled === '1',
    auto_expiry_days: parseInt(map.auto_expiry_days || '3', 10),
    auto_expiry_notify_tv: map.auto_expiry_notify_tv !== '0', // padrão ligado se habilitado
    auto_expiry_notify_wa: map.auto_expiry_notify_wa === '1',
    auto_expiry_message: map.auto_expiry_message || 'Olá {{nome}}! Sua assinatura do Korp TV vence em {{dias}} dia(s) (no dia {{data}}). Renove para continuar assistindo sem interrupções!',
    test_api_without_adults: map.test_api_without_adults || '',
    test_api_with_adults: map.test_api_with_adults || '',
    portal_public_domain: map.portal_public_domain || process.env.PORTAL_PUBLIC_DOMAIN || '',
    test_app_url: map.test_app_url || '',
    test_auto_whatsapp: map.test_auto_whatsapp !== '0',
    test_whatsapp_message: map.test_whatsapp_message || 'Olá {{nome}}! Seu teste do Korp TV foi liberado com sucesso 🎉\n\n📲 *Baixar Aplicativo:* {{app_url}}\n\n👤 *Usuário:* {{usuario}}\n🔑 *Senha:* {{senha}}\n📅 *Vencimento:* {{vencimento}}\n\nAbra o aplicativo e assista no seu celular, TV Box ou Smart TV!'
  };
}

// Salva ou atualiza configurações
async function saveSettings(settingsObj) {
  for (const [key, value] of Object.entries(settingsObj)) {
    const valStr = typeof value === 'boolean' ? (value ? '1' : '0') : String(value ?? '');
    const exists = (await db.query('SELECT key FROM admin_settings WHERE key = ?', [key])).rows.length > 0;
    if (exists) {
      await db.query('UPDATE admin_settings SET value = ? WHERE key = ?', [valStr, key]);
    } else {
      await db.query('INSERT INTO admin_settings (key, value) VALUES (?, ?)', [key, valStr]);
    }
  }
  return getSettings();
}

// Migra a URL/Host IPTV de todos os clientes conectados ao portal de uma vez só
async function migrateAllClientsUrl(rawNewUrl) {
  if (!rawNewUrl) {
    throw new Error('Nova URL do IPTV é obrigatória.');
  }

  // Normaliza host (remove trailing slash)
  const newHost = rawNewUrl.trim().replace(/\/+$/, '');
  const now = Date.now();

  // 1. Atualiza aparelhos Xtream
  const resXtream = await db.query(
    `UPDATE devices 
     SET xtream_host = ?, playlist_updated_at = ? 
     WHERE playlist_type = 'xtream'`,
    [newHost, now]
  );

  // 2. Salva como URL padrão nas configurações
  await saveSettings({ default_iptv_url: newHost });

  const affected = resXtream.changes || 0;
  return {
    success: true,
    newHost,
    updatedAt: now,
    affected
  };
}

// Analisa a data de expiração (formato DD/MM/AAAA) e retorna dias restantes (-1 se já venceu ou inválido)
function calculateDaysRemaining(expStr) {
  if (!expStr || typeof expStr !== 'string') return null;
  if (expStr.toLowerCase().includes('ilimitado')) return null;

  const parts = expStr.trim().split('/');
  if (parts.length === 3) {
    const day = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1;
    const year = parseInt(parts[2], 10);
    const expDate = new Date(year, month, day, 23, 59, 59);
    const diffMs = expDate.getTime() - Date.now();
    return Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  }
  return null;
}

// Executa verificação e envio das automações de renovação
async function runExpiryAutomations() {
  const cfg = await getSettings();
  if (!cfg.auto_expiry_enabled) {
    return { success: true, executed: 0, reason: 'Automação desabilitada nas configurações' };
  }

  const daysThreshold = cfg.auto_expiry_days;
  const devices = (await db.query('SELECT * FROM devices WHERE status != ?', ['blocked'])).rows;

  let countTv = 0;
  let countWa = 0;
  const errors = [];
  const now = Date.now();
  const ONE_DAY_MS = 20 * 60 * 60 * 1000; // 20 horas para não reenviar no mesmo dia

  for (const dev of devices) {
    if (!dev.playlist_exp_date) continue;
    const daysLeft = calculateDaysRemaining(dev.playlist_exp_date);
    if (daysLeft === null) continue;

    // Se estiver na janela de aviso (ex: faltam entre 0 e X dias)
    if (daysLeft >= 0 && daysLeft <= daysThreshold) {
      const lastNotified = Number(dev.last_expiry_notified_at || 0);
      if (now - lastNotified < ONE_DAY_MS) {
        // Já foi avisado recentemente hoje
        continue;
      }

      // Prepara o texto da mensagem
      const messageText = (cfg.auto_expiry_message || '')
        .replace(/\{\{nome\}\}/g, dev.name || 'Cliente')
        .replace(/\{\{dias\}\}/g, String(daysLeft))
        .replace(/\{\{data\}\}/g, dev.playlist_exp_date)
        .replace(/\{\{aparelho\}\}/g, dev.model || 'sua TV');

      let notifiedThisDev = false;

      // 1. Notificação na TV
      if (cfg.auto_expiry_notify_tv) {
        try {
          const msgId = `exp_${Date.now()}`;
          await db.query(
            'UPDATE devices SET message = ?, message_type = ?, message_id = ? WHERE id = ?',
            [messageText, 'warning', msgId, dev.id]
          );
          countTv++;
          notifiedThisDev = true;
        } catch (e) {
          errors.push(`Erro ao enviar TV para ${dev.id}: ${e.message}`);
        }
      }

      // 2. Notificação no WhatsApp
      if (cfg.auto_expiry_notify_wa && dev.client_phone && cfg.whatsapp_api_url && cfg.whatsapp_api_token) {
        try {
          await sendWhatsAppMessage({
            apiUrl: cfg.whatsapp_api_url,
            apiToken: cfg.whatsapp_api_token,
            phone: dev.client_phone,
            message: messageText
          });
          countWa++;
          notifiedThisDev = true;
        } catch (e) {
          errors.push(`Erro WhatsApp (${dev.client_phone}): ${e.message}`);
        }
      }

      if (notifiedThisDev) {
        await db.query('UPDATE devices SET last_expiry_notified_at = ? WHERE id = ?', [now, dev.id]);
      }
    }
  }

  return {
    success: true,
    scanned: devices.length,
    notifiedTv: countTv,
    notifiedWa: countWa,
    errors
  };
}

module.exports = {
  getSettings,
  saveSettings,
  migrateAllClientsUrl,
  runExpiryAutomations
};

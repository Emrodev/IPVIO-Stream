const fetch = global.fetch || require('node-fetch');

/**
 * Envio de mensagem WhatsApp seguindo o padrão oficial da API do WhatsApp (Cloud API Meta)
 * e compatível com gateways WhatsApp padrão.
 */
async function sendWhatsAppMessage({ apiUrl, apiToken, phone, message }) {
  if (!apiUrl) {
    throw new Error('URL da API do WhatsApp não configurada.');
  }
  if (!phone || !message) {
    throw new Error('Telefone ou mensagem vazia.');
  }

  // Remove caracteres não numéricos do telefone
  const cleanPhone = String(phone).replace(/\D/g, '');
  if (cleanPhone.length < 8) {
    throw new Error(`Número de telefone inválido: ${phone}`);
  }

  const endpoint = apiUrl.trim();
  const token = (apiToken || '').trim();

  // Payload padrão oficial WhatsApp Cloud API
  const payload = {
    messaging_product: 'whatsapp',
    recipient_type: 'individual',
    to: cleanPhone,
    type: 'text',
    text: {
      preview_url: false,
      body: message
    }
  };

  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  }

  const resText = await response.text();
  let resJson;
  try {
    resJson = JSON.parse(resText);
  } catch (_) {
    resJson = { raw: resText };
  }

  if (!response.ok) {
    const errorMsg = resJson?.error?.message || resJson?.message || resText || `HTTP ${response.status}`;
    throw new Error(`Erro na API do WhatsApp: ${errorMsg}`);
  }

  return {
    success: true,
    data: resJson
  };
}

module.exports = {
  sendWhatsAppMessage
};

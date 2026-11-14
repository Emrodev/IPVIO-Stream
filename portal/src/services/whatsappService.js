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

  // Remove caracteres não numéricos do telefone e normaliza DDI 55 se omitido
  let cleanPhone = String(phone).replace(/\D/g, '');
  if ((cleanPhone.length === 10 || cleanPhone.length === 11) && !cleanPhone.startsWith('55')) {
    cleanPhone = '55' + cleanPhone;
  }
  if (cleanPhone.length < 10) {
    throw new Error(`Número de telefone inválido: ${phone}`);
  }

  const endpoint = apiUrl.trim();
  const token = (apiToken || '').trim();

  // Contrato oficial da ViperConnect / WhatsApp Cloud API
  const payload = {
    messaging_product: 'whatsapp',
    to: cleanPhone,
    type: 'text',
    text: {
      body: message
    }
  };

  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  }

  const response = await fetch(endpoint, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload)
  });

  const resText = await response.text();
  let resJson;
  try {
    resJson = JSON.parse(resText);
  } catch (_) {
    resJson = { raw: resText };
  }

  if (!response.ok) {
    let errorMsg = resJson?.error?.message 
      || resJson?.error 
      || resJson?.message 
      || (typeof resJson === 'string' ? resJson : '') 
      || resText 
      || `HTTP ${response.status}`;
    if (typeof errorMsg === 'object') errorMsg = JSON.stringify(errorMsg);
    throw new Error(`Erro ${response.status}: ${errorMsg}`);
  }

  return {
    success: true,
    data: resJson
  };
}

module.exports = {
  sendWhatsAppMessage
};

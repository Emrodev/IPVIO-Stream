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

/**
 * Consulta a API da ViperConnect para checar se o número é realmente um WhatsApp ativo
 * e recuperar o LID / user_id do contato.
 */
async function checkWhatsAppNumber({ apiUrl, apiToken, phone }) {
  if (!apiUrl) return { isValid: false, reason: 'URL da API WhatsApp não configurada' };

  let cleanPhone = String(phone).replace(/\D/g, '');
  if ((cleanPhone.length === 10 || cleanPhone.length === 11) && !cleanPhone.startsWith('55')) {
    cleanPhone = '55' + cleanPhone;
  }

  // Se a apiUrl for do tipo https://viper.kabaweb.in/v25/5581988985300/messages
  // o endpoint de contatos da ViperConnect é https://viper.kabaweb.in/{sessionPhone}/contacts?search={cleanPhone}
  try {
    const urlObj = new URL(apiUrl.trim());
    const match = urlObj.pathname.match(/\/(?:v\d+\/)?(\d{10,15})\/?/);
    const sessionPhone = match ? match[1] : '';

    if (!sessionPhone) {
      // Se não identificar a sessão pela URL, assume como válido para não bloquear indevidamente
      return { isValid: true, phone: cleanPhone, lid: null };
    }

    const contactsUrl = `${urlObj.origin}/${sessionPhone}/contacts?search=${encodeURIComponent(cleanPhone)}`;
    const headers = {};
    if (apiToken) {
      headers['Authorization'] = apiToken.startsWith('Bearer ') ? apiToken : `Bearer ${apiToken}`;
    }

    const resp = await fetch(contactsUrl, { method: 'GET', headers });
    if (!resp.ok) {
      return { isValid: true, phone: cleanPhone, lid: null };
    }

    const data = await resp.json();
    const contacts = Array.isArray(data?.contacts) ? data.contacts : [];
    
    // Procura correspondência do telefone no array retornado
    const found = contacts.find(c => {
      const p = String(c.phone_number || '').replace(/\D/g, '');
      const uid = String(c.user_id || '');
      return p === cleanPhone || p.endsWith(cleanPhone.slice(-8)) || uid.includes(cleanPhone);
    });

    if (found) {
      return {
        isValid: true,
        phone: cleanPhone,
        lid: found.user_id || null,
        name: found.display_name || null
      };
    }

    // Se o contato ainda não estava na agenda/cache da sessão da Viper,
    // verifica pelo tamanho e formato padrão de número BR ou se retornou vazia
    return {
      isValid: contacts.length > 0,
      phone: cleanPhone,
      lid: contacts[0]?.user_id || null
    };
  } catch (err) {
    console.warn('[WhatsApp Check Warning]', err.message);
    return { isValid: true, phone: cleanPhone, lid: null, error: err.message };
  }
}

module.exports = {
  sendWhatsAppMessage,
  checkWhatsAppNumber
};

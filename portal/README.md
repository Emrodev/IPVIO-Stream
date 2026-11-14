# IPVIO Stream · Portal de Gerenciamento de Dispositivos, Playlists & OTA

Painel web administrativo e API REST para gerenciamento remoto centralizado de dispositivos rodando o aplicativo **IPVIO Stream**.

Permite gerenciar TVs e celulares à distância: parear via código de ativação na tela, atribuir listas M3U / Xtream Codes remotamente, verificar validade da conta no provedor, enviar mensagens push e distribuir atualizações de APK via OTA.

---

## 🚀 Principais Recursos

1. **Dashboard Web com Autenticação**:
   - Tela de login com usuário e senha protegida por tokens de sessão.
   - Interface moderna responsiva com tema escuro (Dark Mode).
   - Métricas em tempo real: total de aparelhos, online agora, listas ativas e versão OTA vigente.

2. **Vinculação Simplificada por Código (Pairing Code)**:
   - Aparelhos sem lista exibem um **Código de Ativação** (ex: `7A1-9FB`) e MAC Virtual na tela inicial.
   - No portal, basta clicar em **`🔗 Vincular por Código`** e informar as credenciais Xtream: o aparelho sincroniza e abre os canais automaticamente em segundos!

3. **Monitoramento e Validade de Playlist**:
   - Consulta automática da data de expiração, status e conexões simultâneas via API Xtream Codes (`player_api.php`).
   - Badges coloridas indicando status: 🟢 Válida (com contagem de dias restantes), 🟡 Vencendo em breve ou 🔴 Vencida.
   - Botão para reconsultar validade sob demanda.

4. **Gerenciamento de Dispositivos**:
   - Identificação completa: Device ID, MAC Virtual (`00:1A:79:XX:XX:XX`), Modelo, IP interno/externo e versão do app instalada.
   - Bloqueio / Desbloqueio remoto de dispositivos.
   - Renomeação personalizada de aparelhos.

5. **Mensagens Push na Tela**:
   - Envio de notificações instantâneas individuais ou em massa (Broadcast).
   - Níveis de aviso: Informativo (azul), Aviso/Cobrança (amarelo) ou Alerta Urgente (vermelho).

6. **Servidor de Atualizações OTA (Over-The-Air)**:
   - Hospedagem direta de arquivos APK enviados pelo painel.
   - Suporte a links externos de download (ex: GitHub Releases).
   - Flag de atualização obrigatória.

7. **Banco de Dados Híbrido (Zero Configuração)**:
   - Inicia por padrão com **SQLite** sem necessidade de instalar nada.
   - Pronto para migrar para **PostgreSQL** a qualquer momento mudando apenas o `.env`.

---

## 🛠️ Como Executar o Portal

### Opção 1: Execução Local com Node.js

1. **Pré-requisitos**:
   - Node.js 18+ instalado.

2. **Instalação e execução**:
   ```bash
   cd portal
   npm install
   npm start
   ```

3. **Acesso ao Painel**:
   - Abra no navegador: `http://localhost:3000` (ou `http://SEU_IP:3000`)
   - **Usuário padrão:** `admin`
   - **Senha padrão:** `admin123`

---

### Opção 2: Execução com Docker / Docker Compose

```bash
cd portal
docker compose up -d
```
O serviço estará acessível na porta configurada (padrão `3000`).

---

## ⚙️ Configuração (.env)

Crie ou edite o arquivo `portal/.env` para personalizar as opções:

```env
# Porta do servidor
PORT=3000

# Credenciais de acesso ao Painel Web
ADMIN_USER=admin
ADMIN_PASS=admin123
ADMIN_KEY=admin123
SESSION_SECRET=ipvio-stream-secret-token-key-2026

# Banco de dados ('sqlite' ou 'postgres')
DB_TYPE=sqlite
SQLITE_PATH=./data/portal.db

# Se for usar PostgreSQL:
# DB_TYPE=postgres
# DATABASE_URL=postgresql://usuario:senha@localhost:5432/ipvio_portal
```

---

## 📱 Configurando o App Android para Conectar ao Portal

No arquivo **`app/build.gradle.kts`**, altere a variável `PORTAL_URL`:

```kotlin
// ==============================================================================
// CONFIGURAÇÃO DO PORTAL REMOTO IPVIO
// Altere esta URL sempre que mudar de servidor/domínio para os novos APKs:
val PORTAL_URL: String = (project.findProperty("PORTAL_URL") as String?)
    ?: System.getenv("PORTAL_URL")
    ?: "http://192.168.1.100:3000"
// ==============================================================================
```

Ao compilar o APK (`./gradlew assembleRelease`), todos os novos aplicativos já apontarão automaticamente para este endereço.

Também é possível definir via parâmetro de linha de comando:
```bash
./gradlew assembleRelease -PPORTAL_URL="https://seu-portal.com"
```

---

## 📚 Endpoints da API

### Autenticação & Administração (Painel Web)
- `POST /api/admin/login`: Autentica com `username` e `password`, retorna token de sessão.
- `GET /api/admin/devices`: Lista todos os dispositivos registrados.
- `POST /api/admin/devices/link-by-code`: Vincula playlist diretamente usando o Código de Ativação (`code`).
- `POST /api/admin/devices/:id/playlist`: Atualiza a playlist de um aparelho específico.
- `POST /api/admin/devices/:id/check-validity`: Consulta validade da lista no servidor Xtream.
- `POST /api/admin/devices/:id/message`: Envia mensagem na tela de um aparelho.
- `POST /api/admin/devices/broadcast-message`: Envia mensagem para todos os aparelhos ativos.
- `PUT /api/admin/devices/:id`: Altera nome ou status (`active`/`blocked`).
- `DELETE /api/admin/devices/:id`: Exclui registro do dispositivo.
- `GET /api/admin/ota`: Lista versões OTA publicadas.
- `POST /api/admin/ota/upload`: Faz upload de novo APK e publica atualização.

### Comunicação com os Aparelhos (Android)
- `POST /api/device/register`: Registro do aparelho e obtenção de configurações.
- `POST /api/device/heartbeat`: Sinal de vida periódico e sincronização de playlists / mensagens.
- `POST /api/device/message/ack`: Confirmação de leitura de mensagens push.
- `GET /api/device/ota/check`: Verificação de novas atualizações de aplicativo.

# IPVIO Stream · Portal de Gerenciamento de Dispositivos & OTA

Portal web e API para gerenciamento remoto de dispositivos que executam o aplicativo **IPVIO Stream**.

## Recursos
- **Dashboard Web**: Painel administrativo responsivo em tempo real (Dark Mode).
- **Gerenciamento de Dispositivos**: Identificação por Device ID e MAC Virtual (`00:1A:79:XX:XX:XX`), modelo, IP, versão do app e status (Online/Offline/Bloqueado).
- **Atribuição Remota de Playlists**: Envio de listas M3U ou credenciais Xtream Codes diretamente para o aparelho.
- **Mensagens na Tela (Push)**: Envio de avisos, cobranças ou mensagens urgentes diretamente na TV do usuário.
- **Servidor OTA de Atualizações**: Upload de APKs compilados ou links do GitHub Releases para atualização direta no app.
- **Banco de Dados Flexível**: Inicia por padrão com **SQLite** (sem dependências externas) e pronto para migrar para **PostgreSQL** apenas alterando o `.env`.

---

## Como Executar Localmente

### 1. Pré-requisitos
- Node.js 18+ instalado.

### 2. Instalação e Inicialização
```bash
cd portal
npm install
npm start
```
O painel estará disponível em: **`http://localhost:3000`**
- **Chave de Admin Padrão**: `admin123` (configurável no `.env`)

---

## Como Executar com Docker / Docker Compose

```bash
cd portal
docker compose up -d
```

---

## Como Migrar para PostgreSQL

O portal possui uma camada de abstração que suporta SQLite e PostgreSQL com as mesmas consultas. Para trocar:

1. No arquivo `.env` (ou nas variáveis de ambiente do Docker), altere:
```env
DB_TYPE=postgres
DATABASE_URL=postgresql://usuario:senha@seu-host-postgres:5432/ipvio_portal
```
2. Ao reiniciar o servidor, o portal criará automaticamente todas as tabelas no PostgreSQL.

---

## Endpoints da API

### Dispositivos (App Android)
- `POST /api/device/register`: Registro inicial do aparelho.
- `POST /api/device/heartbeat`: Heartbeat periódico (a cada 60s).
- `POST /api/device/message/ack`: Confirmação de recebimento de mensagem.
- `GET /api/device/ota/check`: Consulta de novas versões do aplicativo.

### Administração (Painel Web)
*(Requer header `x-admin-key: SUA_CHAVE`)*
- `GET /api/admin/devices`: Lista todos os dispositivos.
- `PUT /api/admin/devices/:id`: Atualiza status ou nome do aparelho.
- `POST /api/admin/devices/:id/playlist`: Envia playlist remota.
- `POST /api/admin/devices/:id/message`: Envia mensagem push individual.
- `POST /api/admin/devices/broadcast-message`: Envia mensagem para todos os aparelhos.
- `GET /api/admin/ota`: Lista versões OTA publicadas.
- `POST /api/admin/ota/upload`: Upload de novo arquivo APK.

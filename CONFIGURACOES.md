# 📺 Korp TV — Guia de Configurações e Arquitetura

Este documento reúne todas as configurações, variáveis de ambiente e fluxos implementados no ecossistema do **Korp TV** (Aplicativo Android + Portal de Gerenciamento).

---

## 1. Rebranding e Nome do Aplicativo (`APP_NAME`)

O aplicativo Android suporta rebranding automático via Gradle sem alterar o código-fonte base. O nome do app é resolvido pela variável `APP_NAME` em `app/build.gradle.kts` seguindo a seguinte ordem de prioridade:

1. **Parâmetro de Linha de Comando no Gradle:**
   ```bash
   ./gradlew assembleRelease -PAPP_NAME="Korp TV"
   ```
2. **Variável de Ambiente:**
   ```bash
   export APP_NAME="Korp TV"
   ```
3. **Arquivo Local `secrets.properties` (Ignorado pelo Git):**
   ```properties
   APP_NAME=Korp TV
   PORTAL_URL=https://seu-dominio.com
   ```
4. **Fallback Automático (Padrão):**
   Caso nenhuma variável ou arquivo de configuração seja informado, o app compila com o nome original `"IPVIO Stream"`.

### O que essa variável altera automaticamente:
* Nome exibido no Android / Launcher (`android:label` e `@string/app_name`).
* Todos os textos das telas e menus em Compose via `BuildConfig.APP_NAME` e substituição dinâmica no `Lang.tr`.
* Identificador `User-Agent` de requisições de rede e checagem de atualizações OTA.
* Título da página de controle remoto web integrado.

---

## 2. Identidade Visual e Logos

Os ativos visuais foram criados no padrão moderno com gradientes em ciano e azul elétrico:

### No Portal Web:
* `/logo-icon.svg`: Ícone vetorial estilizado com o "K" em formato de reprodução e antena de TV.
* `/logo.svg`: Logo horizontal completa com o emblema e a tipografia **KORP TV**.
* `/favicon.svg`: Ícone para as abas dos navegadores.

### No Aplicativo Android (`app/src/main/res/`):
* `drawable/ic_brand_mark.png`: Logo transparente de alta resolução exibida na sidebar, loading e tela inicial.
* `drawable/ic_logo_fg.png`: Elemento frontal para o ícone adaptativo (`ic_launcher.xml`).
* `drawable/banner.png`: Banner 16:9 em alta resolução para exibição na gaveta de apps do Android TV / Google TV.
* `mipmap-*/ic_launcher.png` e `ic_launcher_round.png`: Ícones legados e adaptativos para todas as resoluções de tela (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).

---

## 3. Portal de Gerenciamento (`portal/`)

### 3.1. Execução via Docker com PostgreSQL (Recomendado)
O portal possui orquestração completa pronta para produção com PostgreSQL 16:

```bash
docker compose up -d --build
```

#### O que acontece automaticamente na inicialização:
* **Banco PostgreSQL com Healthcheck:** O container `korp-postgres` inicia primeiro e só libera o portal quando o banco estiver 100% pronto.
* **Migração Automática do SQLite:** Na primeira execução com PostgreSQL, o sistema detecta o arquivo `portal/data/portal.db`, lê todas as configurações salvas, aparelhos vinculados, histórico de testes e registros de atualização OTA e migra tudo automaticamente para o PostgreSQL.
* **Persistência de Dados:** O banco é salvo no volume `pgdata`, e os APKs são persistidos no diretório `./portal/public/apks`.
* **CI/CD no GitHub (GHCR):** O workflow `.github/workflows/docker-publish.yml` compila e publica automaticamente a imagem Docker em `ghcr.io/kabaweb/korp-portal:latest` a cada push na branch `main`.

### 3.2. Variáveis de Ambiente e Arquivo `.env` (Ignorado pelo Git)
No diretório `portal/.env`, configure as variáveis do servidor:

```env
PORT=3000
DB_TYPE=sqlite
ADMIN_USER=admin
ADMIN_PASS=sua_senha_aqui
ADMIN_KEY=sua_chave_secreta_api
PORTAL_PUBLIC_DOMAIN=https://seu-dominio.com
```

### 3.2. Configurações Salvas no Painel Web (`/`):
* **URL Padrão do IPTV (Host / Servidor):**
  - Configurada na aba *Automações & WhatsApp*.
  - Ao vincular um aparelho por código ou configurar playlist, o campo do servidor já abre **automaticamente preenchido** com esta URL.
  - Se o campo for deixado em branco, a API utiliza esta URL por padrão, evitando falhas de verificação de expiração.
* **Domínio Público do Portal (HTTPS):**
  - Utilizado para montar os links públicos de download do APK (`/app.apk`) enviados por WhatsApp e QR Code, garantindo que o link externo nunca seja enviado como `localhost`.
* **Código do Downloader (AFTVnews):**
  - Facilitador para Smart TVs, Android TV e Fire TV Stick. O cliente só precisa digitar um código numérico de 5 a 6 dígitos no aplicativo **Downloader** da Play Store/Amazon Store.
  - Para gerar seu código permanente:
    1. Acesse [go.aftvnews.com](https://go.aftvnews.com)
    2. Digite o link direto do APK: `https://seu-dominio/app.apk`
    3. Conclua o captcha e clique em **Shorten**.
    4. Copie o código numérico gerado e cole no campo *Código do Downloader* nas configurações do portal.
  - Ele aparecerá automaticamente na página `/teste`, no modal de QR Code e nas mensagens de WhatsApp para os clientes.
* **Integração com API WhatsApp (ViperConnect / Cloud API):**
  - **URL da API:** Endpoint da instância (ex: `https://viper.dominio.com/v25/{SESSION}/messages`).
  - **Token de Acesso:** Bearer Token de autenticação.
  - **Validação de Número:** Antes de gerar o teste, o portal consulta a existência do número no WhatsApp via API e armazena o status (`is_valid_wa`) e o identificador do contato (`wa_lid`).
* **Chatbot de Testes Automáticos (Painel Slim):**
  - Endpoint da API para listas familiares (Sem Adultos).
  - Endpoint da API para listas Com Adultos.

---

## 4. Atualizações OTA (Over-The-Air)

1. **Compilação do APK Release:**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot"
   .\gradlew.bat assembleRelease
   ```
2. **Localização do APK gerado:**
   `app/build/outputs/apk/release/app-release.apk`
3. **Publicação no Portal:**
   - Faça upload pelo painel do portal na aba **Atualizações OTA** ou copie para `portal/public/apks/Korp-TV-vX.X.X.apk`.
   - O portal atualiza automaticamente o link `/app.apk` e notifica os aparelhos conectados que possuem uma versão anterior.

---

## 5. Aplicação Web & Smart TV (Samsung Tizen, LG webOS, Browsers)

A aplicação **Korp TV Web** (`web-tv/`) replica fielmente a interface e o ecossistema do aplicativo Android TV:
* **Domínio Oficial de Acesso Web:** `https://playkorp.kabaweb.in`
* **Porta Dedicada no Servidor:** `3001` (acesso local: `http://localhost:3001`).
* **Navegação D-Pad / Controle Remoto:** Suporte nativo a Setas (Cima, Baixo, Esquerda, Direita), Enter/OK e Botão Voltar (Escape / Backspace / KeyCodes Tizen e webOS `10009` e `461`).
* **Pareamento com o Portal:** Ao abrir, se ainda não estiver configurada, gera o código de vinculação e QR Code na tela da TV para ativação instantânea via portal (`/api/device/register` e `/api/device/heartbeat`).
* **Player Integrado:** Hls.js com proxy reverso inteligente de streaming (`/api/proxy?url=...`) com suporte a redirecionamentos e cabeçalhos HTTP Range para contornar bloqueios de CORS de servidores IPTV.
* **Empacotamento Smart TV:**
  - **Samsung Tizen:** Manifesto pronto em `web-tv/tizen/config.xml` (Tizen Web Widget apontando para `https://playkorp.kabaweb.in`).
  - **LG webOS:** Manifesto pronto em `web-tv/webos/appinfo.json`.
* **Execução via Docker:**
  O serviço `webtv` está integrado ao `docker-compose.yml` e ao pipeline do GitHub Actions para publicação automática em `ghcr.io/kabaweb/korp-webtv`.

---

## 6. Segurança e Arquivos Ignorados pelo Git

Os seguintes arquivos são confidenciais e **nunca** devem ser comitados no Git:
* `secrets.properties`: Chaves de API privadas (TMDB, OMDB, URL do portal, nome do app).
* `keystore.properties` e `*.keystore`: Chaves criptográficas de assinatura do aplicativo Android.
* `portal/.env`: Credenciais do administrador do portal e segredo de sessão.
* `portal/data/portal.db`: Banco de dados de clientes, tokens e histórico de testes.
* `*.apk`: Binários compilados.

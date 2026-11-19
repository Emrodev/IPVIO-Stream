# IPVIO Stream · Player IPTV para Android TV & Portal de Gerenciamento

> **Aviso Importante:** O aplicativo **não fornece e não inclui nenhum tipo de conteúdo**. Ele não contém canais, listas, filmes ou séries embutidos. O aplicativo é exclusivamente um reprodutor para listas que **você** adiciona por conta própria (login Xtream Codes, endereço M3U ou portal Stalker). Utilize apenas conteúdos que você possui autorização legal para reproduzir.

---

## 📌 Visão Geral

O **IPVIO Stream** é um reprodutor IPTV open-source e profissional, projetado especificamente para a experiência de controle remoto em **Android TV, Google TV e Amazon Fire TV**, desenvolvido nativamente com **Kotlin** e **Jetpack Compose**.

Esta versão estendida traz uma suíte completa de gerenciamento e funcionalidades avançadas:
- **Download Offline (VOD)**: Baixe filmes e episódios de séries para assistir sem internet, com proteção integrada vinculada à validade da assinatura.
- **Gravações Locais**: Grave transmissões de TV ao vivo com buffer contínuo e exclua os arquivos diretamente pela interface.
- **Portal Web de Gerenciamento Remoto**: Vincule listas à distância por código na tela (Pairing Code), distribua atualizações OTA e monitore aparelhos em tempo real.
- **Banco de Dados Padrão SQLite**: Inicialização simplificada e com zero dependências externas para o servidor do portal.
- **Player WebTV Integrado**: Assista também pelo navegador em PCs e Smart TVs via D-Pad/teclado.
- **100% Traduzido em Português do Brasil (pt-BR)**.

---

## ✨ Funcionalidades do Aplicativo Android TV

### 🎬 Filmes e Séries (VOD) & Download Offline
- **Download Local para Filmes e Séries**: Baixe conteúdos para assistir offline no armazenamento interno ou em pendrive/SSD.
- **Validação de Assinatura IPTV**: A reprodução de arquivos baixados é automaticamente conferida contra a validade da conta (`exp_date`) sincronizada pelo portal. Se o plano estiver expirado ou o aparelho bloqueado, o aplicativo impede a reprodução.
- **Linha e Aba "Arquivos Baixados"**: Acesso rápido e organizado na tela inicial e no menu superior.
- **Gerenciamento no App**: Exclua arquivos baixados diretamente pelo botão na tela de detalhes do filme ou pelo menu de contexto de cada episódio.
- **Fichas Técnicas & Metadados**: Integração com TMDB para sinopses, elenco, diretores, trailers e pôsteres em alta resolução.
- **Seleção de Qualidade e Legendas**: Suporte a múltiplas qualidades e faixas de áudio.

### 📺 TV ao Vivo (Live TV)
- **Guia de Programação (EPG)** completo com linha do tempo, exibição do que está passando e próximos eventos.
- **Do Início (Catch-up/Archive)**: Volte a transmissão ao vivo do início em canais compatíveis.
- **Gravação Direta & Lembretes**: Grave a programação atual em segundo plano ou programe notificações de início de programas favoritos.
- **Buffer de Retrocesso**: Pause e volte a transmissão da TV ao vivo em tempo real.
- **Organização de Canais**: Seleção rápida de categorias, favoritos, canais mais assistidos e modo esportes.

### ⚡ Player Avançado (Media3 / ExoPlayer)
- Suporte nativo a transmissões **HLS (.m3u8), MPEG-TS e MPEG-DASH**.
- Decodificador de áudio **FFmpeg embutido** para reprodução perfeita de formatos AC3, E-AC3 e DTS.
- Reconexão inteligente para TV ao vivo (watchdog contra travamentos e quedas de rede).
- Ajustes finos: troca de proporção de tela (zoom/fit), sincronização de taxa de quadros (frame-rate matching), temporizador de desligamento (sleep timer) e relógio no topo da tela.

### 🎨 Personalização & Experiência
- Navegação fluida 100% pensada para o controle remoto (D-Pad).
- Mais de 12 temas visuais e seleção de fontes (Outfit, Inter ou do sistema).
- Perfis de usuário com controle parental por código PIN.
- Histórico de reprodução, progresso salvo por perfil e "Continuar Assistindo".

---

## 🌐 Portal de Gerenciamento de Aparelhos & OTA

O repositório inclui um painel administrativo web construído em **Node.js (Express)**, localizado na pasta `portal/`.

### Principais Recursos do Portal:
1. **Ativação Fácil por Código (Pairing Code)**:
   - Aparelhos recém-instalados mostram um código de 6 caracteres na tela (ex: `A4B-9X2`).
   - O administrador digita o código no painel e atribui o login Xtream. O aparelho carrega a lista imediatamente sem ninguém precisar digitar URLs ou senhas no controle remoto.
2. **Distribuição de Atualizações OTA (Over-The-Air)**:
   - Envie o novo arquivo APK pelo painel web.
   - O portal detecta automaticamente o `versionCode` e o `versionName` do APK e disponibiliza o download imediato para todas as TVs conectadas.
3. **Monitoramento de Validade & Conexões**:
   - Consulta periódica das credenciais Xtream (`player_api.php`) para acompanhar dias restantes de assinatura, status e limite de telas.
4. **Notificações na Tela (Push)**:
   - Envie avisos ou lembretes de renovação diretamente para a TV do cliente.
5. **Monitoramento "Assistindo Agora"**:
   - Acompanhe em tempo real qual canal ou conteúdo está sendo executado em cada dispositivo ativo.
6. **Migração em Massa de Servidor**:
   - Altere a URL do servidor IPTV de todos os aparelhos com um único clique no painel.

---

## 💾 Banco de Dados Padrão: SQLite

Por padrão de projeto, o portal utiliza **SQLite** (`./data/portal.db`), permitindo subir o servidor instantaneamente sem a necessidade de instalar, rodar ou configurar nenhum banco externo.

- **Vantagens**:
  - Zero configuração: basta iniciar a aplicação e o arquivo do banco é criado e inicializado automaticamente.
  - Portabilidade total: o arquivo `portal.db` contém todos os dados e tabelas.
  - Suporte opcional a PostgreSQL: caso deseje migrar para ambientes com altíssimo volume de requisições concorrentes, basta alternar a variável `DB_TYPE=postgres` no arquivo `.env`.

---

## 🚀 Como Executar o Portal Web

### 1. Execução Local com Node.js

```bash
# Entre na pasta do portal
cd portal

# Instale as dependências
npm install

# Inicie o servidor
npm start
```

O painel estará disponível em: `http://localhost:3000` (ou `http://IP_DO_SERVIDOR:3000`).

- **Usuário padrão:** `admin`
- **Senha padrão:** `admin123`

### 2. Configuração do Arquivo `.env` (Exemplo Padrão)

Crie ou edite o arquivo `portal/.env`:

```env
# Porta do servidor
PORT=3000

# Credenciais de acesso ao Painel Web
ADMIN_USER=admin
ADMIN_PASS=admin123
ADMIN_KEY=admin123
SESSION_SECRET=chave-secreta-para-tokens-2026

# Banco de dados padrão (SQLite - sem dependências adicionais)
DB_TYPE=sqlite
SQLITE_PATH=./data/portal.db

# Exemplo opcional com PostgreSQL (caso queira usar no futuro):
# DB_TYPE=postgres
# DATABASE_URL=postgresql://usuario:senha@localhost:5432/ipvio_portal
```

---

## 📱 Compilação do Aplicativo Android TV

### Pré-requisitos
- **Android Studio** ou **JDK 17+**.
- Android SDK instalado com as plataformas e build-tools mais recentes.

### Configurar a URL do Portal Remoto
Antes de gerar a release, configure o endereço onde seu portal web está hospedado.

No arquivo `app/build.gradle.kts`:
```kotlin
val PORTAL_URL: String = (project.findProperty("PORTAL_URL") as String?)
    ?: System.getenv("PORTAL_URL")
    ?: "http://192.168.1.100:3000" // Substitua pelo IP ou domínio do seu portal
```

Também é possível passar diretamente via linha de comando ao compilar:
```bash
# Compilar versão de lançamento (Release APK)
./gradlew assembleRelease -PPORTAL_URL="https://seu-portal.com"
```

O arquivo final gerado estará em:
`app/build/outputs/apk/release/app-release.apk`

---

## 🖥️ WebTV (Player Web & Smart TV)

Na pasta `webtv/` encontra-se um player web completo, desenvolvido com **React + Tailwind CSS + Video.js / Hls.js**:
- Totalmente compatível com navegadores de Smart TVs (Samsung Tizen, LG webOS, Android TV) e navegadores desktop.
- Controle total via teclado ou teclas direcionais do controle remoto (D-Pad).
- Conecta-se diretamente aos mesmos endpoints de playlist e autenticação do portal.

Para executar localmente:
```bash
cd webtv
npm install
npm run dev
```

---

## 🔒 Privacidade e Segurança

- O aplicativo é distribuído sem chaves de API embutidas. Chaves TMDB/OMDb podem ser inseridas opcionalmente pelo usuário no menu de configurações e ficam salvas exclusivamente na memória local do aparelho.
- As senhas e dados de playlist trafegam diretamente entre o aparelho e o provedor IPTV cadastrado (ou o portal próprio administrado por você).
- O código-fonte não contém dependências com serviços de telemetria fechados ou de terceiros.

---

## 📄 Licença

Este projeto é disponibilizado sob a licença de código aberto aplicável ao repositório original. Consulte o arquivo [LICENSE](LICENSE) para obter detalhes.

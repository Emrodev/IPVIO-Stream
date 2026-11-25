package com.ipvio.stream

// Brazilian Portuguese translations (part 5/6): German-Portuguese rows
val DictPt5 = """
Dunkler Kasten hinter der Uhr. Aus = nur die Zahlen.¦Fundo escuro atrás do relógio. Desligado = apenas os números.
Puffer (bei langsamem Internet)¦Buffer (para conexões lentas)
Bildrate anpassen¦Ajustar taxa de quadros
Ruckelt – Puffer wird beim nächsten Start größer¦Travando – o buffer aumentará na próxima inicialização
App-Aktualisierung¦Atualização do aplicativo
Adresse der Update-Datei (https://…/update.json)¦Endereço do arquivo de atualização (https://…/update.json)
Nach Update suchen¦Buscar atualizações
Herunterladen und installieren¦Baixar e instalar
Du hast die neueste Version.¦Você já tem a versão mais recente.
Bitte warten …¦Aguarde …
Installation gestartet¦Instalação iniciada
Verbindung hängt – neuer Versuch¦Conexão travada – tentando novamente
Titel pro Zeile (Kino)¦Títulos por linha (Cinema)
Rechts = nächste Staffel  ·  Links = Staffeln¦Direita = próxima temporada  ·  Esquerda = temporadas
Outfit¦Outfit
Inter¦Inter
System¦Sistema
Noch keine Favoriten¦Nenhum favorito ainda
Zu Live TV¦Ir para TV ao vivo
Noch nichts in deiner Liste¦Sua lista está vazia
Filme entdecken¦Descobrir filmes
Noch nichts angesehen¦Nada assistido ainda
Hier erscheinen die zuletzt gestarteten Filme und Serien.¦Os filmes e séries iniciados recentemente aparecerão aqui.
Auf einem Sender OK gedrückt halten, bei Filmen und Serien auf der Detailseite „Zu Favoriten“ wählen.¦Segure OK sobre um canal; para filmes e séries selecione „Adicionar aos favoritos“ na tela de detalhes.
Auf der Seite eines Films oder einer Serie „Meine Liste“ wählen.¦Selecione „Minha lista“ na tela de qualquer filme ou série.
Keine Treffer¦Nenhum resultado
Mit diesen Filtern gibt es hier keine Titel.¦Nenhum título encontrado com estes filtros.
Filter zurücksetzen¦Redefinir filtros
Diese Liste ist noch leer¦Esta lista ainda está vazia
Auf einem Sender OK gedrückt halten und „Zu Favoriten“ wählen.¦Segure OK sobre um canal e selecione „Adicionar aos favoritos“.
In „Alle Sender“ oder einer Kategorie auf einem Sender OK gedrückt halten und „Zu …“ wählen.¦Em „Todos os canais“ ou em uma categoria, segure OK no canal e selecione „Adicionar a…“.
In dieser Kategorie gibt es keine Sender. Links eine andere Kategorie wählen.¦Não há canais nesta categoria. Escolha outra categoria à esquerda.
Prüfe die Schreibweise oder suche nach einem Teil des Titels. Kleine Tippfehler findet die Suche trotzdem.¦Verifique a ortografia ou busque por parte do nome. A busca tolera pequenos erros de digitação.
Outfit (Standard) und Inter sind in die App eingebaut, die übrigen Schriften stammen von deinem Gerät.¦Outfit (padrão) e Inter são integradas ao app; as demais fontes vêm do seu próprio aparelho.
Top 10 heute¦Top 10 hoje
Danach¦A seguir
Für diesen Sender gibt es kein Programm.¦Não há dados de programação para este canal.
FILM¦FILME
SERIE¦SÉRIE
Runter für Details und Abspielen¦Para baixo para detalhes e reprodução
Links/Rechts = {#0} s springen¦Esquerda/Direita = pular {#0} s
2020er¦Anos 2020
2010er¦Anos 2010
2000er¦Anos 2000
1990er¦Anos 1990
Infos auf dem Poster¦Informações no pôster
Breites Szenenbild¦Imagem de cena ampla
Neu: Version {0}¦Novo: versão {0}
Fehler {#0}¦Erro {#0}
Keine Update-Adresse eingetragen¦Nenhum endereço de atualização configurado
Update-Datei unvollständig¦Arquivo de atualização incompleto
Leere Antwort¦Resposta vazia
Lade … {#0} %¦Carregando … {#0} %
Erlaube der App einmal „Apps installieren“ und tippe dann erneut.¦Permita que o app „Instale apps desconhecidos“ nas configurações e tente novamente.
Trage die Adresse einer kleinen Datei update.json ein, die du selbst bereitstellst: {"versionCode": 2, "versionName": "1.1", "apk": "ipvio.apk"}. Die neue APK muss mit demselben Schlüssel signiert sein wie die installierte, sonst lehnt Android sie ab.¦Insira o endereço de um arquivo update.json próprio: {"versionCode": 2, "versionName": "1.1", "apk": "ipvio.apk"}. O novo APK precisa ser assinado com a mesma chave do app instalado.
Startet die nächste Folge, sobald der Abspann beginnt. Geht nur, wenn für die Serie Abspann-Zeiten bekannt sind (TheIntroDB); sonst bleibt es beim Hinweis in den letzten 30 Sekunden.¦Inicia o próximo episódio assim que os créditos começam (requer dados no TheIntroDB); caso contrário avisa nos últimos 30 segundos.
Zeigt die aktuelle Uhrzeit beim Abspielen in einer Ecke (nicht, solange die Steuerleiste offen ist).¦Mostra a hora atual em um canto da tela durante a reprodução (oculto quando a barra de controle estiver aberta).
Oben rechts¦Superior direito
Oben links¦Superior esquerdo
Unten rechts¦Inferior direito
Unten links¦Inferior esquerdo
Die Uhr folgt der Zeitzone und dem 12/24-Stunden-Format aus den Einstellungen. Im Player geht es auch über Einstellungen > Uhrzeit.¦O relógio segue o fuso horário e formato configurados. No player também pode ser ajustado em Configurações > Horário.
Ein größerer Puffer lädt mehr Film im Voraus und verhindert Ruckeln bei schwacher Leitung; dafür dauert der Start etwas länger und es wird mehr Speicher gebraucht. „Automatisch“ beginnt normal und vergrößert den Puffer von selbst, wenn es mehrfach ruckelt (aktuell Stufe {#0}). Gilt ab dem nächsten gestarteten Titel.¦Um buffer maior carrega mais vídeo antecipadamente evitando travamentos; a inicialização demora um pouco mais e consome mais memória. „Automático“ inicia normal e expande se houver travamentos (atualmente nível {#0}).
Passt die Bildwiederholrate des Fernsehers an den Film an (z. B. 24 Bilder), wenn der Fernseher das ohne Bildaussetzer kann. Bei Fire TV zusätzlich in den Geräteeinstellungen „Originale Bildrate anpassen“ prüfen.¦Sincroniza a taxa de atualização da TV com a do vídeo (ex: 24 fps). No Fire TV, ative também „Ajustar taxa de quadros original“ nas configurações do dispositivo.
{#0} min¦{#0} min
Quelle {#0}¦Fonte {#0}
Tonspur „{0}“ ist mit diesem Ausgang nicht abspielbar – andere Spur im Menü wählen¦A faixa de áudio „{0}“ não é suportada por esta saída – escolha outra faixa no menu
Diese Tonspur kann hier nicht abgespielt werden – andere Spur gewählt¦Esta faixa de áudio não pôde ser reproduzida – outra faixa foi selecionada
Verbindung hängt – neuer Versuch ({#0}/2)¦Conexão instável – tentando novamente ({#0}/2)
Video-Rate (laut Datei): {0}¦Taxa de vídeo (segundo o arquivo): {0}
Ton: {0}¦Áudio: {0}
Puffer: {0}¦Buffer: {0}
Auto (5)¦Auto (5)
Weil du „{0}“ geschaut hast¦Porque você assistiu a „{0}“
Ähnlich wie {0}¦Semelhante a {0}
Oben findest du alle {0}, Neuheiten, Top bewertet und Kategorien.¦No topo você encontra todos os {0}, lançamentos, mais bem avaliados e categorias.
Fortsetzen · {#0} Min¦Continuar · {#0} min
Weiter mit S{#0} · Folge {#1}¦Continuar com T{#0} · Ep. {#1}
noch nichts gemerkt¦nada registrado ainda
Liste angelegt. Mit „Sender hinzufügen“ füllen.¦Lista criada. Adicione canais através de „Adicionar canais“.
Zu „¦Adicionar a „
“ hinzufügen: OK auf einem Sender fügt ihn hinzu bzw. entfernt ihn wieder¦“: pressione OK em um canal para adicionar ou remover
Entfernt¦Removido
Hinzugefügt¦Adicionado
Diese Playlist ist sehr groß: {0} Titel in {#1} Sprachen.¦Esta lista é muito grande: {0} títulos em {#1} idiomas.
„Alles laden“ lädt alle {0} Titel. Das kann einige Minuten dauern.¦„Carregar tudo“ baixa todos os {0} títulos. Isso pode levar alguns minutos.
Gewählt: {0} Titel¦Selecionados: {0} títulos
Das dauert gerade sehr lange.¦O carregamento está demorando mais que o normal.
Sehr große Playlists brauchen lange. Mit der Sprachauswahl lädst du nur die Titel, die du wirklich brauchst.¦Listas gigantes levam mais tempo. Ao selecionar apenas seus idiomas, o carregamento fica muito mais rápido.
Server nicht gefunden (DNS)¦Servidor não encontrado (DNS)
Sichere Verbindung fehlgeschlagen. Datum und Uhrzeit des Geräts prüfen¦Falha na conexão segura. Verifique a data e a hora do aparelho
Zeitüberschreitung, Server antwortet nicht¦Tempo esgotado, o servidor não responde
API-Schlüssel¦Chaves de API
TMDB, IMDb und Rotten Tomatoes¦TMDB, IMDb e Rotten Tomatoes
Die App enthält keine eigenen Schlüssel. Mit deinen eigenen, kostenlosen Schlüsseln zeigt sie Poster, Beschreibungen, Besetzung, Trailer, automatische Genres sowie IMDb- und Rotten-Tomatoes-Wertungen. Ohne Schlüssel übernimmt sie Beschreibung, Cover und Bewertung von deinem Anbieter (bei Xtream-Zugängen).¦O app não possui chaves próprias embutidas. Com suas chaves gratuitas, ele exibe pôsteres, sinopses, elenco, trailers, gêneros automáticos e notas do IMDb/Rotten Tomatoes. Sem chaves, usa os dados do seu provedor Xtream.
TMDB (Film-Infos)¦TMDB (informações de filmes)
TMDB API-Schlüssel (v3)¦Chave de API do TMDB (v3)
Kostenlos unter themoviedb.org: Konto anlegen, anmelden, dann Einstellungen > API > API-Schlüssel anfordern und den 32-stelligen „API Key“ eintragen. Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert.¦Gratuito em themoviedb.org: crie sua conta, vá em Configurações > API > Solicitar chave e cole aqui o „API Key“ de 32 caracteres. Este app usa a API do TMDB mas não é endossado por ele.
OMDb (IMDb- und Rotten-Tomatoes-Wertung)¦OMDb (notas do IMDb e Rotten Tomatoes)
OMDb API-Schlüssel¦Chave de API do OMDb
Kostenlos unter omdbapi.com > API Key (FREE). Danach den Aktivierungslink in der E-Mail anklicken, sonst funktioniert der Schlüssel nicht. Geht auch ohne TMDB-Schlüssel.¦Gratuito em omdbapi.com > API Key (FREE). Clique no link de ativação enviado por e-mail para validar a chave. Funciona de forma independente do TMDB.
Speichern und prüfen¦Salvar e testar
Schlüssel entfernen¦Remover chaves
Schlüssel gültig¦Chave válida
Schlüssel ungültig¦Chave inválida
Keine Verbindung¦Sem conexão
Die Schlüssel bleiben nur auf diesem Gerät. Nach dem Eintragen die App einmal neu starten, damit alle Bereiche die Infos laden. Intro- und Abspann-Zeiten kommen von TheIntroDB und brauchen keinen Schlüssel.¦As chaves ficam salvas apenas neste aparelho. Reinicie o aplicativo após salvá-las para atualizar todos os dados. Tempos de introdução vêm do TheIntroDB e não precisam de chave.
Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert. Wertungen: OMDb. Intro-Zeiten: TheIntroDB.¦Este produto utiliza a API do TMDB mas não é endossado ou certificado pelo TMDB. Notas: OMDb. Introduções: TheIntroDB.
IPVIO Stream ist freie Software unter der GNU General Public License v3 (GPL-3.0). Verwendete Bibliotheken: AndroidX/Jetpack Compose, Media3 (Apache 2.0), OkHttp, Coil (Apache 2.0), Jellyfin FFmpeg-Decoder (GPL-3.0), Schriften Outfit und Inter (SIL Open Font License).¦O IPVIO Stream é um software livre sob a licença GNU GPL v3. Bibliotecas utilizadas: AndroidX/Jetpack Compose, Media3 (Apache 2.0), OkHttp, Coil, decodificador Jellyfin FFmpeg (GPL-3.0), fontes Outfit e Inter (SIL OFL).
Automatische Genres brauchen einen TMDB-Schlüssel (Einstellungen > API-Schlüssel).¦Gêneros automáticos requerem uma chave TMDB (Configurações > Chaves de API).
Tipp: Unter Einstellungen > API-Schlüssel einen eigenen TMDB-Schlüssel eintragen, dann erscheinen hier Beschreibung, Cover und Bewertungen.¦Dica: insira sua chave TMDB em Configurações > Chaves de API para exibir sinopse, capa e avaliações aqui.
Zugang wird geprüft …¦Verificando dados de acesso …
Sender werden geladen …¦Carregando canais …
Filme werden geladen …¦Carregando filmes …
Serien werden geladen …¦Carregando séries …
Serien werden geladen ({#0}/{#1}) …¦Carregando séries ({#0}/{#1}) …
Die Schnittstelle des Anbieters antwortet nicht wie erwartet.¦O servidor do provedor não respondeu no formato esperado.
Zugang abgelehnt. Benutzername, Passwort oder Abo prüfen.¦Acesso negado. Verifique usuário, senha ou validade do plano.
Der Server hat keine gültige Playlist geliefert.¦O servidor não retornou uma lista válida.
Update verfügbar: Version {0}¦Atualização disponível: versão {0}
Update wird geladen …¦Baixando atualização …
Installiert: Version {0}¦Instalada: versão {0}
Automatisch nach Updates suchen¦Buscar atualizações automaticamente
Einmal am Tag. Ein neues Update erscheint in der Glocke oben rechts.¦Verifica uma vez ao dia. Novas versões são indicadas no sino no canto superior direito.
Jetzt nach Update suchen¦Buscar atualizações agora
Updates kommen von der Projektseite auf GitHub. Deine Playlists, Favoriten, der Verlauf und die Einstellungen bleiben bei einem Update erhalten. Android fragt vor der Installation einmal nach.¦As atualizações são baixadas do GitHub oficial. Suas listas, favoritos, histórico e configurações permanecem intactos.
Eigene Update-Adresse (leer = GitHub)¦Endereço próprio de atualização (vazio = GitHub)
Noch keine Veröffentlichung gefunden¦Nenhum lançamento encontrado ainda
Dieses Stream-Format wird nicht unterstützt.¦Este formato de transmissão não é suportado.
Verbindung hängt – neuer Versuch ({#0})¦Conexão travada – nova tentativa ({#0})
Verbindung unterbrochen – neuer Versuch ({#0})¦Conexão interrompida – nova tentativa ({#0})
Stream beendet – verbinde neu …¦Transmissão encerrada – reconectando …
Das Portal hat keine Abspiel-Adresse geliefert. Später erneut versuchen.¦O portal não retornou o link de reprodução. Tente mais tarde.
Zu wenig freier Speicher für den Live-Puffer¦Espaço insuficiente para o buffer da TV ao vivo
Zu wenig freier Speicher¦Armazenamento insuficiente
Aufnahme läuft weiter – dafür braucht dein Anbieter 2 gleichzeitige Verbindungen¦Gravação em andamento – seu provedor precisa suportar 2 conexões simultâneas
30 s zurück¦30 s para trás
Aufnehmen¦Gravar
Aufnahme beenden¦Parar gravação
Aufnahme gestartet¦Gravação iniciada
Aufnahme beendet¦Gravação concluída
Aufnahme läuft¦Gravando
Sender unbekannt¦Canal desconhecido
Aufnahmen ({#0})¦Gravações ({#0})
Zurückspulen und Aufnehmen¦Retroceder e gravar
Live TV zurückspulen¦Retroceder TV ao vivo
Speichert den laufenden Sender auf dem Gerät. Dann kannst du pausieren, ohne etwas zu verpassen, mit den Spultasten zurückspringen und mit „Zurück zu Live“ wieder aufholen. Gilt für normale Live-Streams (nicht HLS).¦Salva o canal em exibição no aparelho permitindo pausar e retroceder sem perder nada. Válido para streams diretos (não HLS).
Puffergröße¦Tamanho do buffer
Ungefähr: 512 MB reichen für 10–20 Minuten in HD. Der Puffer wird beim Schließen des Players gelöscht.¦Estimativa: 512 MB suportam de 10 a 20 minutos em HD. O buffer é liberado ao sair do canal.
Speicherort für Aufnahmen¦Local de salvamento das gravações
Nachlauf nach Sendungsende¦Tempo extra após o término do programa
Aufnehmen: im Player bei einem Sender auf den roten Knopf. Die laufende Sendung wird bis zu ihrem Ende laut TV-Guide aufgenommen, beginnt sie schon im Puffer, auch von ihrem Anfang an. Schaust du währenddessen einen anderen Sender, braucht das eine zweite Verbindung zu deinem Anbieter. Aufnahmen liegen im App-Ordner und werden beim Deinstallieren der App gelöscht.¦Para gravar: pressione o botão vermelho no player. O programa será gravado até o término indicado no guia. Assistir a outro canal simultaneamente consome uma conexão extra do provedor.
Noch keine Aufnahmen.¦Nenhuma gravação realizada ainda.
Stoppen¦Parar
Abspielen: Live TV > „Aufnahmen“.¦Para assistir: TV ao vivo > „Gravações“.
Interner Speicher¦Armazenamento interno
USB / Speicherkarte {#0}¦USB / Cartão de memória {#0}
frei¦livre
Verbindung beendet¦Conexão encerrada
Puffer beendet¦Buffer finalizado
Puffer-Adresse ungültig¦Endereço de buffer inválido
Mit anzeigen¦Exibir também
Nicht mehr mit anzeigen¦Não exibir mais
wird mit angezeigt¦também em exibição
„Mit anzeigen“ zeigt eine Playlist zusätzlich zur aktiven: Filme und Serien erscheinen zusammen (doppelte Filme nur einmal, gleiche Serien als eine Serie mit mehreren Quellen). In Filme und Serien filterst du über „Quelle“, im Live TV wechselst du die Quelle mit dem Knopf oben. Jede weitere Playlist braucht zusätzlich Speicher.¦„Exibir também“ mescla outra lista com a ativa: títulos duplicados são consolidados como fontes alternativas no mesmo filme ou série. Cada lista extra consome memória adicional.
Weitere Playlist wird geladen: {0}¦Carregando lista adicional: {0}
Zu wenig Speicher für eine weitere Playlist auf diesem Gerät.¦Memória insuficiente para carregar outra lista simultânea neste aparelho.
Kartengröße, Seitenleiste, Raster¦Tamanho dos cards, barra lateral, grade
Karten¦Cards
Beim Markieren einer Auswahl zeigt die Vorschau sofort die Wirkung, OK übernimmt sie. Schriftgröße und Farben findest du unter „Design“.¦Ao selecionar uma opção, a prévia atualiza na hora; OK confirma. Cores e tamanho de texto estão em „Aparência“.
Seitenleiste¦Barra lateral
Position¦Posição
Links¦Esquerda
Rechts¦Direita
Oben¦Topo
Immer mit Text¦Sempre com rótulos de texto
Aus: nur Symbole, die Texte erscheinen beim Hineingehen. An: Symbole und Texte sind immer zu sehen (braucht mehr Platz).¦Desligado: apenas ícones, o texto surge ao focar. Ligado: ícones e rótulos sempre visíveis.
Zurück bringt dich jederzeit in die Seitenleiste – auch wenn sie rechts oder oben steht.¦A tecla Voltar sempre retorna à barra lateral, mesmo posicionada no topo ou à direita.
Sport¦Esportes
Jetzt live¦Ao vivo agora
Gleich¦Em breve
Sportsender¦Canais esportivos
Keine Sportsender gefunden¦Nenhum canal esportivo encontrado
Sportsender werden an Kategorie und Namen erkannt (z. B. „Sport“, „DAZN“, „Eurosport“). Sport-Sendungen erscheinen, sobald der TV-Guide geladen ist.¦Canais de esporte são reconhecidos por nome e categoria (ex: „Sport“, „DAZN“, „ESPN“). Os eventos aparecem após o guia carregar.
TV-Guide wird noch geladen¦O guia de TV ainda está sendo carregado
Stalker-Portal¦Portal Stalker
Portal-Adresse (z. B. http://portal.tld:8080/c/)¦Endereço do portal (ex: http://portal.com:8080/c/)
MAC-Adresse (z. B. 00:1A:79:12:34:56)¦Endereço MAC (ex: 00:1A:79:12:34:56)
Sender und Filme werden geladen (Serien noch nicht). Die MAC-Adresse bekommst du von deinem Anbieter.¦Canais e filmes estão sendo carregados. O endereço MAC é fornecido pelo seu provedor.
Angaben unvollständig: Portal-Adresse und MAC-Adresse (z. B. 00:1A:79:12:34:56) nötig.¦Dados incompletos: informe o endereço do portal e o MAC (ex: 00:1A:79:12:34:56).
Das Portal antwortet nicht. Adresse prüfen.¦O portal não responde. Verifique o endereço digitado.
Das Portal lehnt diese MAC-Adresse ab. MAC und Abo beim Anbieter prüfen.¦O portal recusou este endereço MAC. Verifique com seu provedor.
Das Portal hat keine Sender geliefert. MAC-Adresse und Abo beim Anbieter prüfen.¦O portal não retornou canais. Verifique sua assinatura.
Filme werden geladen ({#0}) …¦Carregando filmes ({#0}) …
Datei vom Gerät¦Arquivo do aparelho
Auf diesem Gerät gibt es keine Dateiauswahl. Lade die Datei über die Handy-Seite rechts hoch.¦Este aparelho não possui seletor de arquivos. Envie o arquivo pelo navegador do celular à direita.
Das ist keine gültige M3U-Datei.¦Este arquivo não é uma lista M3U válida.
Die Playlist-Datei ist nicht mehr da. Bitte erneut hochladen.¦O arquivo da lista não foi encontrado. Faça o upload novamente.
Playlist wird gelesen …¦Lendo lista de reprodução …
Zugangsdaten eintragen oder eine M3U-Datei hochladen und senden. Die Playlist wird danach automatisch geladen.¦Insira seus dados de login ou envie um arquivo M3U. A lista carregará em seguida.
Auf ein anderes Gerät übertragen¦Transferir para outro aparelho
Verlauf, Weiterschauen, Favoriten, Listen und Einstellungen mitnehmen – z. B. vom Fire TV auf das Handy. Auf Wunsch auch die Playlists mit Zugangsdaten.¦Transfira histórico, favoritos, listas e configurações para outra TV, celular ou Fire TV.
Übertragung im WLAN starten¦Iniciar transferência via Wi-Fi
Übertragung beenden¦Parar transferência
Das Gerät ist nicht mit einem WLAN verbunden.¦O aparelho não está conectado à rede Wi-Fi.
Am Handy (im selben WLAN) öffnen:¦Abra no celular (no mesmo Wi-Fi):
Altes Gerät: Sicherung herunterladen. Neues Gerät: hier ebenfalls starten und die Datei einspielen. Danach startet die App neu.¦Aparelho antigo: baixe o backup. Aparelho novo: inicie aqui e envie o arquivo. O app reiniciará pronto.
Google Drive¦Google Drive
Mit Google verbinden¦Conectar com o Google
Am Handy öffnen und den Code eingeben:¦Abra no celular e digite o código:
Die App speichert nur in ihrem eigenen, versteckten Ordner in deinem Google Drive und sieht sonst nichts.¦O app salva apenas em sua pasta oculta dedicada no Google Drive sem acesso aos seus outros arquivos.
Nicht verbunden (abgelehnt oder Zeit abgelaufen).¦Não conectado (recusado ou tempo esgotado).
Verbunden.¦Conectado com sucesso.
Letzte Sicherung: {0}¦Último backup: {0}
Playlists mit Zugangsdaten mitsichern¦Incluir listas e dados de login no backup
Praktisch für ein neues Gerät. Die Zugangsdaten liegen dann in deinem Google Drive.¦Ideal para trocar de TV. Os logins ficarão salvos em seu Google Drive.
Jetzt in Drive sichern¦Fazer backup no Drive agora
Aus Drive wiederherstellen¦Restaurar do Drive
Trennen¦Desconectar
Sichern …¦Criando backup …
Gesichert.¦Backup concluído.
Laden …¦Carregando …
In Drive gibt es noch keine Sicherung.¦Nenhum backup encontrado no Google Drive.
Die Sicherung in Drive ist ungültig.¦O arquivo de backup no Drive é inválido.
Google antwortet nicht¦O Google não respondeu
Nicht mit Google verbunden¦Não conectado ao Google
Auf dem Fernseher lässt sich die Zwischenablage oft nicht nutzen: Unter Einstellungen > Speicher > „Übertragung im WLAN starten“ kannst du den Bericht am Handy öffnen (Seite „Letzten Absturzbericht anzeigen“) und dort kopieren.¦Em TVs a área de transferência nem sempre funciona: em Configurações > Armazenamento > „Iniciar transferência via Wi-Fi“ você pode abrir o relatório de erros no celular para copiar.
Auf dem Handy im Querformat¦Modo paisagem no celular
Die App ist für den Fernseher gebaut und wirkt quer am besten. Aus: dreht mit dem Gerät. Im Player: Tippen zeigt die Steuerung, Doppeltippen links/rechts spult, in der Mitte Pause.¦O app foi otimizado para TVs e funciona melhor na horizontal. Desligado: gira livremente.
Oder mit der Handy-Kamera scannen¦Ou escaneie com a câmera do celular
QR-Code scannen¦Escanear QR Code
Spende¦Doação
Hallo, ich bin der nigerianische Prinz und brauche dringend deine Hilfe! Spaß beiseite: Die App kostet dich nichts, mich aber ein paar Nächte Schlaf und sehr viel Kaffee. Wenn du mir ein paar Euro schicken möchtest, tanzt mein Kontostand vor Freude. Kein Zwang: Die App bleibt kostenlos, werbefrei und GPL-lizenziert.¦Olá! Brincadeiras à parte: este app não cobra nada, mas custou muitas noites de sono e litros de café. Se você quiser enviar uma contribuição, ficarei muito grato! Mas sem pressão: o app continuará 100% gratuito, sem propagandas e sob licença GPL.
Mit der Handy-Kamera scannen¦Escanear com a câmera do celular
Folgen werden geladen …¦Carregando episódios …
Die Folgen konnten nicht geladen werden. Verbindung oder Zugang prüfen.¦Não foi possível carregar os episódios. Verifique a conexão ou sua assinatura.
Dieser QR-Code ist der PayPal-Link für meine Spende. Mit der Handy-Kamera scannen, dann öffnet sich PayPal.¦Este QR Code abre o link de doação no PayPal ao ser escaneado com o celular.
Unterstütze IPVIO Stream¦Apoie o IPVIO Stream
Raster & Ansicht¦Grade e visualização
American Football¦Futebol Americano
Basketball¦Basquete
Eishockey¦Hóquei no Gelo
Fußball¦Futebol
Tennis¦Tênis
Motorsport¦Automobilismo
Kampfsport¦Artes Marciais / Lutas
Golf¦Golfe
Handball & Volleyball¦Handebol e Vôlei
Rugby & Cricket¦Rugby e Críquete
Radsport¦Ciclismo
Wintersport¦Esportes de Inverno
Darts & Snooker¦Dardos e Sinuca
Nächste 3 Std.¦Próximas 3 horas
Morgen¦Amanhã
Keine Sendungen für diese Auswahl¦Nenhuma transmissão para esta seleção
Zusatzbild¦Tela adicional
Anordnung¦Disposição da tela
Bild-in-Bild¦Picture-in-Picture (PiP)
Geteilt¦Tela dividida
Ton von¦Áudio de
Hauptstream¦Transmissão principal
Streams tauschen¦Inverter telas
Dieser Sender ist beim Anbieter gerade nicht erreichbar. Probiere einen anderen.¦Este canal está inacessível no provedor no momento. Escolha outro.
Dein Anbieter erlaubt vermutlich nur eine Verbindung gleichzeitig. Frage nach einem Zugang mit mehr Verbindungen.¦Seu provedor provavelmente permite apenas 1 tela simultânea.
Die Verbindung ist zu langsam oder wurde abgelehnt. Ein Sender in niedrigerer Qualität hilft oft.¦A conexão está muito lenta ou foi recusada. Uma qualidade menor pode ajudar.
Dein Gerät schafft zwei Videos gleichzeitig nicht. Wähle für beide Bilder SD-Sender.¦Seu aparelho não suporta dois vídeos simultâneos. Selecione canais em SD.
Probiere einen anderen Sender oder eine niedrigere Qualität.¦Tente outro canal ou selecione uma resolução mais baixa.
Schon ein anderes Gerät mit IPVIO Stream? Spiele die Sicherung ein: Playlists, Zugangsdaten, Verlauf, Favoriten und Einstellungen sind danach sofort da.¦Já usa em outro dispositivo? Restaure o backup: listas, favoritos e configurações voltam na hora.
Sicherung von anderem Gerät einspielen¦Restaurar backup de outro aparelho
Keine gültige Sicherung gefunden.¦Nenhum backup válido encontrado.
Auf diesem Gerät gibt es keine Dateiauswahl. Spiele die Sicherung über die Handy-Seite rechts ein.¦Este aparelho não tem gerenciador de arquivos. Envie o backup pelo celular à direita.
Sicherung mit Playlists speichern …¦Salvar backup completo com listas …
Auf diesem Gerät gibt es keine Dateiauswahl. Nutze die Übertragung im WLAN.¦Este aparelho não tem explorador de arquivos. Use a transferência via Wi-Fi.
Gesichert werden Einstellungen, Favoriten, Verlauf, Weiterschauen und Profile. Playlists und Zugangsdaten sind nur in „Sicherung mit Playlists“ enthalten (die Datei enthält Passwörter, gib sie nicht weiter). Beim Wiederherstellen startet die App neu. Auf einem neuen Gerät geht das gleich beim Hinzufügen der ersten Playlist.¦Configurações, favoritos, histórico e perfis são incluídos. Senhas de acesso só entram no backup completo (não compartilhe o arquivo).
Wichtig zu wissen¦Importante saber
IPVIO Stream ist nur ein Abspielgerät. Die App liefert selbst keine Sender, Filme, Serien und keine Playlist. Den Link oder die Zugangsdaten bekommst du von deinem IPTV-Anbieter oder aus deiner eigenen Quelle. Wir können dir keinen Link nennen.¦O IPVIO Stream é apenas um reprodutor de mídia. O app não disponibiliza nem vende canais, listas ou conteúdos. Você deve utilizar seus próprios acessos.
OK = groß¦OK = tela cheia
Handy und dieses Gerät müssen im selben WLAN sein. Halte die Kamera auf das Bild. Es klappt auch aus einiger Entfernung.¦Celular e TV devem estar no mesmo Wi-Fi. Aponte a câmera para o QR Code na tela.
Oder Adresse im Browser eintippen:¦Ou acesse este endereço no navegador:
Zurück oder OK schließt das Bild.¦Voltar ou OK fecha a imagem.
Leiste nach unten¦Barra para baixo
Leiste nach oben¦Barra para cima
Texte¦Rótulos
Immer zeigen¦Sempre visíveis
Nur Symbole¦Apenas ícones
Beim Markieren¦Ao focar
Bild¦Imagem
Kartenbreite¦Largura dos cards
Spalten und Zeilen gelten nur in der Ansicht „Raster“.¦Colunas e linhas se aplicam apenas ao modo „Grade“.
Zeigt die echte App mit deinen Titeln; die Einstellungen erscheinen in einer schmalen Leiste unten. Ein Klick auf eine Auswahl wirkt sofort.¦Exibe o app real com seus conteúdos; a barra de ajustes fica na parte inferior com efeito instantâneo.
Zwei gleich groß¦Duas metades iguais
Einer groß, einer klein¦Uma grande e uma pequena
So sieht eine Karte aus: normal und markiert¦Visual do card: normal e destacado
Score-Overlay¦Placar flutuante
Anzeige¦Exibição
Untereinander¦Empilhados
Nebeneinander¦Lado a lado
In den Ecken verteilt¦Distribuídos nos cantos
Ausgewählte Spiele entfernen¦Remover jogos selecionados
Zusatzbild nicht abspielbar¦Não foi possível reproduzir a tela secundária
Alle Playlists¦Todas as listas
Fenstergröße¦Tamanho da janela
Gilt für Bild-in-Bild. Deine Auswahl wird gespeichert.¦Aplica-se ao modo Picture-in-Picture. Sua escolha fica salva.
Handy-Suche¦Busca pelo celular
Der Server konnte nicht gestartet werden.¦Não foi possível iniciar o servidor local.
Suche und starte Filme, Serien und Sender am Handy. Handy und Fernseher müssen im selben WLAN sein.¦Busque e reproduza conteúdos pelo celular conectado no mesmo Wi-Fi.
Filme, Serien, Sender suchen …¦Buscar filmes, séries e canais …
Pause / Weiter¦Pausar / Reproduzir
Leiser¦Vol -
Lauter¦Vol +
Code vom Fernseher:¦Código da TV:
Tippe einen Titel ein.¦Digite o nome de um título.
Läuft auf dem Fernseher.¦Em exibição na TV.
Fehler. Ist der Fernseher noch an?¦Erro. A TV continua ligada?
Es läuft nichts.¦Nada em reprodução no momento.
Die Playlist wird noch geladen.¦A lista de canais ainda está carregando.
Nichts gefunden.¦Nenhum resultado encontrado.
Folge¦Episódio
Lädt …¦Carregando …
Stopp¦Parar
Beenden¦Sair
Fertige Designs¦Temas prontos
Ein Klick stellt Farbe und Hintergrund passend ein. Darunter kannst du alles weiter von Hand ändern.¦Aplica uma combinação equilibrada de cores e fundo com apenas um clique.
Bildschirm anpassen¦Ajustar bordas da tela
Uhr anzeigen¦Mostrar relógio
Zeigt die Uhrzeit oben neben dem Profil-Symbol.¦Exibe o relógio no topo ao lado do ícone de perfil.
Rand links und rechts¦Margem esquerda e direita
Rand oben und unten¦Margem superior e inferior
Wenn am Fernseher Menü oder Texte am Rand abgeschnitten werden, stelle hier einen Rand ein. Der Hintergrund bleibt vollflächig, der Player nutzt immer den ganzen Bildschirm.¦Se os menus ficarem cortados na borda da sua TV, configure uma margem de segurança aqui.
Leer¦Espaço
Löschen¦Apagar
Alles¦Limpar tudo
Gestern¦Ontem
Heute¦Hoje
Beendet¦Encerrado
Auf¦No canal
Nicht in deinen Sendern¦Fora da sua lista de canais
Kein Sender in deiner Liste gefunden¦Nenhum canal correspondente na sua lista
Live-Ergebnisse: An¦Resultados ao vivo: Ligado
Live-Ergebnisse: Aus¦Resultados ao vivo: Desligado
Jetzt live auf deinen Sendern¦Ao vivo agora nos seus canais
Ergebnisse werden geladen …¦Carregando placares …
Ergebnisse gerade nicht erreichbar – es werden nur Playlist und TV-Guide gezeigt.¦Placares inacessíveis no momento – exibindo dados da lista e guia de TV.
Filme¦Filmes
Dokumentationen¦Documentários
Kinder¦Infantil
Nachrichten¦Notícias
Musik¦Música
Start¦Início
Zuletzt gesehen¦Assistidos recentemente
noch¦restam
▲▼ Sender  ·  OK Steuerung  ·  OK halten: mehr¦▲▼ Canais  ·  OK Controles  ·  Segurar OK: Mais
Meine Teams¦Meus times
Teams merken¦Salvar times favoritos
OK auf einem Team merkt es oder entfernt es wieder. Es erscheinen die Teams der Spiele des gewählten Tages.¦Pressione OK sobre um time para adicioná-lo ou removê-lo dos favoritos.
Hinweise: An¦Notificações: Ligado
Hinweise: Aus¦Notificações: Desligado
Verzögerung¦Atraso
Dein Team spielt gleich¦Seu time joga em breve
Dein Team spielt jetzt¦Seu time está jogando agora
Punkte!¦Ponto / Gol!
Mini-Spielstand¦Mini placar
Ein kleines Fenster in der Ecke zeigt den Stand, solange eines deiner Teams spielt – auch beim Film oder auf einem anderen Sender.¦Uma pequena caixa no canto mostra o placar enquanto seu time joga, mesmo assistindo a outros canais ou filmes.
Fenster: An¦Janela: Ligada
Fenster: Aus¦Janela: Desligada
Größe¦Tamanho
Klein¦Pequeno
Normal¦Normal
Groß¦Grande
Logos und Namen¦Escudos e nomes
Nur Logos¦Apenas escudos
Spielzeit: An¦Tempo de jogo: Ligado
Spielzeit: Aus¦Tempo de jogo: Desligado
Tabelle¦Classificação
Schließen¦Fechar
Keine Tabelle verfügbar.¦Classificação indisponível.
Lade…¦Carregando…
Ausgeblendete¦Ocultos
Alle wieder einblenden¦Mostrar todos novamente
Kategorie ausblenden¦Ocultar categoria
Gruppen ignorieren¦Ignorar grupos
Alle wieder einschalten¦Reativar todos
Ignoriert¦Ignorado
Titel und Sender sind nach der Kennung deines Anbieters gebündelt (z. B. „TR |“, „[EN]“, „AR:“). Schalte Bündel aus, die du nicht brauchst: Sie verschwinden sofort überall, ohne die Playlist neu zu laden. Ausgeschaltet heißt ignoriert. Hilft dort, wo der Sprachfilter eine Kennung nicht erkennt.¦Filtre pacotes pela identificação do provedor (ex: „BR |“, „[PT]“). Grupos desativados somem de todo o app sem precisar recarregar a lista.
Kalender¦Calendário
Ligen¦Ligas
Abgewählte Ligen erscheinen nicht und werden nicht abgefragt.¦Ligas desmarcadas não são exibidas nem consultadas.
Live-Ergebnisse sind aus. Schalte sie oben ein, dann erscheint der Kalender.¦Os resultados ao vivo estão desativados. Ative-os acima para exibir o calendário.
Du hast noch keine Teams gemerkt. Unter „Meine Teams“ kannst du Teams merken.¦Nenhum time salvo. Selecione seus times em „Meus times“.
Kalender wird geladen …¦Carregando calendário de jogos …
In den nächsten 7 Tagen spielt keines deiner Teams.¦Nenhum dos seus times tem jogos marcados nos próximos 7 dias.
Spiele deiner Teams in den nächsten 7 Tagen¦Jogos dos seus times nos próximos 7 dias
Sicherung speichern unter …¦Salvar backup como …
Sicherung aus Datei laden …¦Carregar backup de arquivo …
Sicherung gespeichert.¦Backup salvo com sucesso.
Auf diesem Gerät gibt es keine Dateiauswahl. Nutze „Sicherung erstellen“.¦Este aparelho não tem seletor de arquivos. Use „Fazer backup“.
Auf diesem Gerät gibt es keine Dateiauswahl.¦Este aparelho não possui seletor de arquivos.
„Speichern unter“ lässt dich den Ort wählen, zum Beispiel Google Drive, Downloads oder einen USB-Stick – so nimmst du die Sicherung auf ein anderes Gerät mit.¦„Salvar como“ permite escolher o destino (pen drive, pasta de downloads ou nuvem) para levar para outro dispositivo.
Übersicht¦Visão geral
Spielplan¦Calendário / Rodadas
Teams¦Times
Top-Spiel¦Destaque da rodada
Demnächst¦Próximos
Beliebte Ligen¦Ligas populares
Sportarten¦Modalidades esportivas
Verschoben¦Adiado
Abgesagt¦Cancelado
Statistik¦Estatísticas
Aufstellung¦Escalação
Ereignisse¦Lances
Kommentar¦Comentários
Bank¦Banco de reservas
Tore¦Gols
Zuschauer¦Público
Sender starten¦Iniciar canal
▶ Auf deinen Sendern¦▶ Nos seus canais
Lade Spiele …¦Carregando partidas …
Heute keine Spiele gefunden.¦Nenhum jogo encontrado para hoje.
Keine Spiele für diese Auswahl.¦Nenhum jogo para esta seleção.
Dieser Bereich folgt im nächsten Schritt.¦Esta área estará disponível em breve.
Lade Details …¦Carregando detalhes …
Keine Details verfügbar.¦Detalhes indisponíveis.
Keine Liga aktiv.¦Nenhuma liga selecionada.
Lade Tabelle …¦Carregando tabela de classificação …
Für diese Liga gibt es keine Tabelle.¦Não há tabela de classificação para esta liga.
Lade Teams …¦Carregando times …
Keine Teams gefunden.¦Nenhum time encontrado.
Nächste Spiele¦Próximos jogos
Letzte Spiele¦Últimos jogos
Nächstes Spiel¦Próximo jogo
Letztes Spiel¦Último jogo
Kader¦Elenco
Trainer¦Técnico
Spiele¦Jogos
Team merken¦Favoritar time
Nicht mehr merken¦Remover dos favoritos
Meine Ligen¦Minhas ligas
Meine Sportarten¦Meus esportes
Heute für dich¦Hoje para você
Heute keine Spiele deiner Favoriten.¦Seus times favoritos não jogam hoje.
Noch keine Teams gemerkt. Im Team Center gibt es „Team merken“.¦Nenhum time favoritado. Use „Favoritar time“ na página da equipe.
Design und Farbe¦Aparência e cores
Berechtigung erteilt. Der Autostart ist bereit.¦Permissão concedida. Início automático pronto.
Berechtigung fehlt noch. Bitte „Einstellung öffnen“ wählen und IPVIO Stream erlauben.¦Permissão pendente. Selecione „Abrir configurações“ e autorize o IPVIO Stream.
Hauptliste¦lista principal
zusätzlich angezeigt¦exibida junto
Nicht mehr zusätzlich anzeigen¦Não exibir mais simultaneamente
Zusätzlich anzeigen¦Exibir também
Zur Hauptliste machen¦Definir como lista principal
Die Hauptliste ist die Playlist, mit der die App arbeitet. „Zusätzlich anzeigen“ blendet eine weitere Playlist dazu ein; sie ersetzt die Hauptliste nicht.¦A lista principal é a fonte primária do app. „Exibir também“ adiciona uma lista complementar sem substituí-la.
Sender suchen¦Buscar canal
Sendername eingeben¦Digite o nome do canal
OK auf dem Feld öffnet die Tastatur. Gesucht wird im Sendernamen und in der Kategorie.¦Pressione OK para abrir o teclado. A busca pesquisa no nome e na categoria.
Im TV-Guide suchen¦Buscar no guia de programação
Sender oder Sendung¦Canal ou programa
Suche¦Buscar
Mit dem Handy suchen¦Buscar pelo celular
Handy¦Celular
Zuletzt gesucht (OK lange gedrückt = Eintrag entfernen)¦Buscas recentes (segurar OK = remover item)
Zuletzt gesucht¦Buscas recentes
Suche nach einem Titel: hier erscheinen Beschreibung, Bewertung und Format des markierten Treffers.¦Busque por um título: sinopse, avaliação e formato aparecerão aqui ao focar no resultado.
SportsHub¦Sports Hub
Sports Hub öffnen¦Abrir Sports Hub
Sender-Qualitäten bündeln¦Agrupar qualidades do mesmo canal
Derselbe Sender in 8K, 4K, FHD, HD und SD erscheint als eine Zeile. OK startet die beste Qualität, die anderen wählst du im Menü des Senders (OK gedrückt halten).¦Canais idênticos em 4K, FHD, HD e SD viram uma única linha. OK abre na melhor qualidade; segure OK para escolher outra resolução.
Qualität¦Qualidade
Sprache¦Idioma
Ohne Kennung¦Sem tag
Rand: „Schatten“ legt einen weichen dunklen Schatten unter die Schrift, „Kontur“ zeichnet einen schwarzen Rand rundherum (besser lesbar auf hellem Bild), „Kein“ lässt die Schrift pur.¦Borda: „Sombra“ aplica sombra suave, „Contorno“ desenha linha preta ao redor (ótimo para fundos claros), „Nenhum“ mantém o texto limpo.
Bildschirmrand einstellen¦Calibrar bordas da tela
Zeigt einen Rahmen mit vier Ecken am Fernseher: Stelle den Rand ein, bis alle vier Ecken sichtbar sind. Hilft, wenn dein Fernseher Menü oder Texte am Rand abschneidet.¦Exibe marcadores nos quatro cantos da TV: ajuste até visualizar todos os cantos com clareza.
Du siehst einen weißen Rahmen mit vier grünen Punkten. Wenn eine Ecke fehlt, schneidet dein Fernseher den Rand ab. Rechts/Links ändert den Rand seitlich, Hoch/Runter oben und unten, bis alle vier Ecken sichtbar sind. OK speichert.¦Você vê uma moldura com pontos verdes nos cantos. Ajuste as setas laterais e verticais até ver todos os pontos. Pressione OK para salvar.
Seitlich¦Laterais
Oben/unten¦Superior/inferior
Wiedergabe¦Reprodução
Intro und nächste Folge¦Aberturas e próximo episódio
Uhr im Bild¦Relógio na tela
Netzwerk und Bildrate¦Rede e taxa de quadros
Liste importiert¦Lista importada
Titel gelesen¦títulos lidos
in deiner Playlist gefunden¦encontrados na sua lista
Mehrfachansicht¦Multi-telas
Vier gleich groß¦Quatro telas iguais
Einer groß, drei klein¦Uma tela principal e três menores
OK = Ton hierher, OK lange = Sender wechseln¦OK = ativar áudio nesta tela, Segurar OK = mudar de canal
So sieht die Reihenfolge aus¦Esta é a ordem configurada
Nichts gezeigt¦Nada exibido
Listen importieren¦Importar listas externas
Letterboxd, MyAnimeList, Trakt¦Letterboxd, MyAnimeList, Trakt
Hole deine Listen von Letterboxd, MyAnimeList oder Trakt in die App. Die Titel werden mit deiner Playlist abgeglichen: Merkliste und Gesehen werden übernommen.¦Importe seus filmes e animes do Letterboxd, MyAnimeList ou Trakt e cruze com sua lista de IPTV.
Letterboxd: ZIP oder CSV · MyAnimeList: XML · Trakt: JSON¦Letterboxd: ZIP ou CSV · MyAnimeList: XML · Trakt: JSON
Dieses Gerät hat keinen sicheren Schlüsselspeicher: Playlist-Adressen mit Zugangsdaten werden unverschlüsselt gespeichert.¦Este aparelho não possui keystore seguro: dados de lista serão salvos em texto simples.
Datei vom Gerät wählen¦Escolher arquivo do aparelho
Auf diesem Gerät gibt es keine Dateiauswahl. Nutze den Import im WLAN.¦Este aparelho não tem gerenciador de arquivos. Use o envio pelo celular via Wi-Fi.
Import im WLAN starten¦Iniciar importação via Wi-Fi
Import beenden¦Encerrar importação
Am Handy „Liste importieren“ wählen, den Code eingeben und die Exportdatei hochladen.¦No navegador do celular selecione „Importar lista“, digite o código e faça o upload.
Direkt auf der Startseite bearbeiten¦Editar direto na tela inicial
Öffnet die echte Startseite mit deinen Titeln. Über jeder Reihe erscheint eine Leiste zum Verschieben und Ein- oder Ausblenden.¦Abre a tela inicial permitindo organizar, subir, descer ou ocultar qualquer fileira.
Oder hier in der Liste: Schalte Reihen und Widgets ein oder aus und schiebe sie mit „Hoch“ und „Runter“ an die Stelle, die du möchtest. Die Änderung gilt sofort.¦Ou organize por aqui: ative/desative seções e posicione usando „Mover para cima“ ou „Mover para baixo“.
Immer oben¦Fixar no topo
Ausblenden¦Ocultar
Einblenden¦Exibir
Fertig¦Concluído
Bearbeiten: Über jeder Reihe kannst du sie verschieben oder ausblenden.¦Edição: controle cada fileira pelos botões superiores.
Auf der echten Startseite ansehen¦Ver na tela inicial real
Live-Vorschau¦Pré-visualização em tempo real
Eine Auswahl wirkt sofort, ohne OK.¦As escolhas entram em vigor na hora.
Weiter nach unten wechselt den Bereich, die App dahinter springt mit.¦Navegar para baixo acompanha o aplicativo ao fundo.
Raster: Wie viele Spalten und Zeilen die Rasteransicht (zum Beispiel „Alle Filme“) zeigt.¦Grade: define a quantidade de colunas e linhas da exibição.
Score-Overlay: An¦Placar flutuante: Ligado
Score-Overlay: Aus¦Placar flutuante: Desligado
Setzt Ansicht, Menü, Player, Ton und Start auf die Standardwerte. Playlists, Favoriten und Verlauf bleiben erhalten. Das Design hat einen eigenen Knopf.¦Restaura menus, player e controles para o padrão sem apagar listas, favoritos ou histórico.
Live-Vorschau starten¦Iniciar pré-visualização em tempo real
Zeigt die echte App mit deinen Titeln; unten erscheinen die Einstellungen. Jede Auswahl wirkt sofort, ohne OK.¦Exibe o app com seus conteúdos e controles na barra inferior com aplicação imediata.
Letterboxd · Gesehen¦Letterboxd · Assistidos
Letterboxd · Merkliste¦Letterboxd · Quero assistir
Trakt · Gesehen¦Trakt · Assistidos
Trakt · Merkliste¦Trakt · Quero assistir
MyAnimeList · Gesehen¦MyAnimeList · Assistidos
MyAnimeList · Merkliste¦MyAnimeList · Quero assistir
1 Spiel¦1 jogo
{#0} Spiele¦{#0} jogos
Spiel¦Partida
Formel 1¦Fórmula 1
1. Training¦Treino Livre 1
2. Training¦Treino Livre 2
3. Training¦Treino Livre 3
Qualifying¦Classificação
Sprint-Qualifying¦Classificação Sprint
Sprint¦Corrida Sprint
Rennen¦Corrida Principal
Aktuelle Position¦Posição atual
Alle Ligen¦Todas as ligas
Alle Wettbewerbe¦Todas as competições
Als Favorit merken¦Adicionar aos favoritos
Favorit¦Favorito
Anderen Sender wählen¦Escolher outro canal
Sender zuordnen¦Vincular canal
Automatisch zuordnen¦Vincular automaticamente
Auf deinen Sendern¦Nos seus canais
Live ansehen¦Assistir ao vivo
Kein passender Sender gefunden¦Nenhum canal correspondente encontrado
ESPN liefert keine Streams. Gesucht wird in deinem TV-Guide und in deinen Sendernamen. Du kannst den Sender auch selbst zuordnen; die App merkt sich das für dieses Spiel.¦A ESPN fornece apenas dados de placar, não os vídeos. O app busca em seus canais e no guia. Você também pode associar manualmente.
Von dir zugeordnet¦Vinculado por você
Laut TV-Guide¦Conforme guia de TV
Sendername passt¦Nome do canal correspondente
Verfügbar¦Disponível
Keine Sender in der Playlist.¦Nenhum canal na lista.
Match Center¦Match Center
Daten konnten nicht geladen werden¦Não foi possível carregar os dados
Die Daten konnten nicht geladen werden. Bitte versuche es erneut.¦Falha ao carregar informações. Tente novamente.
ESPN antwortet gerade nicht. Bitte versuche es erneut.¦A ESPN não respondeu. Tente novamente.
ESPN antwortet gerade nicht. Prüfe die Internetverbindung und versuche es erneut.¦Sem resposta da ESPN. Verifique sua conexão à internet e tente novamente.
ESPN hat für dieses Team gerade keine Daten geliefert.¦Não há dados disponíveis para este time no momento.
ESPN liefert für diesen Spieler keine Daten.¦Não há dados disponíveis para este jogador.
Keine Spielerdaten¦Sem dados do jogador
Ergebnisse von heute¦Resultados de hoje
Ergebnisse¦Resultados
Letzte Ergebnisse¦Últimos resultados
Keine Ergebnisse¦Sem resultados
Noch keine Ergebnisse¦Nenhum resultado ainda
Live-Ergebnisse sind aus¦Resultados ao vivo desligados
Ergebnisse, Tabellen und Spielpläne kommen von ESPN (inoffizielle Quelle). Schalte sie ein, um den Sports Hub zu nutzen.¦Placares e tabelas são obtidos via ESPN (fonte pública não oficial). Ative para usar o Sports Hub.
Einschalten¦Ativar
Gerade läuft nichts¦Nenhum evento ao vivo agora
Sobald ein Spiel deiner Ligen beginnt, erscheint es hier.¦Assim que uma partida das suas ligas começar, ela aparecerá aqui.
Ändere die Filter oder schau in den Spielplan.¦Ajuste os filtros ou confira o calendário de jogos.
Spielplan öffnen¦Abrir calendário de partidas
In deinen Ligen ist heute nichts angesetzt. Im Spielplan siehst du die nächsten Tage.¦Nenhum jogo programado para as suas ligas hoje. Consulte as próximas datas.
An diesem Tag ist in deinen Ligen nichts angesetzt. Tennis, Formel 1, UFC, Golf und Cricket zeigt ESPN nur für die laufenden Turniere und Wochenenden.¦Nenhuma partida agendada para este dia nas suas ligas.
Hinweis: Die letzte Aktualisierung hat nicht geklappt. Angezeigt wird der letzte Stand.¦Aviso: a última atualização falhou. Exibindo os dados salvos anteriormente.
In den nächsten Tagen keine Spiele deiner Favoriten.¦Sem jogos dos seus times favoritos nos próximos dias.
Kein Spiel geplant¦Nenhum jogo programado
Keine Spiele geplant¦Nenhum jogo programado
Heute keine Spiele.¦Nenhum jogo hoje.
Gerade keine Ereignisse.¦Nenhum lance no momento.
In diesem Zeitraum liefert ESPN keine Spiele.¦A ESPN não possui dados para este intervalo de datas.
Ligen schaltest du im Reiter „Sender“ unter „Ligen“ ein.¦Ative ligas na aba „Canais“ em „Ligas“.
Lade Daten …¦Carregando dados …
Meine Favoriten¦Meus favoritos
Merke dir Teams, Wettbewerbe, Spieler oder Sportarten. Sie erscheinen hier mit ihren Spielen.¦Favorite equipes, campeonatos ou esportes para acompanhá-los de perto.
Noch keine Favoriten¦Nenhum favorito ainda
Sport entdecken¦Explorar esportes
Nur Favoriten¦Apenas favoritos
OK auf eine Sportart merkt sie oder entfernt sie wieder.¦Pressione OK sobre uma modalidade para favoritar ou remover.
Wichtige Ereignisse¦Principais lances
Spielbericht¦Resumo da partida
Direktvergleich¦Confronto direto (H2H)
Form¦Desempenho recente
Klassement¦Classificação
Kampfabend¦Card de lutas
Spieler¦Jogadores
Startelf¦Titulares
Ersatzbank¦Reservas
eingewechselt¦entrou
ausgewechselt¦saiu
Satz¦Set
Sätze¦Sets
Spitze¦Líderes
Torjäger¦Artilheiros
Topspieler¦Destaques
Wettbewerbe¦Competições
Aktuell¦Atual
Geboren¦Nascimento
Gewicht¦Peso
Nation¦Nacionalidade
Nummer¦Número
Team¦Time
Beginn: {0}¦Início: {0}
{0} führt {1} gegen {2}.¦{0} lidera por {1} contra {2}.
{0} gewinnt {1} gegen {2}.¦{0} vence por {1} a {2}.
Es steht {0} zwischen {1} und {2}.¦Placar de {0} entre {1} e {2}.
{0} und {1} trennen sich {2}.¦{0} e {1} empatam em {2}.
Pkt¦Pts
Sp¦J
S¦V
U¦E
N¦D
TD¦SG
OTN¦DP
Serie¦Sequência
J.¦anos
Am Handy eingeben¦Digitar no celular
Schlüssel vom Handy erhalten und gespeichert. Jetzt „Speichern und prüfen“ drücken.¦Chave recebida do celular e salva com sucesso. Clique em „Salvar e testar“.
Eingabe am Handy beenden¦Concluir digitação pelo celular
Untertitel online suchen¦Buscar legendas online
Kostenlos unter subdl.com > Konto > API. Im Player unter „Audio & Untertitel“ > „Untertitel online suchen“ werden beide Dienste gefragt.¦Obtenha uma chave grátis em subdl.com > Conta > API. No player em „Áudio e legendas“ > „Buscar legendas online“ ambos os serviços serão consultados.
Keine Untertitel in den gewünschten Sprachen¦Nenhuma legenda encontrada nos idiomas solicitados
Kein Schlüssel für die Untertitel-Suche. Einstellungen > API-Schlüssel¦Nenhuma chave de API para busca de legendas. Vá em Configurações > Chaves de API
"""

package com.ipvio.stream

// Simplified Chinese translations: German-Chinese rows, added as an extra language column.
val DictZh = """
Playlist hinzufügen¦添加播放列表
Dauert nur ein paar Sekunden.¦只需几秒钟。
Aus Zwischenablage einfügen¦从剪贴板粘贴
Server (z. B. http://server.tld:8080)¦服务器（例如 http://server.tld:8080）
Benutzername¦用户名
Passwort¦密码
Passwort verbergen¦隐藏密码
Passwort anzeigen¦显示密码
Backup-Server {#0} (optional)¦备用服务器 {#0}（可选）
+ Backup-Server¦+ 备用服务器
Fällt der Server aus, wechselt die App von selbst zu einem Backup-Server. Anbieter nennen sie oft als weitere Server- oder DNS-Adressen; Benutzername und Passwort bleiben gleich.¦主服务器故障时，应用会自动切换到备用服务器。服务商通常把它列为额外的服务器或 DNS 地址；用户名和密码不变。
Backup-Server ungültig: Adresse wie http://server.tld:8080 eingeben.¦备用服务器地址无效：请输入类似 http://server.tld:8080 的地址。
Backup-Server¦备用服务器
Backup-Server: {#0}¦备用服务器：{#0}
Backup-Server speichern¦保存备用服务器
Erreichbar¦可连接
Nicht erreichbar seit {0}¦自 {0} 起无法连接
Nicht erreichbar: {0}¦无法连接：{0}
Hauptserver: {0}¦主服务器：{0}
Hinzufügen¦添加
Abbrechen¦取消
Alle¦全部
Alle anzeigen¦显示全部
Neu hinzugefügt¦最近添加
Beliebtheit¦热度
Playlist¦播放列表
vor 1990¦1990 年以前
{#0} Titel¦{#0} 个节目
{0} werden geladen … {#1} / {#2}¦正在加载 {0} … {#1} / {#2}
{0} laden … {#1}/{#2}¦正在加载 {0} … {#1}/{#2}
Metadaten¦元数据
Länder¦国家/地区
Zurück = Filter¦返回键 = 筛选
Sortierung¦排序
Rating¦评分
Jahr¦年份
Genre¦类型
Land¦国家
Zurücksetzen¦重置
Raster¦网格
Kino¦影院
Ansicht¦视图
Keine Treffer – Filter zurücksetzen.¦没有结果 —— 请重置筛选条件。
Liste¦列表
Besetzung¦演员
Regie¦导演
Erstellt von¦创作者
Schauspiel¦表演
Drehbuch¦编剧
geboren {0}¦生于 {0}
Weniger¦收起
Mehr lesen¦展开更多
Infos konnten nicht geladen werden.¦无法加载信息。
Keine Titel in deiner Playlist¦播放列表中没有内容
In deiner Playlist¦已在播放列表中
{#0} weitere Titel dieser Person sind nicht in deiner Playlist.¦此人的另外 {#0} 部作品不在你的播放列表中。
Lade …¦加载中 …
Zuletzt geschaut¦最近观看
Meine Liste¦我的列表
In Meiner Liste¦已在我的列表
Filter¦筛选
Standard¦默认
Kacheln¦磁贴
{#0} Kategorien¦{#0} 个分类
Kategorien¦分类
Sonstige¦其他
Anime¦动漫
Animation¦动画
Film¦电影
{#0} Staffel(n)¦{#0} 季
{#0} Min¦{#0} 分钟
bei {#0} Min¦第 {#0} 分钟处
TMDB antwortet mit Fehler {#0}¦TMDB 返回错误 {#0}
Keine Verbindung zu TMDB ({0})¦无法连接 TMDB（{0}）
Besetzung nicht lesbar ({0})¦无法读取演员信息（{0}）
Besetzung nicht verfügbar: {0}¦演员信息不可用：{0}
Keine App zum Öffnen des Trailers gefunden¦未找到可播放预告片的应用
Externer Player¦外部播放器
Kein externer Player installiert (z. B. VLC)¦未安装外部播放器（如 VLC）
Keine Beschreibung gefunden.¦未找到简介。
Tipp: TMDB-Schlüssel in secrets.properties eintragen, dann erscheinen hier Beschreibung, Cover und Bewertungen.¦提示：在 secrets.properties 中填入你的 TMDB 密钥，即可在此显示简介、封面和评分。
Fortsetzen {0}¦继续播放 {0}
Von vorn¦从头播放
Abspielen¦播放
Trailer¦预告片
Extern¦外部
Automatisch (beste)¦自动（最佳）
Qualität¦画质
Automatisch¦自动
Sprache¦语言
Quelle¦来源
gilt für alle Folgen¦适用于所有分集
Staffeln¦季
Staffel {#0}¦第 {#0} 季
Episoden¦分集
lade Infos …¦正在加载信息 …
Links/Rechts = Staffel wechseln¦左/右方向键 = 切换季
Ähnliche Titel in deiner Playlist¦播放列表中的相似内容
Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert.¦本产品使用 TMDB API，但未获得 TMDB 认可或认证。
Raster anpassen¦调整网格
Spalten¦列数
Zeilen¦行数
Die Ansicht dahinter ändert sich sofort. Zurück schließt.¦背后的视图会立即变化，按返回键关闭。
Keine EPG-Adresse gefunden¦未找到 EPG 地址
Playlist enthält keine Sender-IDs (tvg-id)¦播放列表不含频道 ID（tvg-id）
Keine Programmdaten erhalten ({0})¦未收到节目单数据（{0}）
Geladen: {#0} Sender mit Programm¦已加载：{#0} 个带节目单的频道
Noch nicht geladen¦尚未加载
Wird geladen …¦加载中 …
Intro überspringen¦跳过片头
Rückblick überspringen¦跳过前情回顾
Abspann überspringen¦跳过片尾
Titel-Infos¦影片信息
Cover, Beschreibungen, Bewertungen¦封面、简介、评分
Schauspieler, Regie, ähnliche Titel¦演员、导演、相似作品
Playlist-Zwischenspeicher¦播放列表缓存
Schneller Start ohne Wartezeit¦快速启动，无需等待
Bilder¦图片
Zwischengespeicherte Cover und Hintergründe¦已缓存的封面和背景图
Noch einmal Zurück drücken zum Beenden¦再按一次返回键退出
Die Playlist konnte nicht geladen werden¦播放列表加载失败
Noch keine Sender¦暂无频道
Prüfe die Internetverbindung, die Adresse und die Zugangsdaten.¦请检查网络连接、地址和登录信息。
Erneut versuchen¦重试
Im Trend¦热门
Top 10¦前十名
Top bewertet¦高分
Diese Playlist enthält keine Filme oder Serien. Live TV findest du links.¦此播放列表没有电影或剧集，直播电视在左侧。
Filme¦电影
Serien¦剧集
Alle {#0}¦全部 {#0}
Alle {0}¦全部 {0}
Keine {0} in dieser Playlist¦此播放列表没有{0}
Weitere Kategorien findest du unter „Kategorien“ oben.¦更多分类在上方的「分类」里。
Filme, Serien oder Sender suchen…¦搜索电影、剧集或频道…
Letzte Suchanfragen¦最近搜索
Verlauf löschen¦清除历史
Keine Treffer für „{0}“¦没有「{0}」的结果
Suche „{0}“¦搜索「{0}」
Live TV¦直播电视
Weiterschauen¦继续观看
Mehr Infos¦更多信息
Favoriten¦收藏
Alle Sender¦全部频道
Noch keine Favoriten. Sender mit OK (gedrückt halten) oder der Menü-Taste als Favorit markieren.¦暂无收藏。长按确认键或按菜单键可收藏频道。
Keine Sender¦无频道
Violett & Türkis¦紫与青
Ozean¦海洋
Smaragd¦翡翠
Sonnenuntergang¦日落
Rosé¦玫瑰
Gold¦金色
Orange¦橙色
Eisblau¦冰蓝
Limette¦青柠
Purpur¦紫色
Schiefer¦石板灰
Mitternacht¦午夜
OLED-Schwarz¦OLED 黑
Graphit¦石墨
Tiefblau¦深蓝
Aus¦关
An¦开
Dezent¦柔和
Normal¦正常
Endet um {0}¦{0} 结束
Klein¦小
Mittel¦中
Groß¦大
Kompakt¦紧凑
Jetzt¦现在
Jetzt {0}¦现在 {0}
TV-Guide – Ansicht¦节目单 – 视图
Zoom¦缩放
Zeilenhöhe¦行高
Vergangene Sendungen¦已播节目
Zurück = schließen¦返回 = 关闭
Menü-Taste im TV-Guide = Zoom¦节目单中按菜单键 = 缩放
Zeitzone {0} (änderbar unter Einstellungen > Sprache & Region)¦时区 {0}（在「设置 > 语言与地区」中修改）
Sendungen¦节目
Sender¦频道
Menü¦菜单
Zurück¦返回
in {#0} Min¦{#0} 分钟后
noch {#0} Min¦剩余 {#0} 分钟
Keine Beschreibung vorhanden.¦暂无简介。
TV-Guide¦节目单
Wähle eine Sendung mit den Pfeiltasten, OK schaltet auf den Sender.¦用方向键选择节目，按确认键切换到该频道。
Zurück bringt dich zur Senderspalte, nochmal Zurück schließt.¦按返回键回到频道栏，再按一次返回键关闭。
lädt …¦加载中 …
Kein Programm verfügbar¦无节目单数据
Kein Programm für diesen Sender¦此频道无节目单数据
Danach {0}¦接下来：{0}
OK halten = Kategorie als Favorit¦长按确认键 = 收藏分类
OK = ansehen¦确认键 = 观看
halten = Favorit¦长按 = 收藏
Mono¦单声道
Stereo¦立体声
{#0} Kanäle¦{#0} 个频道
Spur {#0}¦音轨 {#0}
1× (Normal)¦1×（正常）
Zugang abgelehnt ({#0}). Benutzername, Passwort oder Abo beim Anbieter prüfen.¦访问被拒（{#0}）。请向服务商核对用户名、密码或订阅状态。
Dieser Sender ist beim Anbieter nicht mehr vorhanden.¦服务商已下架此频道。
Dieser Titel ist beim Anbieter nicht mehr vorhanden.¦服务商已下架此内容。
Maximale Verbindungen des Anbieters erreicht. Anderes Gerät oder Profil beenden und erneut versuchen.¦已达服务商最大连接数。请关闭其他设备或退出其他用户后重试。
Zu viele gleichzeitige Verbindungen. Anderes Gerät beenden und erneut versuchen.¦同时连接数过多。请停止其他设备后重试。
Der Anbieter hat gerade ein Problem ({#0}). Später erneut versuchen.¦服务商当前出现故障（{#0}），请稍后重试。
Der Anbieter lehnt die Wiedergabe ab ({#0}).¦服务商拒绝播放（{#0}）。
Keine Verbindung zum Anbieter. Internet und Netzwerk prüfen.¦无法连接服务商，请检查网络。
Dieses Videoformat kann das Gerät nicht abspielen.¦此设备无法播放该视频格式。
Der Stream ist fehlerhaft oder wird nicht unterstützt.¦视频流异常或不受支持。
Wiedergabe nicht möglich.¦无法播放。
Audio & Untertitel¦音频与字幕
Links schließt das Menü¦按左键关闭菜单
Rechts wechselt zu den Untertiteln¦按右键切换到字幕
Audio¦音频
Keine Auswahl verfügbar¦无可选项
Untertitel¦字幕
Dieser Stream enthält keine Untertitel.¦此视频流没有字幕。
Aussehen der Untertitel¦字幕外观
Folgen¦分集
Folge {#0}¦第 {#0} 集
Einpassen¦适应
Zoom (Rand abschneiden)¦缩放（裁切边缘）
Strecken¦拉伸
Verbindung unterbrochen – neuer Versuch ({#0}/3)¦连接中断 —— 正在重试（{#0}/3）
Intro¦片头
So sehen deine Untertitel aus¦这是字幕的显示效果
Senderliste¦频道列表
Aus Favoriten entfernen¦取消收藏
Zu Favoriten hinzufügen¦加入收藏
Bildformat¦画面比例
Extern öffnen¦用外部播放器打开
Hoch und Runter wechseln den Sender¦上/下方向键切换频道
Zeitleiste¦进度条
= {#0} s springen¦= 快进 {#0} 秒
Halten = weiter springen¦长按 = 持续快进
Vorherige Folge: Staffel {#0}, Folge {#1}¦上一集：第 {#0} 季第 {#1} 集
Vorherige Folge¦上一集
Nächste Folge: Staffel {#0}, Folge {#1}¦下一集：第 {#0} 季第 {#1} 集
Nächste Folge¦下一集
{#0} Sekunden zurück¦后退 {#0} 秒
Pause¦暂停
{#0} Sekunden vor¦前进 {#0} 秒
Einstellungen¦设置
Links = zurück¦左键 = 返回
Größe¦大小
Farbe¦颜色
Hintergrund¦背景
Rand¦描边
OK = nächster Wert. Die Vorschau zeigt das Ergebnis.¦确认键切换下一个值，预览区显示效果。
Videoqualität¦视频画质
Keine Auswahl verfügbar (feste Qualität).¦无可选项（画质固定）。
Sprungweite¦快进步长
Gilt für Links/Rechts, Halten und die Zeitleiste.¦适用于左/右方向键、长按和进度条。
Nächste Folge automatisch¦自动播放下一集
Endzeit anzeigen¦显示结束时间
Startet in {0}¦{0} 开始
Server antwortet mit Fehler {#0}¦服务器返回错误 {#0}
Der Server hat keine Daten geliefert¦服务器未返回数据
Keine Sender in der Playlist gefunden¦播放列表中未找到频道
Unbekannter Fehler¦未知错误
Allgemein¦常规
Start, Beenden, Autostart¦启动、退出、开机自启
Sprache & Region¦语言与地区
Sprache, Uhrzeit, Zeitzone¦语言、时间、时区
Kartengröße, Raster¦卡片大小、网格
Design¦外观
Farben, Schrift, Animationen¦颜色、字体、动画
Reihenfolge und Einträge¦顺序与条目
Player¦播放器
Spulen, Intro, Bildformat¦快进、片头、画面比例
Ton & Untertitel¦音频与字幕
Sprachen, Untertitel-Stil¦语言、字幕样式
Genres & Kategorien¦类型与分类
Automatisch oder vom Anbieter¦自动或来自服务商
Live-TV & Guide¦直播与节目单
TV-Programm, Favoriten¦节目表、收藏
Playlists¦播放列表
Verwalten, Aktualisieren¦管理、更新
Jugendschutz¦家长控制
PIN, Inhalte ab 18¦PIN 码、成人内容
Speicher¦存储
Cache, Verlauf, Fortsetzen¦缓存、历史、续播
Diagnose¦诊断
Status und Fehlersuche¦状态与故障排查
Über IPVIO Stream¦关于 IPVIO Stream
Version, Quellen, Zurücksetzen¦版本、来源、重置
Wirklich? OK zum Bestätigen¦确定吗？按确认键确认
‹ Zurück¦‹ 返回
Automatisch (Gerät)¦自动（跟随设备）
Sprache der App¦应用语言
Alle Menüs und Hinweise erscheinen in dieser Sprache. Beschreibungen, Besetzung und Titel-Infos kommen von TMDB und werden in derselben Sprache geladen, sobald du einen Titel öffnest. Namen von Sendern, Filmen und Kategorien stammen von deinem Anbieter und bleiben unverändert.¦所有菜单和提示都会使用此语言。简介、演员和影片信息来自 TMDB，打开条目时按同一语言加载。频道、影片和分类名称来自你的服务商，保持不变。
Uhrzeit¦时间
Zeitformat¦时间格式
24 Stunden (14:30)¦24 小时制（14:30）
12 Stunden (2:30 PM)¦12 小时制（2:30 PM）
Zeitzone¦时区
Systemzeit (Gerät: {0})¦系统时间（设备：{0}）
Uhrzeit jetzt¦当前时间
Gilt für alle Uhrzeiten, zum Beispiel im TV-Guide und in der Live-TV-Infoleiste. „Systemzeit“ nutzt die Einstellung deines Geräts. Zeigt die App Uhrzeiten falsch an, steht dein Fernseher vermutlich auf UTC; wähle dann hier deine Zeitzone.¦适用于所有时间显示，例如节目单和直播信息栏。「系统时间」使用设备设置；如果应用显示的时间不对，多半是电视被设成了 UTC，请在此选择时区。
PIN eingeben¦输入 PIN 码
Die Jugendschutz-Einstellungen sind mit einer PIN geschützt.¦家长控制设置受 PIN 码保护。
Falsche PIN¦PIN 码错误
Neue PIN festlegen¦设置新 PIN 码
Wähle eine 4-stellige PIN.¦请设置 4 位数字 PIN 码。
PIN bestätigen¦确认 PIN 码
Gib die PIN noch einmal ein.¦请再次输入 PIN 码。
Die PINs stimmen nicht überein¦两次输入的 PIN 码不一致
Inhalte für Erwachsene ausblenden¦隐藏成人内容
Blendet Kategorien und Titel mit Begriffen wie XXX, Adult oder 18+ in der ganzen App aus: Startseite, Suche, Listen und Live-TV.¦在整个应用（首页、搜索、列表、直播）中隐藏包含 XXX、Adult、18+ 等字样的分类和节目。
Der Schutz ist aktiv.¦保护已生效。
{#0} Einträge in deiner Playlist würden ausgeblendet.¦播放列表中有 {#0} 个条目会被隐藏。
Kein Erwachsenen-Inhalt erkannt.¦未检测到成人内容。
PIN¦PIN 码
PIN ist gesetzt¦已设置 PIN 码
Keine PIN gesetzt¦未设置 PIN 码
PIN ändern¦修改 PIN 码
PIN festlegen¦设置 PIN 码
PIN entfernen¦移除 PIN 码
Mit einer PIN lässt sich der Schutz nur nach Eingabe der PIN ändern. Die Erkennung arbeitet mit Stichwörtern in Kategorie- und Titelnamen und ist kein vollständiger Filter. Beim Zurücksetzen aller Einstellungen bleibt die PIN erhalten.¦设置 PIN 码后，需先输入才能更改保护设置。检测基于分类和名称中的关键词，并非完整过滤。重置所有设置时 PIN 码会保留。
Start¦首页
Beim Einschalten des Geräts öffnen¦设备启动时打开
IPVIO Stream startet automatisch, sobald der Fernseher oder die Box hochgefahren ist.¦电视或盒子开机完成后自动启动 IPVIO Stream。
Berechtigung erteilt¦已授予权限
Berechtigung „Über anderen Apps einblenden“ fehlt¦缺少「显示在其他应用上层」权限
Einstellung öffnen¦打开设置
Prüfen¦检查
Ab Android 10 erlaubt das System den Autostart nur mit dieser Berechtigung. Manche Geräte (z. B. einzelne Fire-TV-Modelle) blockieren den Autostart trotzdem; das kann die App nicht ändern.¦从 Android 10 开始，系统要求具备此权限才允许自启动。部分设备（如某些 Fire TV 型号）仍会阻止自启动，应用无法更改。
Suche¦搜索
Startseite¦首页
Diese Seite öffnet sich beim Start der App. Versteckte Menüpunkte sind nicht wählbar.¦应用启动时打开此页面。隐藏的菜单项无法被选中。
Letzten Sender beim Start abspielen¦启动时播放上次频道
Wenn du zuletzt Live TV gesehen hast, läuft dieser Sender direkt nach dem Start.¦如果上次看的是直播，启动后会直接播放该频道。
Bedienung¦控制
Beenden bestätigen¦退出确认
Auf der Startseite muss „Zurück“ zweimal gedrückt werden, damit die App nicht aus Versehen schließt.¦在首页需连按两次「返回」才退出，避免误操作。
Dieses Gerät bietet die Einstellung nicht an. Autostart funktioniert evtl. trotzdem.¦此设备未提供该设置，自启动仍可能生效。
Zwischenspeicher¦缓存
Leeren¦清除
Gesamt¦总计
Geleerte Daten lädt die App bei Bedarf neu. Titel-Infos und Bilder erscheinen danach kurz später. Der Playlist-Zwischenspeicher macht den Start schnell; ohne ihn wartet die App auf den Download.¦应用会在需要时重新加载被清除的数据，影片信息和图片会稍晚出现。播放列表缓存可加快启动；没有它，启动时需要等待下载。
Verlauf¦历史
Suchverlauf löschen¦清除搜索历史
Zuletzt geschaut löschen¦清除最近观看
Weiterschauen löschen¦清除继续观看
Meine Liste und Favoriten bleiben immer erhalten.¦我的列表和收藏始终保留。
Filme, Serien und Sender¦电影、剧集和频道
Raster zeigt viele Titel auf einmal. Kino zeigt oben die Details zum markierten Titel und unten eine Reihe zum Durchblättern.¦网格可一次显示大量节目；影院模式在上方显示选中节目的详情，下方一行用于浏览。
Spalten (Raster)¦列数（网格）
Zeilen (Raster)¦行数（网格）
Banner wechselt¦横幅切换
nie¦从不
alle {#0} s¦每 {#0} 秒
Kartengröße (Reihen)¦卡片大小（行数）
Kategorien-Ansicht¦分类视图
Liste mit Vorschau¦带预览的列表
Das Raster kannst du zusätzlich direkt in der Rasteransicht mit dem Raster-Knopf ändern.¦也可以在网格视图中直接用网格按钮调整。
Verschiebe Einträge mit den Pfeil-Knöpfen oder blende sie aus. Start und Einstellungen bleiben immer sichtbar.¦用方向按钮移动条目或将其隐藏。首页和设置始终可见。
versteckt¦已隐藏
sichtbar¦可见
Standard-Reihenfolge wiederherstellen¦恢复默认顺序
Sprung links/rechts¦跳过左/右
Standard-Bildformat¦默认画面比例
Intro überspringen anbieten¦提示跳过片头
Zeigt bei Serien einen Knopf „Intro überspringen“, wenn Intro-Zeiten bekannt sind.¦已知片头时间时，剧集播放时显示「跳过片头」按钮。
Stream-Info am Start¦播放开始时显示流信息
Zeigt kurz Auflösung, Ton und Format beim Abspielen von Filmen und Serien.¦播放电影和剧集时短暂显示分辨率、音频和格式。
Steuerleiste ausblenden nach¦控制栏在以下时间后隐藏
Startet die nächste Folge nach dem Abspann von selbst.¦片尾结束后自动开始下一集。
Zeigt, um wie viel Uhr der Film voraussichtlich endet.¦显示电影预计结束时间。
Intro automatisch überspringen¦自动跳过片头
Überspringt bekannte Intros ohne Rückfrage.¦已知片头时直接跳过，不再询问。
Bedienung: OK = Pause, Links/Rechts = Sprung (Halten = weiter springen), mehrere Sprünge werden gesammelt, Zurück beim Spulen = abbrechen, Hoch = Zeitleiste, Runter = Steuerleiste. Intro-Zeiten stammen aus der Community-Datenbank TheIntroDB und gibt es nicht für jede Serie. Die Stream-Info erscheint nur bei Filmen und Serien.¦操作：确认键 = 暂停，左/右 = 快进（长按持续快进），多次快进会合并，快进中按返回 = 取消，上 = 进度条，下 = 控制栏。片头时间来自社区数据库 TheIntroDB，并非所有剧集都有。流信息仅对电影和剧集显示。
Farbthema¦配色主题
Eigene Farbe¦自定义颜色
Übernehmen¦应用
Ungültig¦无效
Wähle ein Farbfeld oder gib einen Hex-Code ein, z. B. #FF6A00. Die App passt die Farbe automatisch so an, dass Schrift immer gut lesbar bleibt.¦选择色块或输入十六进制色值，例如 #FF6A00。应用会自动调整颜色，保证文字始终清晰可读。
Darstellung¦外观
Animationen¦动画
Anzeigegröße¦显示尺寸
Oberfläche aktuell ×¦当前界面 ×
(Bildschirm¦（屏幕
dp breit). Standard ist 85 %: mehr Inhalt, Elemente bleiben gut lesbar. „Automatisch“ vergrößert die App auf sehr breiten Bildschirmen.¦dp 宽）。默认 85%：内容更多，元素仍清晰易读。「自动」会在超宽屏上放大应用。
Schrift¦字体
Schriftart¦字体系列
Schriftgröße¦字号
Aa – So sieht deine Schrift aus¦Aa —— 这是字体的显示效果
Fünf klare Systemschriften, die auf jedem Gerät vorhanden sind. Die Schriftgröße wirkt zusätzlich zur Anzeigegröße.¦提供五种各设备都有的简洁系统字体。字号在显示尺寸之上叠加生效。
Vorschau¦预览
Aktiv¦启用
Hervorhebung¦高亮
Animationen „Aus“ schaltet die Vergrößerung beim Fokus ab, „Dezent“ macht sie schwächer. Das schont schwache Geräte.¦动画设为「关」会禁用焦点放大，「柔和」则减弱效果，可减轻性能较弱设备的负担。
Design zurücksetzen¦重置外观
keiner seit dem Start¦启动以来无
Version¦版本
Bildschirm¦屏幕
{0} × {1} dp · Dichte¦{0} × {1} dp · 密度
Inhalte¦内容
{0} Sender · {1} Filme · {2} Serien¦{0} 个频道 · {1} 部电影 · {2} 部剧集
Ladezustand¦加载状态
Programm (EPG)¦节目表（EPG）
{0} · {1} Sender¦{0} · {1} 个频道
lädt {0}/{1}¦正在加载 {0}/{1}
kein TMDB-Schlüssel¦无 TMDB 密钥
Letzter Fehler¦最近错误
Fehler seit Start¦启动以来错误数
Playlist neu laden¦重新加载播放列表
Bericht kopieren¦复制报告
IPVIO Diagnose¦IPVIO 诊断
Der Bericht enthält keine Adressen, Benutzernamen oder Passwörter. Er wird nur in die Zwischenablage kopiert und nirgendwohin gesendet.¦报告不含地址、用户名或密码，仅复制到剪贴板，不会发送到任何地方。
Tonspur¦音轨
Automatische Genres¦自动类型识别
Aus = Kategorien deines Anbieters¦关 = 使用服务商分类
Automatische Genres sortieren Titel nach Genre (Anime = Animation aus Japan). Beim ersten Mal dauert das einige Minuten, danach ist alles gespeichert. Aus = die Kategorien deines Anbieters in der Reihenfolge deines Anbieters. In der Ansicht „Kategorien“ kannst du jederzeit zwischen beiden wechseln.¦自动类型识别按类型归类节目（动漫 = 日本动画）。首次运行需几分钟，之后结果会保存。关 = 按服务商的分类和顺序显示。在「分类」视图中可随时切换两者。
Automatische Genres brauchen den TMDB-Schlüssel in Config.kt.¦自动类型识别需要在 Config.kt 中配置 TMDB 密钥。
TV-Programm (EPG)¦节目表（EPG）
Programm bei {0} von {1} Sendern möglich¦{1} 个频道中有 {0} 个提供节目表
XMLTV: {0}¦XMLTV：{0}
Die App sucht die Programmdaten automatisch (Xtream-Schnittstelle oder XMLTV-Adresse aus der Playlist). Wenn dein Anbieter eine eigene XMLTV-Adresse nennt, trage sie hier ein.¦应用会自动查找节目表数据（Xtream 接口或播放列表中的 XMLTV 地址）。如果服务商提供了单独的 XMLTV 地址，请填在这里。
Speichern & laden¦保存并加载
Neu laden¦重新加载
Sender oder Kategorien als Favorit markieren: OK gedrückt halten oder Menü-Taste.¦收藏频道或分类：长按确认键或按菜单键。
Meine Playlists ({0})¦我的播放列表（{0}）
Aktivieren¦启用
Umbenennen¦重命名
Löschen¦删除
Name der Playlist¦播放列表名称
Name speichern¦保存名称
+ Weitere Playlist hinzufügen¦+ 添加另一个播放列表
Aktive Playlist neu laden¦重新加载当前播放列表
Automatisch aktualisieren¦自动更新
Bei jedem Start¦每次启动时
Täglich¦每天
Wöchentlich¦每周
Nur von Hand¦仅手动
Playlist aktualisieren¦更新播放列表
Abstand¦间隔
alle {0} Stunden¦每 {0} 小时
Wochentag¦星期
Stunde¦小时
Minute¦分钟
Zuletzt aktualisiert:¦上次更新：
noch nie¦从未
Nächste Aktualisierung:¦下次更新：
„Bei jedem Start“ lädt die Playlist bei jedem Öffnen der App. Die geplanten Varianten laufen auch im Hintergrund, solange das Gerät eingeschaltet und online ist, und holen verpasste Termine beim nächsten Start nach. „Nur von Hand“ nutzt immer den gespeicherten Stand; Neu laden geht jederzeit mit dem Knopf oben. Die Uhrzeit richtet sich nach deiner Zeitzone unter „Sprache & Region“.¦「每次启动时」会在每次打开应用时加载播放列表。定时选项还会在设备开机联网时于后台运行，并在下次启动时补上错过的时间。「仅手动」始终使用已保存的副本，可随时用上方按钮重新加载。时间按「语言与地区」中的时区计算。
Zugangsdaten werden nur auf diesem Gerät gespeichert und hier nicht angezeigt. Es ist immer eine Playlist aktiv. Mit „Aktivieren“ wechselst du die Liste.¦登录信息仅保存在本设备，不在此显示。始终只有一个播放列表处于启用状态，用「启用」切换。
IPVIO Stream · Version¦IPVIO Stream · 版本
IPVIO Stream ist ein Player und liefert selbst keine Inhalte. Nutze nur Quellen, für die du eine Berechtigung hast.¦IPVIO Stream 只是播放器，本身不提供任何内容。请仅使用你有权使用的来源。
Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert. Intro-Zeiten: TheIntroDB.¦本产品使用 TMDB API，但未获得 TMDB 认可或认证。片头时间来自 TheIntroDB。
Systemeinstellungen der App¦应用系统设置
Setzt Ansicht, Design, Menü, Player, Ton und Start auf die Standardwerte. Playlists, Favoriten, Verlauf und Fortsetzen-Positionen bleiben erhalten. Zwischenspeicher leerst du unter „Speicher“.¦将视图、外观、菜单、播放器、音频和启动项恢复默认。播放列表、收藏、历史和续播位置会保留。缓存请在「存储」中清除。
Directing¦导演
Acting¦表演
Writing¦编剧
Director¦导演
anzeigen ›¦显示 ›
Download fehlgeschlagen (HTTP {0})¦下载失败（HTTP {0}）
Datei unplausibel¦文件内容异常
Offline oder nicht erreichbar¦离线或无法访问
{0} Trailer¦{0} 个预告片
Ratings¦评分
Source¦来源
Value¦值
Episodes¦分集
Episode¦单集
Serif¦衬线体
Leicht¦细体
Monospace¦等宽
Sehr groß¦超大
Riesig¦巨大
Wiedergabe:¦播放：
Keine Programmdaten erhalten¦未收到节目表数据
Zurück mit der Zurück-Taste.¦按返回键返回。
Deutsch¦德语
Englisch¦英语
Japanisch¦日语
Koreanisch¦韩语
Französisch¦法语
Spanisch¦西班牙语
Türkisch¦土耳其语
Kroatisch¦克罗地亚语
Serbisch¦塞尔维亚语
Bosnisch¦波斯尼亚语
Weiß¦白色
Gelb¦黄色
Grün¦绿色
Cyan¦青色
Pink¦粉色
Schwarz¦黑色
Keiner¦无
Kein¦无
Halbtransparent¦半透明
Schatten¦阴影
Kontur¦描边
Action¦动作
Abenteuer¦冒险
Komödie¦喜剧
Krimi¦犯罪
Dokumentation¦纪录片
Drama¦剧情
Familie¦家庭
Fantasy¦奇幻
Historie¦历史
Horror¦恐怖
Musik¦音乐
Mystery¦悬疑
Romantik¦爱情
Thriller¦惊悚
Krieg¦战争
Western¦西部
Kinder¦儿童
Nachrichten¦新闻
Reality¦真人秀
Soap¦肥皂剧
Untertitel-Stil¦字幕样式
Wiedergabe-Tempo¦播放速度
ansehen¦观看
schließen¦关闭
JETZT¦正在播出
BEENDET¦已结束
Letzter Absturz¦上次崩溃
wenig¦低
mittel¦中
viel¦高
keiner¦无
Sleep-Timer¦睡眠定时
Letzter Kanal¦上次频道
Kein Kanal mit dieser Nummer¦没有此编号的频道
Zurück zu Live¦返回直播
LIVE¦直播
Profile¦用户
Verlauf, Favoriten, Jugendschutz je Person¦每人独立的历史、收藏、家长控制
Wer schaut?¦正在观看的是谁？
Hauptprofil¦主用户
Wechseln¦切换
Profil hinzufügen¦添加用户
Name des Profils¦用户名称
Beim Start fragen: Wer schaut?¦启动时询问「正在观看的是谁？」
Zeigt beim Öffnen der App die Profilauswahl, sobald es mehr als ein Profil gibt.¦当存在多个用户时，打开应用即显示用户选择。
Beim Wechseln startet die App kurz neu.¦切换时应用会短暂重启。
Jedes Profil hat eigenen Verlauf, eigene Favoriten, Ansicht, Player-Einstellungen und eigenen Jugendschutz. Playlist, Sprache und Aktualisierung sind für alle gleich.¦每个用户都有独立的历史、收藏、视图、播放器设置和家长控制。播放列表、语言和更新设置对所有人相同。
Sicherung¦备份
Sicherung erstellen¦创建备份
Sicherung wiederherstellen¦恢复备份
Sicherung fehlgeschlagen.¦备份失败。
Keine gültige Sicherung gefunden.¦未找到有效的备份。
Gesichert werden Einstellungen, Favoriten, Verlauf, Weiterschauen und Profile. Playlists und Zugangsdaten sind nicht enthalten. Beim Wiederherstellen startet die App neu.¦会备份设置、收藏、历史、继续观看和用户，不包含播放列表和登录凭据。恢复时应用会重启。
Standard-Ansicht¦默认视图
Oberfläche aktuell ×{0} (Bildschirm {#1} dp breit).¦当前界面 ×{0}（屏幕宽 {#1} dp）。
Standard ist 85 %: mehr Inhalt, Elemente bleiben gut lesbar. „Automatisch“ vergrößert die App auf sehr breiten Bildschirmen.¦默认 85%：内容更多，元素仍清晰易读。「自动」会在超宽屏上放大应用。
Zuletzt aktualisiert¦上次更新
Nächste Aktualisierung¦下次更新
Bereit¦就绪
Lade…¦加载中…
läuft¦播放中
Startet in {#0} s¦{#0} 秒后开始
Gespeichert¦已保存
Anbieter-Kategorien¦服务商分类
Auto-Genres¦自动类型
bereit¦就绪
aktiv¦已启用
keine¦无
Installiert¦已安装
Oder vom Handy senden (gleiches WLAN):¦或用手机发送（连接同一 Wi-Fi）：
Bitte mit dem Internet verbinden¦请连接网络
So schaltest du IPVIO Stream frei – ohne etwas am Fernseher zu tippen:¦解锁 IPVIO Stream —— 无需在电视上输入任何内容：
1.  Öffne auf deinem Handy diese Adresse:¦1. 在手机上打开这个地址：
3.  Dieser Bildschirm schaltet sich danach automatisch frei.¦3. 之后此页面会自动解锁。
Prüfe …¦正在检查 …
Erneut prüfen¦重新检查
Neuen Code anfordern¦获取新验证码
Testphase: noch {#0} Tage¦试用：剩余 {#0} 天
Testphase: noch {#0} Tag¦试用：剩余 {#0} 天
Entwicklung (ohne Prüfung)¦开发版（不校验）
Gültig¦有效
dauerhaft¦永久
Gültig bis¦有效期至
noch {#0} Tage¦剩余 {#0} 天
Öffne auf dem Handy:¦在手机上打开：
Der Link gilt 30 Minuten. Nach der Zahlung schaltet sich dieses Gerät automatisch frei.¦链接 30 分钟内有效。付款后此设备会自动解锁。
Geräte ({#0} von {#1})¦设备（{#0} / {#1}）
dieses Gerät¦本设备
Entfernen¦移除
Mit „Entfernen“ wird ein Platz frei, zum Beispiel für einen neuen Fernseher.¦「移除」可释放一个名额，例如给新电视使用。
Talk¦脱口秀
Wien¦维也纳
Zürich¦苏黎世
Belgrad¦贝尔格莱德
Athen¦雅典
Bukarest¦布加勒斯特
Lissabon¦里斯本
Moskau¦莫斯科
Tokio¦东京
Seoul¦首尔
Mexiko-Stadt¦墨西哥城
Indien¦印度
Schnell {0}¦快进 {0}
Vancouver¦温哥华
Private Version¦私人版本
Zu viele Fehlversuche. Bitte in {#0} s erneut versuchen.¦错误次数过多，请在 {#0} 秒后重试。
Bitte eine gültige Adresse eingeben (beginnt mit http:// oder https://).¦请输入有效地址（以 http:// 或 https:// 开头）。
Angaben unvollständig: Server, Benutzername und Passwort nötig.¦信息不完整：服务器、用户名和密码均为必填。
Der Server hat keine Daten geliefert.¦服务器未返回数据。
Keine Sender in der Playlist gefunden.¦播放列表中未找到频道。
Zugang abgelehnt ({#0}). Benutzername, Passwort oder Abo prüfen.¦访问被拒（{#0}）。请核对用户名、密码或订阅状态。
Playlist nicht gefunden (404). Adresse prüfen.¦未找到播放列表（404），请检查地址。
Der Anbieter begrenzt gerade die Verbindungen ({#0}). Später erneut versuchen.¦服务商当前限制连接数（{#0}），请稍后重试。
Laden fehlgeschlagen.¦加载失败。
Laden fehlgeschlagen ({0}).¦加载失败（{0}）。
Server nicht gefunden. Adresse und Internetverbindung prüfen.¦找不到服务器，请检查地址和网络连接。
Zeitüberschreitung: Der Server antwortet nicht.¦超时：服务器无响应。
Keine Verbindung zum Server.¦无法连接服务器。
Sichere Verbindung fehlgeschlagen (Zertifikat).¦安全连接失败（证书问题）。
Ungültige Adresse.¦地址无效。
Die Playlist ist zu groß für den Speicher dieses Geräts.¦播放列表过大，超出本设备内存。
Verbindung unterbrochen. Bitte erneut versuchen.¦连接中断，请重试。
Verbindung wieder da – lade neu …¦连接已恢复 —— 正在重新加载 …
Bildschirm kann nicht wach gehalten werden – der Bildschirmschoner kann starten.¦无法保持屏幕常亮 —— 屏保可能会启动。
Automatische Genres brauchen den TMDB-Schlüssel (Datei secrets.properties im Projektordner).¦自动类型识别需要 TMDB 密钥（项目目录中的 secrets.properties 文件）。
Bildschirm wach¦屏幕常亮
an¦开
aus¦关
Dieses Profil ist durch den Jugendschutz gesperrt. Profile wechseln und bearbeiten geht nur mit der PIN.¦此用户已被家长控制锁定，切换和编辑用户需要 PIN 码。
Das zuletzt genutzte Profil ist durch den Jugendschutz gesperrt. Zum Wechseln bitte dessen PIN eingeben.¦上次使用的用户已被家长控制锁定，请输入其 PIN 码后切换。
Eigene Farben¦自定义配色
Farbe 1 · Flächen¦颜色 1 · 表面
Farbe 2 · Hervorhebung¦颜色 2 · 高亮
Farbe 2 · automatisch¦颜色 2 · 自动
Farbe 2 ist auf diesem Hintergrund schwer lesbar.¦颜色 2 在此背景上不易阅读。
Farbe 1 färbt Knöpfe, Balken und markierte Flächen – die Schrift darauf wird automatisch hell oder dunkel, damit sie lesbar bleibt. Farbe 2 färbt Hervorhebungen, Rahmen und farbige Schrift. „Automatisch“ berechnet Farbe 2 passend zu Farbe 1.¦颜色 1 用于按钮、栏和选中表面 —— 其上的文字会自动变亮或变暗以保证可读。颜色 2 用于高亮、边框和彩色文字。「自动」会根据颜色 1 计算出匹配的颜色 2。
IPVIO Navy¦IPVIO 海军蓝
Einfacher mit dem Handy¦用手机更轻松
Mit der Fernbedienung zu tippen ist mühsam. Schick die Zugangsdaten lieber vom Handy:¦用遥控器输入很麻烦，改成从手机发送信息：
Handy mit demselben WLAN verbinden wie dieses Gerät.¦让手机连接与此设备相同的 Wi-Fi。
Im Browser des Handys diese Adresse öffnen:¦在手机浏览器打开这个地址：
Den Code von hier eingeben:¦输入此处显示的验证码：
Zugangsdaten eintragen und senden. Die Playlist wird danach automatisch geladen.¦填写信息并发送，播放列表随后自动加载。
Die Übertragung vom Handy ist gerade nicht verfügbar. Das Gerät muss mit dem WLAN verbunden sein.¦手机传输当前不可用，此设备必须连接 Wi-Fi。
Code¦验证码
Playlist wird geladen¦正在加载播放列表
Verbindung zum Server wird aufgebaut …¦正在连接服务器 …
Sender werden heruntergeladen …¦正在下载频道 …
Kategorien werden sortiert …¦正在整理分类 …
Playlist wird gespeichert …¦正在保存播放列表 …
heruntergeladen¦已下载
Große Playlists können bis zu einer Minute dauern.¦大型播放列表可能需要一分钟。
Deine Bibliothek ist für automatische Genres zu groß (über 60.000 Filme und Serien). Es werden die Kategorien deines Anbieters verwendet. Blende unten Sprachen aus, die du nicht brauchst – dann passt es wieder.¦你的媒体库过大，无法自动识别类型（超过 60,000 部电影和剧集），将改用服务商的分类。请在下方隐藏不需要的语言，之后即可重新启用。
Sprachen der Inhalte¦内容语言
Blende Sprachen aus, die du nicht brauchst – sie verschwinden dann überall: Startseite, Filme, Serien, Live-TV und Suche. Erkannt wird die Kennzeichnung deines Anbieters (z. B. „DE |“, „[TR]“, „GERMAN“). Das ist bei IPTV-Anbietern die Sprache der Tonspur, nicht das Herkunftsland des Films.¦隐藏不需要的语言 —— 它们会在首页、电影、剧集、直播和搜索中全部消失。识别依据是你的服务商标签（例如「DE |」、「[TR]」、「GERMAN」）。对 IPTV 服务商而言，这指的是音轨语言，而不是影片的制片国家。
Sprachen werden gezählt …¦正在统计语言 …
Noch keine Playlist geladen.¦尚未加载播放列表。
Nur meine Sprache¦仅我的语言
Alle zeigen¦显示全部
Ohne Kennung¦无标签
Genres, Sprachen der Inhalte¦类型、内容语言
Genres werden erkannt¦正在识别类型
Länder werden ergänzt¦正在添加国家/地区
Automatische Genres sind für diese Auswahl ausgeschaltet¦当前选择下自动类型识别已关闭
Deine Bibliothek hat {#0} Filme und Serien – die Grenze für automatische Genres liegt bei {#1}. Blende in Einstellungen > Playlists > „Sprachen der Inhalte“ die Sprachen aus, die du nicht brauchst. Danach werden die Genres im Hintergrund erkannt.¦你的媒体库有 {#0} 部电影和剧集 —— 自动类型识别上限为 {#1}。请在「设置 > 播放列表 > 内容语言」中隐藏不需要的语言，类型随后会在后台识别。
Genres werden gerade im Hintergrund erkannt. Die Kategorien erscheinen nach und nach.¦类型正在后台识别，分类会逐步出现。
Sprachauswahl wird angewendet …¦正在应用语言选择 …
Ausgeblendete Sprachen werden gar nicht erst geladen. Das spart viel Arbeitsspeicher und macht große Playlists schneller.¦被隐藏的语言完全不会加载，可大幅节省内存并加快大型播放列表的加载。
Mindestens eine Sprache muss sichtbar bleiben.¦至少保留一种语言可见。
Geladen: {#0} Sender mit Programm (gekürzt, Speicher knapp)¦已加载：{#0} 个带节目的频道（精简模式，低内存）
Alle Sprachen dieser Playlist sind ausgeblendet¦此播放列表的所有语言都被隐藏
Alle Sprachen zeigen¦显示全部语言
Neue Infos · aktualisieren¦新信息 · 刷新
Suche …¦搜索中 …
Profil {#0}¦用户 {#0}
Sende deine Playlist an den Fernseher. Den Code siehst du auf dem Bildschirm.¦把播放列表发送到电视，验证码显示在屏幕上。
M3U-Link¦M3U 链接
Code vom Fernseher¦电视上的验证码
An den Fernseher senden¦发送到电视
oder Xtream-Login¦或 Xtream 登录
Gesendet. Schau jetzt auf den Fernseher.¦已发送，请查看电视。
Falscher Code. Bitte zurück und erneut versuchen.¦验证码错误，请返回重试。
Eingabe unvollständig oder ungültig.¦输入不完整或无效。
Zu viele Fehlversuche. Auf dem Fernseher neu starten.¦尝试次数过多，请在电视上重新开始。
Laden¦加载
Playlist wird heruntergeladen …¦正在下载播放列表 …
Download wird fortgesetzt …¦正在续传下载 …
Verbindung unterbrochen – neuer Versuch …¦连接中断 —— 正在重试 …
Download konnte nicht fortgesetzt werden. Bitte erneut versuchen.¦无法续传下载，请重试。
Playlist wird geprüft …¦正在检查播放列表 …
Titel werden eingelesen …¦正在读取节目 …
Gespeicherte Playlist wird gelesen …¦正在读取已保存的播放列表 …
Sprachen auswählen¦选择语言
Laden abgebrochen. Deine Angaben bleiben gespeichert.¦已取消加载，你的信息仍已保存。
Die gewählten Inhalte sind zu groß für den Speicher dieses Geräts. Bitte weniger Sprachen wählen.¦所选内容过大，超出本设备内存，请减少语言数量。
Zu wenig freier Speicherplatz auf dem Gerät. Bitte Platz schaffen (z. B. unbenutzte Apps löschen) und erneut versuchen.¦设备可用空间不足，请释放空间（例如删除不用的应用）后重试。
Noch einmal Zurück drücken zum Abbrechen¦再按一次返回键取消
Deine Angaben sind gespeichert – bei einer Unterbrechung geht es beim nächsten Start weiter.¦你的信息已保存 —— 若中断，下次启动会继续加载。
Bitte mindestens eine Sprache wählen.¦请至少选择一种语言。
Passt in den Speicher dieses Geräts.¦可装入本设备内存。
Das Laden wurde mehrmals unterbrochen. Deine Angaben sind gespeichert – drücke „Hinzufügen“, um es erneut zu versuchen.¦加载多次中断，你的信息已保存 —— 按「添加」重试。
Meine Sprache zeigen¦显示我的语言
Welche Inhalte möchtest du sehen?¦想看什么？
Diese Playlist ist sehr groß: {#0} Titel in {#1} Sprachen.¦此播放列表非常大：{#0} 个节目，{#1} 种语言。
Wähle die Sprachen, die du sprichst – dann zeigt dir die App nur Filme, Serien und Sender in diesen Sprachen. Das Laden geht deutlich schneller und braucht weniger Speicher. Ändern kannst du das jederzeit unter Einstellungen > Sprachen.¦选择你使用的语言 —— 应用随后只显示这些语言的电影、剧集和频道。加载会快很多，也更省内存。可随时在「设置 > 语言」中修改。
Alles laden¦全部加载
„Alles laden“ lädt alle {#0} Titel. Das kann einige Minuten dauern.¦「全部加载」会加载全部 {#0} 个节目，可能需要几分钟。
Markiere die Sprachen, deren Titel du sehen möchtest. Erkannt wird die Kennzeichnung deines Anbieters (z. B. „DE |“ oder „[TR]“) – bei IPTV ist das die Sprache der Tonspur, nicht das Herkunftsland.¦选择你想观看的内容所属语言。应用依据服务商标签判断（例如「DE |」或「[TR]」）—— 对 IPTV 而言这指的是音轨语言，而非制片国家。
Gewählt: {#0} Titel¦已选择：{#0} 个节目
Sehr groß für dieses Gerät – das Laden kann lange dauern.¦对本设备来说过大 —— 加载可能需要很久。
Diese Auswahl ist sehr groß für dieses Gerät – das Laden kann lange dauern.¦此选择对本设备来说过大 —— 加载可能需要很久。
Die Sprachauswahl erscheint, sobald der Download fertig ist.¦下载完成后会出现语言选择。
Das dauert ungewöhnlich lange. Mit der Sprachauswahl lädst du nur die Titel, die du wirklich brauchst.¦这次耗时异常长。使用语言选择可只加载真正需要的节目。
Sprachen wählen¦选择语言
Weiter warten¦继续等待
Aus = Kategorien deines Anbieters (Standard). Umschalten geht auch direkt in der Ansicht „Kategorien“.¦关 = 使用服务商分类（默认）。也可以直接在「分类」视图中切换。
Folgen {0}¦分集 {0}
{#0} Abschnitte¦{#0} 个分段
Links/Rechts = Abschnitt wechseln¦左/右 = 切换分段
Zu Folge springen¦跳转到指定集
Gehe zu Folge …¦跳转到第 … 集
Folgennummer¦集数
Springen¦跳转
Fortsetzen¦继续播放
Gesehen¦已观看
Weiter mit {0}¦继续观看 {0}
Als Nächstes¦接下来
Menü-Taste oder OK lange drücken = entfernen¦菜单键或长按确认键 = 移除
Aus „Weiterschauen“ entfernt¦已从「继续观看」中移除
Rechts / Vorspulen = 50 weiter¦右 / 快进 = 前进 50
Zurückspulen = zurück¦快退 = 返回
Links schließt¦按左键关闭
Italienisch¦意大利语
Portugiesisch¦葡萄牙语
Polnisch¦波兰语
Russisch¦俄语
Arabisch¦阿拉伯语
Niederländisch¦荷兰语
Keine¦无
Zweite Tonspur¦第二音频语言
Wie die App-Sprache¦与应用语言相同
Immer an¦始终开启
Nur bei fremder Tonspur¦仅外语时
Sprache der Untertitel¦字幕语言
Untertitel für Hörgeschädigte (CC)¦听障字幕（CC）
Bevorzugt Untertitel mit Geräuschen und Sprecherangaben ([Musik], [Tür knallt]), wenn der Stream welche hat.¦当视频流提供时，优先选择带声音提示和说话人标注（[音乐]、[关门声]）的字幕。
„Nur bei fremder Tonspur“: Läuft ein Titel in einer Sprache, die du nicht eingestellt hast (z. B. Japanisch), erscheinen Untertitel automatisch; bei deiner Sprache bleiben sie aus. Wählst du im Player unter Audio & Untertitel von Hand, merkt sich die App das für diese Serie bzw. diesen Film – die nächste Folge startet gleich richtig.¦「仅外语时」：如果节目以你未设置的语言播放（例如日语），字幕会自动出现；是你的语言时则保持关闭。如果你在播放器「音频与字幕」中手动选择，应用会记住该选择，下一集直接沿用。
Quelle & Qualität¦来源与画质
Bei mehreren Quellen¦存在多个来源时
Automatisch (passend zum Gerät)¦自动（适配本设备）
Immer die höchste¦始终最高
Höchstens Full HD¦最高全高清
Höchstens HD (spart Daten)¦最高高清（省流量）
Bei Störung automatisch wechseln¦出问题时自动切换
Ruckelt ein Titel mehrmals oder lässt er sich nicht abspielen, nimmt die App eine andere (kleinere) Quelle desselben Titels – an derselben Stelle.¦如果节目反复卡顿或无法播放，应用会在同一位置切换到该节目的另一个（较小的）来源。
Dieses Gerät¦本设备
4K-Bildschirm¦4K 屏幕
Full-HD-Bildschirm¦全高清屏幕
HD-Bildschirm¦高清屏幕
Bildschirm unbekannt¦屏幕未知
4K-Decoder¦4K 解码
ja¦是
nein¦否
Gelernt: 4K ruckelte hier mehrmals – Automatik nimmt höchstens Full HD.¦已学习：4K 在此多次卡顿 —— 现在自动最高选全高清。
Viele Anbieter haben denselben Film mehrfach (4K, FHD, HD). „Automatisch“ nimmt die beste Quelle, die dein Gerät flüssig schafft: 4K nur mit 4K-Bildschirm und passendem Decoder. Eine feste Wahl auf der Detailseite hat immer Vorrang.¦很多服务商会提供同一部电影的多个版本（4K、FHD、HD）。「自动」选择本设备能流畅播放的最佳来源：只有 4K 屏幕加上合适解码器才会选 4K。详情页上的固定选择始终优先。
Defekte Links¦失效链接
Keine gestörten Titel bekannt.¦未发现失效节目。
{#0} Titel als gestört gemerkt¦{#0} 个节目被标记为失效
Gestörte Titel ausblenden¦隐藏失效节目
Blendet Filme und Sender aus, die beim Abspielen „nicht mehr vorhanden“ meldeten oder mehrmals scheiterten. Sonst tragen sie ein Warnsymbol.¦隐藏曾报告「已下架」或多次失败的电影和频道，否则它们会显示警告标志。
Liste leeren¦清空列表
Die App prüft nicht von sich aus Tausende Links – das würde dein Anbieter als Missbrauch werten. Sie merkt sich nur, was beim Abspielen schiefging. Ohne Internet, bei Zugangs- oder Serverproblemen wird nichts gemerkt. Spielt ein Titel wieder, ist er sofort freigegeben.¦应用不会自行检查成千上万个链接 —— 服务商会视其为滥用。它只记住播放时出过的问题；无网络、登录或服务器故障时不做记录。节目一旦能正常播放，记录即被清除。
Bildspur¦视频轨道
{#0} Quellen¦{#0} 个来源
Gestört¦失效
{0} – wechsle auf {1}¦{0} —— 正在切换到 {1}
Quelle gestört¦来源失效
Ruckelt¦卡顿
Format nicht abspielbar¦格式无法播放
andere Quelle¦其他来源
{0} passt nicht zu diesem Gerät – spiele {1}¦{0} 不适合本设备 —— 正在播放 {1}
Diese Qualität¦此画质
Quelle gestört – spiele eine andere¦来源失效 —— 正在播放其他来源
bei Störung wird automatisch gewechselt¦出问题时自动切换
Diese Quelle war zuletzt nicht erreichbar – es wird eine andere genommen.¦此来源上次无法访问 —— 将改用其他来源。
Diese Quelle war zuletzt nicht erreichbar.¦此来源上次无法访问。
Spulen, Intro, Qualität, defekte Links¦快进、片头、画质、失效链接
Sprachen, CC, Untertitel-Stil¦语言、CC、字幕样式
Neue Senderliste¦新建频道列表
Neue Senderliste …¦新建频道列表 …
Senderliste {#0}¦频道列表 {#0}
Liste löschen¦删除列表
Liste umbenennen¦重命名列表
Name¦名称
Speichern¦保存
Zu Favoriten¦加入收藏
Nach oben¦上移
Nach unten¦下移
Aus „{0}“ entfernen¦从「{0}」中移除
In „{0}“ – entfernen¦已在「{0}」中 —— 移除
Zu „{0}“¦添加到「{0}」
Erinnern¦提醒我
Erinnerung löschen¦删除提醒
Sender jetzt ansehen¦立即观看此频道
Erinnerung gesetzt – {#0} Min vorher¦已设置提醒 —— 提前 {#0} 分钟
Erinnerung gelöscht¦提醒已删除
Erinnerung¦提醒
Erinnerungen¦提醒列表
ansehen / später: erinnern¦观看 / 稍后：提醒
Läuft jetzt¦正在播出
In {#0} Min¦{#0} 分钟后
Antippen öffnet den Sender¦点击打开该频道
Umschalten¦切换
Schließen¦关闭
Eigene Senderlisten¦自建频道列表
Noch keine eigenen Listen.¦还没有自建列表。
Zum Beginn¦在开始时
{#0} Min vorher¦提前 {#0} 分钟
Keine Erinnerungen gesetzt.¦未设置提醒。
Im TV-Guide auf einer späteren Sendung OK drücken > „Erinnern“. Ist die App offen, erscheint eine Karte mit „Umschalten“ (auch während du etwas anderes schaust). Ist sie geschlossen, kommt eine Benachrichtigung – antippen öffnet den Sender. Das Gerät muss dafür eingeschaltet sein (Standby reicht meist); manche Geräte zeigen Benachrichtigungen nur in ihrer Mitteilungsliste.¦在节目单中对之后的节目按确认键 > 「提醒我」。如果应用开着，会出现带「切换」的卡片（即使你正在看别的）；如果应用已关闭，会收到通知 —— 点按即可打开该频道。设备需处于开机状态（待机通常也可以）；部分设备只在通知列表里显示通知。
TV-Programm, Senderlisten, Erinnerungen¦节目单、频道列表、提醒
Film, Serie oder Sender sagen …¦说出电影、剧集或频道名称 …
Sprechen¦语音输入
Mit dem Handy suchen¦用手机搜索
Öffne auf dem Handy (gleiches WLAN):¦在手机上打开（同一 Wi-Fi）：
Code: {#0}¦验证码：{#0}
Dann tippen oder auf das Mikrofon der Handy-Tastatur tippen und sprechen. Der Begriff erscheint sofort hier.¦然后直接输入，或点手机键盘上的麦克风说话，关键词会立刻显示在这里。
Gesucht¦已搜索
Film, Serie oder Sender¦电影、剧集或频道
Tipp: Tippe auf das Mikrofon deiner Handy-Tastatur und sprich den Titel.¦提示：点手机键盘上的麦克风，说出片名即可。
Läuft gerade ein Film oder Sender, wartet die Aktualisierung, bis du fertig bist – so ruckelt nichts.¦如果正在播放电影或频道，更新会等你结束再继续 —— 避免卡顿。
Abdunkeln¦变暗
Ausblenden¦隐藏
Archiv¦回看
Archiv (Catchup)¦回看（时移）
Archiv: verpasste Sendungen¦回看：错过的节目
IM ARCHIV¦回看中
Deine Playlist meldet keine Sender mit Archiv.¦你的播放列表里没有支持回看的频道。如果服务商提供回看（时移），重新加载播放列表后这些频道会出现在这里。
{#0} Sender mit Archiv¦{#0} 个支持回看的频道
{#0} Tage¦{#0} 天
1 Tag¦1 天
Diese Sendung ist im Archiv des Anbieters nicht abrufbar.  Zurück = Live-Sender.¦此节目无法从服务商的回看中播放。返回键 = 回到直播。
Kein Programm bekannt¦无节目信息
Keine vergangenen Sendungen im Programm.¦节目单中没有已播节目。
Nicht im Archiv verfügbar¦回看中不可用
Nächste Sendung: Live¦下一个节目：直播
Nächste Sendung: {0}¦下一个节目：{0}
Vorherige Sendung¦上一个节目
Vorherige Sendung: {0}¦上一个节目：{0}
Von Anfang an¦从头开始
ansehen / Archiv / erinnern¦观看 / 回看 / 提醒
Sender mit Archiv tragen das Archiv-Symbol. Im TV-Guide lassen sich vergangene Sendungen dieser Sender mit OK abspielen (Spulen-Tasten = einen Tag zurück/vor). Im Player: „Von Anfang an“ startet die laufende Sendung neu, „Archiv“ listet die verpassten Sendungen. Am Ende einer Sendung geht es mit der nächsten weiter, Zurück führt zum Live-Bild. Welche Sender ein Archiv haben und wie viele Tage es reicht, bestimmt dein Anbieter.¦支持回看的频道会显示回看图标。在节目单中按确认键可播放这些频道的已播节目（快退/快进键 = 前后一天）。播放器中「从头开始」会重播当前节目，「回看」列出错过的节目。节目结束后自动播放下一个；返回键回到直播画面。哪些频道支持回看、可回看多少天，由你的服务商决定。
Rating: beste zuerst¦评分：从高到低
Rating: schwächste zuerst¦评分：从低到高
Jahr: neueste zuerst¦年份：从新到旧
Jahr: älteste zuerst¦年份：从旧到新
Noch keine Favoriten. Auf einem Sender OK gedrückt halten und „Zu Favoriten“ wählen.¦暂无收藏。长按频道确认键并选择「加入收藏」。
Diese Liste ist noch leer. In „Alle Sender“ oder einer Kategorie auf einem Sender OK gedrückt halten und „Zu …“ wählen.¦此列表还是空的。在「全部频道」或某个分类中长按频道确认键并选择「添加到 …」。
Sender als Favorit: auf dem Sender OK gedrückt halten > „Zu Favoriten“. Kategorie als Favorit: auf der Kategorie OK gedrückt halten.¦收藏频道：长按频道确认键 > 「加入收藏」。收藏分类：长按分类确认键。
halten = Favorit / Listen¦长按 = 收藏 / 列表
Anlegen: Live TV > „Neue Senderliste“ oder auf einem Sender OK gedrückt halten > „Neue Senderliste …“. OK gedrückt halten auf einem Sender: Favorit, zu einer Liste hinzufügen oder entfernen, in der Liste nach oben/unten schieben. OK gedrückt halten auf einer Liste: umbenennen oder löschen. Hat deine Fernbedienung eine Menü-Taste, geht es auch damit. Im Player wechselst du mit Links zu deinen Listen; Umschalten bleibt in der Liste.¦创建：直播电视 > 「新建频道列表」，或长按频道确认键 > 「新建频道列表 …」。长按频道确认键可收藏、加入/移出列表、在列表内上移/下移。长按列表确认键可重命名或删除。遥控器若有菜单键也同样有效。播放器中按左键可进入列表，换台只在列表内进行。
Abschnitt¦分段
Staffel¦季
1 Staffel¦1 季
{#0} Staffeln¦{#0} 季
1 Folge¦1 集
{#0} Folgen¦{#0} 集
Alle Einstellungen zurücksetzen¦重置所有设置
Jetzt prüfen¦立即检查
Letzten Film / letzte Serie beim Start fortsetzen¦启动时续播上次电影 / 剧集
Wenn du zuletzt einen Film oder eine Serie gesehen hast, läuft sie nach dem Start an der gemerkten Stelle weiter (bei einer fertigen Folge mit der nächsten).¦如果上次看的是电影或剧集，启动后会从中断处继续（一集播完则接着下一集）。
Aktuelles Design¦当前外观
Markiere eine Farbe, um sie hier zu sehen.¦高亮某个颜色即可在此预览。
Nur Vorschau – OK übernimmt das Design.¦仅预览 —— 按确认键应用该外观。
Neu¦新增
Automatische Genres, Kategorien¦自动类型、分类
Verwalten, Sprachen, Aktualisieren¦管理、语言、更新
Sprachen der Inhalte öffnen¦打开内容语言
Welche Sprachen geladen werden, stellst du unter „Playlists“ ein – das bestimmt, was aus der Playlist geladen wird.¦你在「播放列表」中选择加载哪些语言 —— 这决定了从播放列表中载入什么内容。
Deine Bibliothek ist für automatische Genres zu groß (über 60.000 Filme und Serien). Es werden die Kategorien deines Anbieters verwendet. Blende unter „Playlists“ > „Sprachen der Inhalte“ Sprachen aus, die du nicht brauchst – dann passt es wieder.¦你的媒体库过大，无法自动识别类型（超过 60,000 部电影和剧集），将改用服务商的分类。请在「播放列表 > 内容语言」中隐藏不需要的语言，之后即可重新启用。
Alle an¦全部开启
Alle aus¦全部关闭
Übernehmen und laden¦应用并加载
Verwerfen¦放弃
{#0} von {#1} Sprachen gewählt¦已选择 {#0} / {#1} 种语言
{#0} Sender¦{#0} 个频道
{#0} Filme¦{#0} 部电影
{#0} Serien¦{#0} 部剧集
Aktuelle Liste¦当前列表
Archiv deines Anbieters¦服务商的回看
Aufnahmen¦录制
Aus dem Archiv ansehen¦从回看观看
Beliebte Filme¦热门电影
Beliebte Serien¦热门剧集
Deine Playlist meldet keine Sender mit Archiv. Wenn dein Anbieter ein Archiv (Catchup) anbietet, erscheinen die Sender hier nach dem nächsten Laden der Playlist.¦你的播放列表里没有支持回看的频道。如果服务商提供回看（时移），重新加载播放列表后这些频道会出现在这里。
Hallo, {0}¦你好，{0}
Keine Erinnerungen¦无提醒
Live¦直播
Nächste Sendungen¦接下来的节目
Sendungen aus dem Archiv deines Anbieters¦服务商回看中的节目
Suche nach Sendern, Filmen, Serien …¦搜索频道、电影、剧集 …
Home¦首页
EPG¦EPG
TV-Programm¦节目单
Deine Auswahl¦你的精选
Noch nichts in deiner Liste. Auf der Seite eines Films oder einer Serie „Meine Liste“ wählen.¦列表中还没有内容。在电影或剧集详情页选择「我的列表」。
Zur Liste¦加入列表
OK lange drücken = entfernen¦长按确认键 = 移除
Heute¦今天
Gestern¦昨天
Zu Favoriten hinzugefügt¦已加入收藏
Aus Favoriten entfernt¦已取消收藏
Links/Rechts = Spalte  ·  Zurück schließt¦左/右 = 切列 · 返回关闭
laufender Sender, Kategorien, schließen¦当前频道、分类、关闭
Zurück: zum laufenden Sender, dann zu den Kategorien, dann schließen.¦返回：先到当前频道，再到分类，最后关闭。
Tag¦天
Beispieltext¦示例文字
Bilder bei Filmen und Serien¦电影和剧集的图片
Bildschirm füllen¦铺满屏幕
Cover¦封面
Favorit¦收藏
Favoriten & Listen¦收藏与列表
Intro-Zeiten stammen aus der Community-Datenbank TheIntroDB und gibt es nicht für jede Serie. Die Stream-Info erscheint nur bei Filmen und Serien.¦片头时间来自社区数据库 TheIntroDB，并非所有剧集都有。流信息仅对电影和剧集显示。
Kartengröße¦卡片大小
Markiere eine Einstellung, um sie hier zu sehen.¦高亮某个设置即可在此预览。
Noch keine Favoriten. Auf einem Sender OK gedrückt halten, bei Filmen und Serien auf der Detailseite „Zu Favoriten“ wählen.¦暂无收藏。长按频道确认键；电影和剧集请在详情页选择「加入收藏」。
Sender, Kategorie, schließen¦频道、分类、关闭
Szenenbild¦静态图
Zoom 115 %¦缩放 115 %
Zoom 133 %¦缩放 133 %
Zurück schließt dieses Fenster¦返回键关闭此窗口
Zurück: zum Sender, dann zur Kategorie, dann schließen.¦返回：先到频道，再到分类，最后关闭。
Oben findest du alle Filme, Neuheiten, Top bewertet und Kategorien.¦顶部可以找到全部电影、新上架、高分和分类。
Oben findest du alle Serien, Neuheiten, Top bewertet und Kategorien.¦顶部可以找到全部剧集、新上架、高分和分类。
Titel pro Zeile¦每行节目数
Schriftgröße (gilt für die ganze App)¦字号（适用于整个应用）
Keine Benachrichtigungen¦无通知
Erinnerung setzen¦设置提醒
Ansehen¦观看
Ansehen / Erinnerung¦观看 / 提醒
Alles rund um Aussehen, Wiedergabe und Konto¦关于外观、播放和账户的一切
{#0} Episoden¦{#0} 集
1 Episode¦1 集
Neue Folge · Staffel {#0}, Folge {#1}¦新一集 · 第 {#0} 季第 {#1} 集
{#0} neue Folgen · bis Staffel {#1}, Folge {#2}¦新增 {#0} 集 · 截至第 {#1} 季第 {#2} 集
Es gibt nur {#0} Folgen¦只有 {#0} 集
Verlauf · Filme¦历史 · 电影
Verlauf · Serien¦历史 · 剧集
Noch nichts angesehen. Hier erscheinen die zuletzt gestarteten Filme und Serien.¦还没有观看记录。最近开始看的电影和剧集会显示在这里。
Erneut abspielen¦再次播放
Neue Folge¦新一集
Vorschau beim Markieren¦焦点预览
Top 10 · Im Trend¦前十 · 热门
Neu hinzugefügt · Filme¦最近添加 · 电影
Top bewertet · Filme¦高分 · 电影
Neu hinzugefügt · Serien¦最近添加 · 剧集
Top bewertet · Serien¦高分 · 剧集
Favoriten · Sender¦收藏 · 频道
Favoriten · Filme¦收藏 · 电影
Favoriten · Serien¦收藏 · 剧集
Meine Liste · Filme¦我的列表 · 电影
Meine Liste · Serien¦我的列表 · 剧集
Jetzt ansehen¦立即观看
Sender hinzufügen¦添加频道
Fertig¦完成
Live TV · Jetzt und in den nächsten Stunden¦直播电视 · 现在与未来几小时
Zurück: Sendung, Sender, Kategorie, dann schließen¦返回：节目、频道、分类，然后关闭
OK = ansehen  ·  halten = Favorit  ·  Links = Kategorien¦确认键 = 观看 · 长按 = 收藏 · 左键 = 分类
Anzeigen¦显示
Schrift: Deckkraft¦文字：不透明度
Hintergrund: Deckkraft¦背景：不透明度
Uhrzeit im Bild¦屏上时钟
Technik-Info¦技术信息
Abspann automatisch überspringen¦自动跳过片尾
Uhrzeit: Ort¦时钟：位置
Uhrzeit: Schrift-Deckkraft¦时钟：文字不透明度
Uhrzeit: Größe¦时钟：大小
Uhrzeit: Hintergrund¦时钟：背景
Uhrzeit: Hintergrund-Deckkraft¦时钟：背景不透明度
Dunkler Kasten hinter der Uhr. Aus = nur die Zahlen.¦时钟后面的深色底板。关 = 只显示数字。
Puffer (bei langsamem Internet)¦缓冲（用于网络较慢时）
Bildrate anpassen¦匹配帧率
Ruckelt – Puffer wird beim nächsten Start größer¦出现卡顿 —— 下次会增大缓冲
App-Aktualisierung¦应用更新
Adresse der Update-Datei (https://…/update.json)¦更新文件地址（https://…/update.json）
Nach Update suchen¦检查更新
Herunterladen und installieren¦下载并安装
Du hast die neueste Version.¦已是最新版本。
Bitte warten …¦请稍候 …
Installation gestartet¦已开始安装
Verbindung hängt – neuer Versuch¦连接停滞 —— 正在重试
Titel pro Zeile (Kino)¦每行节目数（影院）
Rechts = nächste Staffel  ·  Links = Staffeln¦右 = 下一季 · 左 = 选季
Outfit¦Outfit
Inter¦Inter
System¦系统
Noch keine Favoriten¦暂无收藏
Zu Live TV¦前往直播电视
Noch nichts in deiner Liste¦列表中还没有内容
Filme entdecken¦发现电影
Noch nichts angesehen¦还没有观看记录
Hier erscheinen die zuletzt gestarteten Filme und Serien.¦最近开始看的电影和剧集会显示在这里。
Auf einem Sender OK gedrückt halten, bei Filmen und Serien auf der Detailseite „Zu Favoriten“ wählen.¦长按频道确认键；电影和剧集请在详情页选择「加入收藏」。
Auf der Seite eines Films oder einer Serie „Meine Liste“ wählen.¦在电影或剧集详情页选择「我的列表」。
Keine Treffer¦没有结果
Mit diesen Filtern gibt es hier keine Titel.¦当前筛选条件下没有节目。
Filter zurücksetzen¦重置筛选
Diese Liste ist noch leer¦此列表还是空的
Auf einem Sender OK gedrückt halten und „Zu Favoriten“ wählen.¦长按频道确认键并选择「加入收藏」。
In „Alle Sender“ oder einer Kategorie auf einem Sender OK gedrückt halten und „Zu …“ wählen.¦在「全部频道」或某个分类中长按频道确认键并选择「添加到 …」。
In dieser Kategorie gibt es keine Sender. Links eine andere Kategorie wählen.¦此分类下没有频道，请在左侧选择其他分类。
Prüfe die Schreibweise oder suche nach einem Teil des Titels. Kleine Tippfehler findet die Suche trotzdem.¦请检查拼写或搜索片名的一部分，少量错别字也能匹配。
Outfit (Standard) und Inter sind in die App eingebaut, die übrigen Schriften stammen von deinem Gerät.¦Outfit（默认）和 Inter 内置于应用，其余字体来自你的设备。
Top 10 heute¦今日前十
Danach¦下一个
Für diesen Sender gibt es kein Programm.¦此频道没有节目单。
FILM¦电影
SERIE¦剧集
Runter für Details und Abspielen¦按向下键查看详情并播放
Links/Rechts = {#0} s springen¦左/右 = 快进 {#0} 秒
2020er¦2020 年代
2010er¦2010 年代
2000er¦2000 年代
1990er¦1990 年代
Infos auf dem Poster¦海报上的信息
Breites Szenenbild¦宽幅剧照
Neu: Version {0}¦新版本：{0}
Fehler {#0}¦错误 {#0}
Keine Update-Adresse eingetragen¦未填写更新地址
Update-Datei unvollständig¦更新文件内容不完整
Leere Antwort¦响应为空
Lade … {#0} %¦加载中 … {#0} %
Erlaube der App einmal „Apps installieren“ und tippe dann erneut.¦请先允许本应用「安装应用」权限，然后重试。
Trage die Adresse einer kleinen Datei update.json ein, die du selbst bereitstellst: {"versionCode": 2, "versionName": "1.1", "apk": "ipvio.apk"}. Die neue APK muss mit demselben Schlüssel signiert sein wie die installierte, sonst lehnt Android sie ab.¦填入你自己提供的小文件 update.json 的地址：{"versionCode": 2, "versionName": "1.1", "apk": "ipvio.apk"}。新 APK 必须与已安装版本使用同一签名，否则 Android 会拒绝安装。
Startet die nächste Folge, sobald der Abspann beginnt. Geht nur, wenn für die Serie Abspann-Zeiten bekannt sind (TheIntroDB); sonst bleibt es beim Hinweis in den letzten 30 Sekunden.¦片尾一开始便自动播放下一集。仅当该系列已知片尾时间（TheIntroDB）时有效；否则只在最后 30 秒给出提示。
Zeigt die aktuelle Uhrzeit beim Abspielen in einer Ecke (nicht, solange die Steuerleiste offen ist).¦播放时在角落显示当前时间（控制栏打开时不显示）。
Oben rechts¦右上
Oben links¦左上
Unten rechts¦右下
Unten links¦左下
Die Uhr folgt der Zeitzone und dem 12/24-Stunden-Format aus den Einstellungen. Im Player geht es auch über Einstellungen > Uhrzeit.¦时钟遵循设置中的时区和 12/24 小时制。播放器中也可用「设置 > 时钟」调整。
Ein größerer Puffer lädt mehr Film im Voraus und verhindert Ruckeln bei schwacher Leitung; dafür dauert der Start etwas länger und es wird mehr Speicher gebraucht. „Automatisch“ beginnt normal und vergrößert den Puffer von selbst, wenn es mehrfach ruckelt (aktuell Stufe {#0}). Gilt ab dem nächsten gestarteten Titel.¦更大的缓冲会预加载更多影片内容，避免网络不佳时卡顿；代价是启动稍慢、占用更多内存。「自动」按常规启动，若反复卡顿会自行增大缓冲（当前级别 {#0}）。从下一个开始播放的节目起生效。
Passt die Bildwiederholrate des Fernsehers an den Film an (z. B. 24 Bilder), wenn der Fernseher das ohne Bildaussetzer kann. Bei Fire TV zusätzlich in den Geräteeinstellungen „Originale Bildrate anpassen“ prüfen.¦在电视支持且不会黑屏的前提下，把电视刷新率匹配到影片（例如 24 帧）。Fire TV 还需在设备设置中勾选「匹配原始帧率」。
{#0} min¦{#0} 分钟
Quelle {#0}¦来源 {#0}
Tonspur „{0}“ ist mit diesem Ausgang nicht abspielbar – andere Spur im Menü wählen¦音轨「{0}」无法通过当前输出播放 —— 请在菜单中选择其他音轨
Diese Tonspur kann hier nicht abgespielt werden – andere Spur gewählt¦此音轨无法在此播放 —— 已自动选择其他音轨
Verbindung hängt – neuer Versuch ({#0}/2)¦连接停滞 —— 正在重试（{#0}/2）
Video-Rate (laut Datei): {0}¦视频帧率（按文件）：{0}
Ton: {0}¦音频：{0}
Puffer: {0}¦缓冲：{0}
Auto (5)¦自动 (5)
Weil du „{0}“ geschaut hast¦因为你观看了「{0}」
Ähnlich wie {0}¦与 {0} 相似
Oben findest du alle {0}, Neuheiten, Top bewertet und Kategorien.¦顶部可以找到全部 {0}、新上架、高分和分类。
Fortsetzen · {#0} Min¦续播 · {#0} 分钟
Weiter mit S{#0} · Folge {#1}¦继续第 {#0} 季 · 第 {#1} 集
noch nichts gemerkt¦暂无记忆记录
Liste angelegt. Mit „Sender hinzufügen“ füllen.¦列表已创建，用「添加频道」填充它。
Zu „¦添加到「
“ hinzufügen: OK auf einem Sender fügt ihn hinzu bzw. entfernt ihn wieder¦」：在频道上按确认键可添加或移除
Entfernt¦已移除
Hinzugefügt¦已添加
Diese Playlist ist sehr groß: {0} Titel in {#1} Sprachen.¦此播放列表非常大：{0} 个节目，{#1} 种语言。
„Alles laden“ lädt alle {0} Titel. Das kann einige Minuten dauern.¦「全部加载」会加载全部 {0} 个节目，可能需要几分钟。
Gewählt: {0} Titel¦已选择：{0} 个节目
Das dauert gerade sehr lange.¦这次耗时非常久。
Sehr große Playlists brauchen lange. Mit der Sprachauswahl lädst du nur die Titel, die du wirklich brauchst.¦超大播放列表可能耗时极长。使用语言选择，只加载真正需要的节目。
Server nicht gefunden (DNS)¦找不到服务器 (DNS)
Sichere Verbindung fehlgeschlagen. Datum und Uhrzeit des Geräts prüfen¦安全连接失败，请检查设备日期和时间
Zeitüberschreitung, Server antwortet nicht¦超时，服务器无响应
API-Schlüssel¦API 密钥
TMDB, IMDb und Rotten Tomatoes¦TMDB、IMDb 和烂番茄
Die App enthält keine eigenen Schlüssel. Mit deinen eigenen, kostenlosen Schlüsseln zeigt sie Poster, Beschreibungen, Besetzung, Trailer, automatische Genres sowie IMDb- und Rotten-Tomatoes-Wertungen. Ohne Schlüssel übernimmt sie Beschreibung, Cover und Bewertung von deinem Anbieter (bei Xtream-Zugängen).¦应用不附带任何密钥。填入你自己的免费密钥后，可显示海报、简介、演员、预告片、自动类型以及 IMDb 和烂番茄评分。没有密钥时（Xtream 登录）会从服务商获取简介、封面和评分。
TMDB (Film-Infos)¦TMDB（影片信息）
TMDB API-Schlüssel (v3)¦TMDB API 密钥（v3）
Kostenlos unter themoviedb.org: Konto anlegen, anmelden, dann Einstellungen > API > API-Schlüssel anfordern und den 32-stelligen „API Key“ eintragen. Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert.¦在 themoviedb.org 免费获取：注册并登录，然后「设置 > API > 申请 API 密钥」，填入 32 位的「API Key」。本产品使用 TMDB API，但未获得 TMDB 认可或认证。
OMDb (IMDb- und Rotten-Tomatoes-Wertung)¦OMDb（IMDb 和烂番茄评分）
OMDb API-Schlüssel¦OMDb API 密钥
Kostenlos unter omdbapi.com > API Key (FREE). Danach den Aktivierungslink in der E-Mail anklicken, sonst funktioniert der Schlüssel nicht. Geht auch ohne TMDB-Schlüssel.¦在 omdbapi.com > API Key (FREE) 免费申请。之后务必点击邮件中的激活链接，否则密钥无效。不填 TMDB 密钥也能使用。
Speichern und prüfen¦保存并验证
Schlüssel entfernen¦移除密钥
Schlüssel gültig¦密钥有效
Schlüssel ungültig¦密钥无效
Keine Verbindung¦无网络连接
Die Schlüssel bleiben nur auf diesem Gerät. Nach dem Eintragen die App einmal neu starten, damit alle Bereiche die Infos laden. Intro- und Abspann-Zeiten kommen von TheIntroDB und brauchen keinen Schlüssel.¦密钥仅保存在本设备。填入后请重启一次应用，让所有版块加载信息。片头和片尾时间来自 TheIntroDB，无需密钥。
Dieses Produkt verwendet die TMDB-API, wird aber nicht von TMDB unterstützt oder zertifiziert. Wertungen: OMDb. Intro-Zeiten: TheIntroDB.¦本产品使用 TMDB API，但未获得 TMDB 认可或认证。评分：OMDb。片头时间：TheIntroDB。
IPVIO Stream ist freie Software unter der GNU General Public License v3 (GPL-3.0). Verwendete Bibliotheken: AndroidX/Jetpack Compose, Media3 (Apache 2.0), OkHttp, Coil (Apache 2.0), Jellyfin FFmpeg-Decoder (GPL-3.0), Schriften Outfit und Inter (SIL Open Font License).¦IPVIO Stream 是 GNU 通用公共许可证 v3（GPL-3.0）下的自由软件。使用的库：AndroidX/Jetpack Compose、Media3 (Apache 2.0)、OkHttp、Coil (Apache 2.0)、Jellyfin FFmpeg 解码器 (GPL-3.0)，字体 Outfit 与 Inter (SIL Open Font License)。
Automatische Genres brauchen einen TMDB-Schlüssel (Einstellungen > API-Schlüssel).¦自动类型识别需要 TMDB 密钥（设置 > API 密钥）。
Tipp: Unter Einstellungen > API-Schlüssel einen eigenen TMDB-Schlüssel eintragen, dann erscheinen hier Beschreibung, Cover und Bewertungen.¦提示：在「设置 > API 密钥」中填入自己的 TMDB 密钥，即可在此看到简介、封面和评分。
Zugang wird geprüft …¦正在检查账号 …
Sender werden geladen …¦正在加载频道 …
Filme werden geladen …¦正在加载电影 …
Serien werden geladen …¦正在加载剧集 …
Serien werden geladen ({#0}/{#1}) …¦正在加载剧集（{#0}/{#1}）…
Die Schnittstelle des Anbieters antwortet nicht wie erwartet.¦服务商接口未按要求响应。
Zugang abgelehnt. Benutzername, Passwort oder Abo prüfen.¦访问被拒，请核对用户名、密码或订阅状态。
Der Server hat keine gültige Playlist geliefert.¦服务器未返回有效的播放列表。
Update verfügbar: Version {0}¦有可用更新：版本 {0}
Update wird geladen …¦正在下载更新 …
Installiert: Version {0}¦已安装：版本 {0}
Automatisch nach Updates suchen¦自动检查更新
Einmal am Tag. Ein neues Update erscheint in der Glocke oben rechts.¦每天一次。有新更新时右上角的铃铛会提示。
Jetzt nach Update suchen¦立即检查更新
Updates kommen von der Projektseite auf GitHub. Deine Playlists, Favoriten, der Verlauf und die Einstellungen bleiben bei einem Update erhalten. Android fragt vor der Installation einmal nach.¦更新来自 GitHub 项目页。升级时播放列表、收藏、历史和设置都会保留。安装前 Android 会询问一次。
Eigene Update-Adresse (leer = GitHub)¦自定义更新地址（留空 = GitHub）
Noch keine Veröffentlichung gefunden¦尚未发现新版本
Dieses Stream-Format wird nicht unterstützt.¦不支持此视频流格式。
Verbindung hängt – neuer Versuch ({#0})¦连接停滞 —— 正在重试（{#0}）
Verbindung unterbrochen – neuer Versuch ({#0})¦连接中断 —— 正在重试（{#0}）
Stream beendet – verbinde neu …¦视频流结束 —— 正在重新连接 …
Das Portal hat keine Abspiel-Adresse geliefert. Später erneut versuchen.¦门户未返回播放地址，请稍后重试。
Zu wenig freier Speicher für den Live-Puffer¦可用空间不足，无法创建直播缓冲
Zu wenig freier Speicher¦可用空间不足
Aufnahme läuft weiter – dafür braucht dein Anbieter 2 gleichzeitige Verbindungen¦录制继续进行 —— 这需要服务商允许 2 个同时连接
30 s zurück¦后退 30 秒
Aufnehmen¦录制
Aufnahme beenden¦停止录制
Aufnahme gestartet¦已开始录制
Aufnahme beendet¦已停止录制
Aufnahme läuft¦录制中
Sender unbekannt¦未知频道
Aufnahmen ({#0})¦录制内容（{#0}）
Zurückspulen und Aufnehmen¦快退并录制
Live TV zurückspulen¦直播快退
Speichert den laufenden Sender auf dem Gerät. Dann kannst du pausieren, ohne etwas zu verpassen, mit den Spultasten zurückspringen und mit „Zurück zu Live“ wieder aufholen. Gilt für normale Live-Streams (nicht HLS).¦把当前频道暂存在设备上。你可以暂停而不错过内容，用快退键回跳，再用「返回直播」追上。仅适用于普通直播流（不支持 HLS）。
Puffergröße¦缓冲大小
Ungefähr: 512 MB reichen für 10–20 Minuten in HD. Der Puffer wird beim Schließen des Players gelöscht.¦大致参考：512 MB 约可存高清内容最近 10–20 分钟。关闭播放器时缓冲会被删除。
Speicherort für Aufnahmen¦录制内容的存储位置
Nachlauf nach Sendungsende¦节目结束后额外录制时长
Aufnehmen: im Player bei einem Sender auf den roten Knopf. Die laufende Sendung wird bis zu ihrem Ende laut TV-Guide aufgenommen, beginnt sie schon im Puffer, auch von ihrem Anfang an. Schaust du währenddessen einen anderen Sender, braucht das eine zweite Verbindung zu deinem Anbieter. Aufnahmen liegen im App-Ordner und werden beim Deinstallieren der App gelöscht.¦录制方法：播放器中在频道上按红色按钮。当前节目会按节目单录到结束，如果开头还在缓冲内则从开头录起。期间观看其他频道需要服务商提供第二个连接。录制内容保存在应用目录，卸载应用时会被删除。
Noch keine Aufnahmen.¦还没有录制内容。
Stoppen¦停止
Abspielen: Live TV > „Aufnahmen“.¦播放方法：直播电视 > 「录制内容」。
Interner Speicher¦内部存储
USB / Speicherkarte {#0}¦USB / 存储卡 {#0}
frei¦可用
Verbindung beendet¦连接已结束
Puffer beendet¦缓冲已结束
Puffer-Adresse ungültig¦缓冲地址无效
Mit anzeigen¦同时显示
Nicht mehr mit anzeigen¦停止显示
wird mit angezeigt¦同时显示中
„Mit anzeigen“ zeigt eine Playlist zusätzlich zur aktiven: Filme und Serien erscheinen zusammen (doppelte Filme nur einmal, gleiche Serien als eine Serie mit mehreren Quellen). In Filme und Serien filterst du über „Quelle“, im Live TV wechselst du die Quelle mit dem Knopf oben. Jede weitere Playlist braucht zusätzlich Speicher.¦「同时显示」会把一个播放列表叠加到当前列表上：电影和剧集一起出现（重复电影只显示一次，同一剧集合并为一个、含多个来源）。在「电影」和「剧集」中可按「来源」筛选，在「直播电视」中用顶部按钮切换来源。每增加一个播放列表都会占用更多内存。
Weitere Playlist wird geladen: {0}¦正在加载另一个播放列表：{0}
Zu wenig Speicher für eine weitere Playlist auf diesem Gerät.¦本设备内存不足，无法再加载一个播放列表。
Kartengröße, Seitenleiste, Raster¦卡片大小、侧栏、网格
Karten¦卡片
Beim Markieren einer Auswahl zeigt die Vorschau sofort die Wirkung, OK übernimmt sie. Schriftgröße und Farben findest du unter „Design“.¦高亮某个选项时预览会立即显示效果，按确认键应用。文字大小和颜色在「外观」中设置。
Seitenleiste¦侧栏
Position¦位置
Links¦左侧
Rechts¦右侧
Oben¦顶部
Immer mit Text¦始终显示文字
Aus: nur Symbole, die Texte erscheinen beim Hineingehen. An: Symbole und Texte sind immer zu sehen (braucht mehr Platz).¦关：只显示图标，进入时才显示标签。开：图标和标签始终可见（占用更多空间）。
Zurück bringt dich jederzeit in die Seitenleiste – auch wenn sie rechts oder oben steht.¦返回键总是先回到侧栏 —— 即使侧栏在右侧或顶部。
Sport¦体育
Jetzt live¦正在直播
Gleich¦即将开始
Sportsender¦体育频道
Keine Sportsender gefunden¦未找到体育频道
Sportsender werden an Kategorie und Namen erkannt (z. B. „Sport“, „DAZN“, „Eurosport“). Sport-Sendungen erscheinen, sobald der TV-Guide geladen ist.¦体育频道按分类和名称识别（例如「Sport」、「DAZN」、「Eurosport」）。节目单加载完成后才会显示体育节目。
TV-Guide wird noch geladen¦节目单仍在加载
Stalker-Portal¦Stalker 门户
Portal-Adresse (z. B. http://portal.tld:8080/c/)¦门户地址（例如 http://portal.tld:8080/c/）
MAC-Adresse (z. B. 00:1A:79:12:34:56)¦MAC 地址（例如 00:1A:79:12:34:56）
Sender und Filme werden geladen (Serien noch nicht). Die MAC-Adresse bekommst du von deinem Anbieter.¦会加载频道和电影（暂不含剧集）。MAC 地址由你的服务商提供。
Angaben unvollständig: Portal-Adresse und MAC-Adresse (z. B. 00:1A:79:12:34:56) nötig.¦信息不完整：需要门户地址和 MAC 地址（例如 00:1A:79:12:34:56）。
Das Portal antwortet nicht. Adresse prüfen.¦门户无响应，请检查地址。
Das Portal lehnt diese MAC-Adresse ab. MAC und Abo beim Anbieter prüfen.¦门户拒绝此 MAC 地址，请向服务商核对 MAC 和订阅状态。
Das Portal hat keine Sender geliefert. MAC-Adresse und Abo beim Anbieter prüfen.¦门户未返回频道，请向服务商核对 MAC 地址和订阅状态。
Filme werden geladen ({#0}) …¦正在加载电影（{#0}）…
Datei vom Gerät¦设备上的文件
Auf diesem Gerät gibt es keine Dateiauswahl. Lade die Datei über die Handy-Seite rechts hoch.¦此设备没有文件选择器，请用右侧的手机页面上传文件。
Das ist keine gültige M3U-Datei.¦这不是有效的 M3U 文件。
Die Playlist-Datei ist nicht mehr da. Bitte erneut hochladen.¦播放列表文件已丢失，请重新上传。
Playlist wird gelesen …¦正在读取播放列表 …
Zugangsdaten eintragen oder eine M3U-Datei hochladen und senden. Die Playlist wird danach automatisch geladen.¦输入登录信息或上传 M3U 文件并发送，播放列表随后自动加载。
Auf ein anderes Gerät übertragen¦转移到另一台设备
Verlauf, Weiterschauen, Favoriten, Listen und Einstellungen mitnehmen – z. B. vom Fire TV auf das Handy. Auf Wunsch auch die Playlists mit Zugangsdaten.¦把历史、继续观看、收藏、列表和设置带走 —— 例如从 Fire TV 到手机。也可选择连同播放列表和登录信息。
Übertragung im WLAN starten¦通过 Wi-Fi 开始转移
Übertragung beenden¦停止转移
Das Gerät ist nicht mit einem WLAN verbunden.¦此设备未连接 Wi-Fi。
Am Handy (im selben WLAN) öffnen:¦在手机上打开（同一 Wi-Fi）：
Altes Gerät: Sicherung herunterladen. Neues Gerät: hier ebenfalls starten und die Datei einspielen. Danach startet die App neu.¦旧设备：下载备份。新设备：同样在此处启动并上传该文件。之后应用会重启。
Google Drive¦Google Drive
Mit Google verbinden¦用 Google 账号连接
Am Handy öffnen und den Code eingeben:¦在手机上打开并输入验证码：
Die App speichert nur in ihrem eigenen, versteckten Ordner in deinem Google Drive und sieht sonst nichts.¦应用只在你的 Google Drive 中自己的隐藏文件夹里存取，看不到其他内容。
Nicht verbunden (abgelehnt oder Zeit abgelaufen).¦未连接（已拒绝或超时）。
Verbunden.¦已连接。
Letzte Sicherung: {0}¦上次备份：{0}
Playlists mit Zugangsdaten mitsichern¦包含带登录信息的播放列表
Praktisch für ein neues Gerät. Die Zugangsdaten liegen dann in deinem Google Drive.¦换新设备时很方便，此后你的登录信息会保存在自己的 Google Drive 中。
Jetzt in Drive sichern¦立即备份到云端
Aus Drive wiederherstellen¦从云端恢复
Trennen¦断开连接
Sichern …¦正在备份 …
Gesichert.¦备份完成。
Laden …¦加载中 …
In Drive gibt es noch keine Sicherung.¦云端还没有备份。
Die Sicherung in Drive ist ungültig.¦云端备份无效。
Google antwortet nicht¦Google 无响应
Nicht mit Google verbunden¦未连接 Google
Auf dem Fernseher lässt sich die Zwischenablage oft nicht nutzen: Unter Einstellungen > Speicher > „Übertragung im WLAN starten“ kannst du den Bericht am Handy öffnen (Seite „Letzten Absturzbericht anzeigen“) und dort kopieren.¦电视上常常无法使用剪贴板：在「设置 > 存储 > 通过 Wi-Fi 开始转移」中，可用手机打开报告（「显示上次崩溃报告」页面）并在那里复制。
Auf dem Handy im Querformat¦手机上强制横屏
Die App ist für den Fernseher gebaut und wirkt quer am besten. Aus: dreht mit dem Gerät. Im Player: Tippen zeigt die Steuerung, Doppeltippen links/rechts spult, in der Mitte Pause.¦应用为电视设计，横屏效果最佳。关：随设备旋转。播放器中：轻点显示控制栏，双击左/右快进，双击中间暂停。
Oder mit der Handy-Kamera scannen¦或用手机相机扫码
QR-Code scannen¦扫描二维码
Unterstütze IPVIO Stream¦支持 IPVIO Stream
Raster & Ansicht¦网格与视图
American Football¦美式橄榄球
Basketball¦篮球
Eishockey¦冰球
Fußball¦足球
Tennis¦网球
Motorsport¦赛车
Kampfsport¦搏击
Golf¦高尔夫
Handball & Volleyball¦手球与排球
Rugby & Cricket¦橄榄球与板球
Radsport¦自行车
Wintersport¦冬季运动
Darts & Snooker¦飞镖与斯诺克
Nächste 3 Std.¦未来 3 小时
Morgen¦明天
Keine Sendungen für diese Auswahl¦此筛选下没有节目
Zusatzbild¦附加画面
Anordnung¦布局
Bild-in-Bild¦画中画
Geteilt¦分屏
Ton von¦声音来自
Hauptstream¦主画面
Streams tauschen¦交换画面
Dieser Sender ist beim Anbieter gerade nicht erreichbar. Probiere einen anderen.¦服务商当前无法访问此频道，请换一个。
Dein Anbieter erlaubt vermutlich nur eine Verbindung gleichzeitig. Frage nach einem Zugang mit mehr Verbindungen.¦你的服务商可能只允许一个同时连接，请申请支持更多连接的账号。
Die Verbindung ist zu langsam oder wurde abgelehnt. Ein Sender in niedrigerer Qualität hilft oft.¦连接太慢或被拒绝，换一个较低画质的频道通常有效。
Dein Gerät schafft zwei Videos gleichzeitig nicht. Wähle für beide Bilder SD-Sender.¦本设备无法同时处理两路视频，请为两个画面都选标清频道。
Probiere einen anderen Sender oder eine niedrigere Qualität.¦请换一个频道或降低画质。
Schon ein anderes Gerät mit IPVIO Stream? Spiele die Sicherung ein: Playlists, Zugangsdaten, Verlauf, Favoriten und Einstellungen sind danach sofort da.¦已有另一台设备装了 IPVIO Stream？恢复备份即可立刻拥有播放列表、登录信息、历史、收藏和设置。
Sicherung von anderem Gerät einspielen¦从另一台设备恢复备份
Auf diesem Gerät gibt es keine Dateiauswahl. Spiele die Sicherung über die Handy-Seite rechts ein.¦此设备没有文件选择器，请用右侧的手机页面恢复备份。
Sicherung mit Playlists speichern …¦保存含播放列表的备份 …
Auf diesem Gerät gibt es keine Dateiauswahl. Nutze die Übertragung im WLAN.¦此设备没有文件选择器，请使用 Wi-Fi 转移。
Gesichert werden Einstellungen, Favoriten, Verlauf, Weiterschauen und Profile. Playlists und Zugangsdaten sind nur in „Sicherung mit Playlists“ enthalten (die Datei enthält Passwörter, gib sie nicht weiter). Beim Wiederherstellen startet die App neu. Auf einem neuen Gerät geht das gleich beim Hinzufügen der ersten Playlist.¦会备份设置、收藏、历史、继续观看和用户；播放列表和登录信息仅在「保存含播放列表的备份」中（该文件含密码，请勿分享）。恢复时应用会重启。新设备可在首次添加播放列表时直接完成。
Wichtig zu wissen¦使用前须知
IPVIO Stream ist nur ein Abspielgerät. Die App liefert selbst keine Sender, Filme, Serien und keine Playlist. Den Link oder die Zugangsdaten bekommst du von deinem IPTV-Anbieter oder aus deiner eigenen Quelle. Wir können dir keinen Link nennen.¦IPVIO Stream 只是播放器，应用本身不提供任何频道、电影、剧集或播放列表。链接或登录信息需由你的 IPTV 服务商或自有来源提供，我们无法提供链接。
OK = groß¦确认键 = 放大
Handy und dieses Gerät müssen im selben WLAN sein. Halte die Kamera auf das Bild. Es klappt auch aus einiger Entfernung.¦手机和此设备必须连接同一 Wi-Fi，把摄像头对准画面即可，保持一定距离也能识别。
Oder Adresse im Browser eintippen:¦或在浏览器中输入地址：
Zurück oder OK schließt das Bild.¦按返回键或确认键关闭图片。
Leiste nach unten¦栏移到底部
Leiste nach oben¦栏移到顶部
Texte¦标签
Immer zeigen¦始终显示
Nur Symbole¦仅图标
Beim Markieren¦高亮时显示
Bild¦画面
Kartenbreite¦卡片宽度
Spalten und Zeilen gelten nur in der Ansicht „Raster“.¦列数和行数仅适用于「网格」视图。
Zeigt die echte App mit deinen Titeln; die Einstellungen erscheinen in einer schmalen Leiste unten. Ein Klick auf eine Auswahl wirkt sofort.¦显示带你真实节目的实际应用界面；设置以细栏形式出现在底部，点选即立刻生效。
Zwei gleich groß¦两个等大画面
Einer groß, einer klein¦一大一小
So sieht eine Karte aus: normal und markiert¦卡片正常与高亮时的样式
Score-Overlay¦比分浮层
Anzeige¦显示方式
Untereinander¦上下堆叠
Nebeneinander¦左右并排
In den Ecken verteilt¦分布到四角
Ausgewählte Spiele entfernen¦移除已选比赛
Zusatzbild nicht abspielbar¦附加画面无法播放
Alle Playlists¦所有播放列表
Fenstergröße¦窗口大小
Gilt für Bild-in-Bild. Deine Auswahl wird gespeichert.¦适用于画中画，你的选择会被保存。
Handy-Suche¦手机搜索
Der Server konnte nicht gestartet werden.¦服务器无法启动。
Suche und starte Filme, Serien und Sender am Handy. Handy und Fernseher müssen im selben WLAN sein.¦在手机上搜索并启动电影、剧集和频道。手机与电视必须连接同一 Wi-Fi。
Filme, Serien, Sender suchen …¦搜索电影、剧集、频道 …
Pause / Weiter¦暂停 / 播放
Leiser¦音量 -
Lauter¦音量 +
Code vom Fernseher:¦电视上的验证码：
Tippe einen Titel ein.¦输入片名。
Läuft auf dem Fernseher.¦正在电视上播放。
Fehler. Ist der Fernseher noch an?¦出错了，电视还开着吗？
Es läuft nichts.¦当前没有播放内容。
Die Playlist wird noch geladen.¦播放列表仍在加载。
Nichts gefunden.¦未找到内容。
Folge¦单集
Lädt …¦加载中 …
Stopp¦停止
Beenden¦结束
Fertige Designs¦预设外观
Ein Klick stellt Farbe und Hintergrund passend ein. Darunter kannst du alles weiter von Hand ändern.¦一键设置配套的颜色和背景，下方仍可逐项手动调整。
Bildschirm anpassen¦调整屏幕
Uhr anzeigen¦显示时钟
Zeigt die Uhrzeit oben neben dem Profil-Symbol.¦在顶部用户图标旁显示时间。
Rand links und rechts¦左右边距
Rand oben und unten¦上下边距
Wenn am Fernseher Menü oder Texte am Rand abgeschnitten werden, stelle hier einen Rand ein. Der Hintergrund bleibt vollflächig, der Player nutzt immer den ganzen Bildschirm.¦如果菜单或文字被电视边缘裁切，可在此设置边距。背景仍铺满全屏，播放器始终使用整个屏幕。
Leer¦间距
Alles¦清除
Beendet¦完成
Auf¦开
Nicht in deinen Sendern¦不在你的频道中
Kein Sender in deiner Liste gefunden¦列表中未找到频道
Live-Ergebnisse: An¦实时比分：开
Live-Ergebnisse: Aus¦实时比分：关
Jetzt live auf deinen Sendern¦你的频道正在直播
Ergebnisse werden geladen …¦正在加载比分 …
Ergebnisse gerade nicht erreichbar – es werden nur Playlist und TV-Guide gezeigt.¦比分暂时无法获取 —— 只显示播放列表和节目单。
Dokumentationen¦纪录片
Zuletzt gesehen¦最近观看
noch¦剩余
▲▼ Sender  ·  OK Steuerung  ·  OK halten: mehr¦▲▼ 换台 · 确认键控制 · 长按确认键查看更多
Meine Teams¦我的球队
Teams merken¦保存球队
OK auf einem Team merkt es oder entfernt es wieder. Es erscheinen die Teams der Spiele des gewählten Tages.¦在球队上按确认键可保存或移除。显示所选日期比赛涉及的球队。
Hinweise: An¦提醒：开
Hinweise: Aus¦提醒：关
Verzögerung¦延迟
Dein Team spielt gleich¦你的球队即将比赛
Dein Team spielt jetzt¦你的球队正在比赛
Punkte!¦进球！
Mini-Spielstand¦迷你比分牌
Ein kleines Fenster in der Ecke zeigt den Stand, solange eines deiner Teams spielt – auch beim Film oder auf einem anderen Sender.¦你关注的球队比赛时，角落的小窗口会显示比分 —— 即使正在看电影或其他频道。
Fenster: An¦窗口：开
Fenster: Aus¦窗口：关
Logos und Namen¦队徽与名称
Nur Logos¦仅队徽
Spielzeit: An¦比赛时间：开
Spielzeit: Aus¦比赛时间：关
Sender korrigieren¦校正频道
Zurücksetzen (automatisch)¦重置（自动）
Kein Sender gefunden. Lange drücken zum Korrigieren.¦未找到频道，长按可校正。
Tabelle¦积分榜
Keine Tabelle verfügbar.¦暂无积分榜。
Ausgeblendete¦已隐藏
Alle wieder einblenden¦全部重新显示
Kategorie ausblenden¦隐藏分类
Gruppen ignorieren¦忽略分组
Alle wieder einschalten¦全部重新开启
Ignoriert¦已忽略
Titel und Sender sind nach der Kennung deines Anbieters gebündelt (z. B. „TR |“, „[EN]“, „AR:“). Schalte Bündel aus, die du nicht brauchst: Sie verschwinden sofort überall, ohne die Playlist neu zu laden. Ausgeschaltet heißt ignoriert. Hilft dort, wo der Sprachfilter eine Kennung nicht erkennt.¦节目和频道按服务商标签分组（例如 "TR |"、"[EN]"、"AR:"）。关闭不需要的分组：它们会立即在所有位置消失，无需重新加载播放列表。关即忽略。当语言过滤无法识别某个标签时很有用。
Kalender¦日历
Ligen¦联赛
Abgewählte Ligen erscheinen nicht und werden nicht abgefragt.¦未勾选的联赛不会显示，也不会查询。
Live-Ergebnisse sind aus. Schalte sie oben ein, dann erscheint der Kalender.¦实时比分已关闭，在上方开启后日历即会出现。
Du hast noch keine Teams gemerkt. Unter „Meine Teams“ kannst du Teams merken.¦你还没有保存任何球队，可在「我的球队」中保存。
Kalender wird geladen …¦正在加载日历 …
In den nächsten 7 Tagen spielt keines deiner Teams.¦未来 7 天没有你关注的球队比赛。
Spiele deiner Teams in den nächsten 7 Tagen¦未来 7 天你关注球队的比赛
Sicherung speichern unter …¦备份另存为 …
Sicherung aus Datei laden …¦从文件加载备份 …
Sicherung gespeichert.¦备份已保存。
Auf diesem Gerät gibt es keine Dateiauswahl. Nutze „Sicherung erstellen“.¦此设备没有文件选择器，请使用「创建备份」。
Auf diesem Gerät gibt es keine Dateiauswahl.¦此设备没有文件选择器。
„Speichern unter“ lässt dich den Ort wählen, zum Beispiel Google Drive, Downloads oder einen USB-Stick – so nimmst du die Sicherung auf ein anderes Gerät mit.¦「另存为」可让你选择保存位置，例如 Google Drive、下载目录或 U 盘 —— 方便把备份带到另一台设备。
Rot¦红色
Blau¦蓝色
Dünn¦细
Dick¦粗
Extra dick¦特粗
Was möchtest du sehen?¦想看什么？
Die Inhalte werden geladen …¦正在加载内容 …
Hier gibt es noch nichts zu sehen. Bitte lass den Bildschirm einrichten.¦这里暂时没有可看的内容，请让他人帮忙设置此页面。
Zum Beenden weiter halten …¦继续长按以退出 …
Boomer Mode beenden¦退出长辈模式
Gib die PIN ein.¦请输入 PIN 码。
Dieser Sender ist gerade nicht verfügbar¦此频道当前不可用
Du kommst gleich wieder zur Auswahl.¦稍后将返回选择界面。
So sieht es aus¦这就是显示效果
Der Boomer Mode ist mit deiner Kindersicherungs-PIN geschützt.¦长辈模式受家长控制 PIN 码保护。
Für den Boomer Mode brauchst du eine 4-stellige PIN. Sie schützt auch die Kindersicherung.¦长辈模式需要 4 位数字 PIN 码，它同时保护家长控制。
Eine ganz einfache Oberfläche für Eltern und Großeltern: große Kacheln mit Bild, kein Menü, keine Suche, keine Einstellungen. Du stellst hier alles ein, die Person davor muss nur noch auswählen.¦为父母长辈准备的极简界面：带图片的大磁贴，没有菜单、搜索和设置。你在这里全部配好，电视前的人只需选择即可。
Boomer Mode jetzt starten¦立即启动长辈模式
Die App wechselt sofort zur einfachen Oberfläche. Stelle vorher alles ein. Zurück zur normalen App: Zurück-Taste 10 Sekunden halten, dann PIN.¦应用会立刻切换到简易界面，请先完成设置。返回普通应用：长按返回键 10 秒，然后输入 PIN 码。
Automatischer Start nach dem Einschalten: bereit¦开机自动启动：已就绪
Automatischer Start: Berechtigung „Über anderen Apps einblenden“ fehlt¦自动启动：缺少「显示在其他应用上层」权限
Im Boomer Mode öffnet sich IPVIO Stream von selbst, sobald der Fernseher oder die Box eingeschaltet wird. Manche Geräte (z. B. einzelne Fire-TV-Modelle) blockieren das trotzdem.¦在长辈模式下，电视或盒子一开机就会自动打开 IPVIO Stream。部分设备（如某些 Fire TV 型号）仍会阻止此行为。
Was der Bildschirm zeigt¦屏幕上显示什么
Schalte Reihen ein oder aus und schiebe sie mit „Hoch“ und „Runter“. Reihen ohne Inhalt bleiben automatisch versteckt.¦开关各行，并用「上」「下」移动位置；没有内容的行会自动隐藏。
Sender auswählen¦选择频道
Wähle aus deiner ganzen Senderliste mit Logos die Sender, die in der Reihe „Meine Sender“ erscheinen. Ohne Auswahl zeigt der Modus deine Live-Favoriten.¦从全部频道列表中挑选带台标的频道，它们会出现在「我的频道」一行。不做选择时，该模式显示你的直播收藏。
Sender auswählen …¦选择频道 …
Auswahl löschen¦清除选择
Filme und Serien auswählen¦选择电影和剧集
Wähle Filme und Serien aus, die in der Reihe „Meine Filme und Serien“ erscheinen. Ein Film startet sofort, bei einer Serie öffnet sich die Folgenliste.¦为「我的电影和剧集」一行挑选内容。电影会直接播放，剧集则打开分集列表。
Filme und Serien auswählen …¦选择电影和剧集 …
Ausgewählt¦已选择
Aussehen¦外观
Größe der Kacheln¦磁贴大小
Größe der Schrift¦文字大小
Markierung: Farbe des Rahmens¦高亮：边框颜色
Markierung: Dicke des Rahmens¦高亮：边框粗细
Zusätzliche helle Innenlinie¦额外的细内描边
Macht die Markierung noch deutlicher, auch bei Farbschwäche.¦让高亮更清晰，对色觉障碍也更友好。
Die markierte Kachel wird leicht größer. Ausschalten = alles bleibt ruhig.¦高亮的磁贴会略微放大。关 = 保持画面平稳。
Logos und Bilder zeigen¦显示台标和图片
Aus = nur der Name in großer Schrift.¦关 = 只用大字显示名称。
So kommst du wieder heraus¦如何退出
Zurück-Taste 10 Sekunden gedrückt halten, dann die PIN eingeben. Am Handy: die Überschrift „Was möchtest du sehen?“ lange berühren.¦长按返回键 10 秒，然后输入 PIN 码。手机上：长按标题「想看什么？」。
Gerade automatisch: deine Live-Favoriten. Sobald du eine Kachel unten mit OK wählst oder Sender auswählst, stellst du die Reihe selbst zusammen.¦当前为自动：显示你的直播收藏。只要在下方用确认键挑选磁贴或选择频道，就会由你自己构建这一行。
Du hast die Sender selbst zusammengestellt. OK auf einer Kachel: nach links oder rechts schieben, oder entfernen.¦频道由你自己组合。在磁贴上按确认键：左右移动或移除。
Aus Favoriten übernehmen¦从收藏导入
Zuletzt gesehene dazu¦添加最近观看
Aus Senderliste …¦从频道列表 …
Noch keine Sender. Wähle oben Sender aus oder markiere sie in Live TV mit dem Stern.¦还没有频道。请在上方选择频道，或在直播电视里用星标标记。
Nach links schieben¦左移
Nach rechts schieben¦右移
OK wählt aus oder nimmt wieder heraus¦确认键选择或移除
Ausgewählte¦已选条目
Noch nichts ausgewählt.¦尚未选择任何内容。
Hier gibt es nichts.¦这里没有内容。
Große Slideshow¦大图轮播
Großes Bild oben mit aktuellen Filmen und Serien¦顶部大图，展示当前电影和剧集
Live TV, Filme, Serien, Favoriten und Meine Liste¦直播电视、电影、剧集、收藏和我的列表
Uhr und Datum¦时钟与日期
Große Uhr mit dem heutigen Datum¦带今天日期的大时钟
Dort weitermachen, wo du aufgehört hast¦从上次中断处继续
Meine Sender¦我的频道
Deine Live-Favoriten als Reihe¦把你的直播收藏排成一行
Live TV gerade¦正在直播
Was jetzt auf deinen Sendern läuft¦你的频道此刻正在播什么
Die beliebtesten Titel des Tages¦今日最热门节目
Weil du etwas geschaut hast¦因为你看过一些内容
Ähnliche Titel zu deinem letzten Film oder deiner letzten Serie¦与你最近看的电影或剧集相似的内容
Titel, die du dir gemerkt hast¦你收藏的节目
Alles, was du zuletzt gestartet hast¦最近开始看的内容
Neue Filme¦新电影
Zuletzt hinzugefügte Filme¦最近添加的电影
Beste Filme¦精选电影
Die am besten bewerteten Filme¦评分最高的电影
Neue Serien¦新剧集
Zuletzt hinzugefügte Serien¦最近添加的剧集
Beste Serien¦精选剧集
Die am besten bewerteten Serien¦评分最高的剧集
Serien nach Genre¦按类型看剧集
Reihen für die häufigsten Serien-Genres¦常见剧集类型各成一行
Filme nach Genre¦按类型看电影
Reihen für die häufigsten Film-Genres¦常见电影类型各成一行
Große Uhr¦大时钟
Uhrzeit und Datum, gut lesbar¦时间与日期，清晰易读
Die Sender, die du ausgewählt hast (sonst die Live-Favoriten)¦你选择的频道（否则显示直播收藏）
Filme und Serien dort fortsetzen, wo es aufgehört hat¦从中断处继续播放电影和剧集
Meine Filme und Serien¦我的电影和剧集
Die Filme und Serien, die du ausgewählt hast¦你选择的电影和剧集
Titel aus der Merkliste¦待看清单中的节目
Was zuletzt gestartet wurde¦最近开始播放的内容
Gezeigt¦显示
Versteckt¦隐藏
Hoch¦上移
Runter¦下移
Auf Standard zurücksetzen¦恢复默认
Baue dir deine Startseite: Schalte Reihen und Widgets ein oder aus und schiebe sie mit „Hoch“ und „Runter“ an die Stelle, die du möchtest. Die Änderung gilt sofort, schau danach auf der Startseite nach.¦定制你的首页：开关各行和小组件，用「上」「下」把它们移到想要的位置。改动立即生效，完成后请到首页查看。
Startseite anpassen¦定制首页
Reihen und Widgets wählen und ordnen¦选择并排列各行与小组件
Ganz einfach: große Kacheln zum Auswählen¦极简：大磁贴，选一下就行
Spende¦捐赠
Hallo, ich bin der nigerianische Prinz und brauche dringend deine Hilfe! Spaß beiseite: Die App kostet dich nichts, mich aber ein paar Nächte Schlaf und sehr viel Kaffee. Wenn du mir ein paar Euro schicken möchtest, tanzt mein Kontostand vor Freude. Kein Zwang: Die App bleibt kostenlos, werbefrei und GPL-lizenziert.¦你好，我是尼日利亚王子，急需你的帮助！开个玩笑：应用对你免费，但它花了我好几个不眠之夜和很多很多咖啡。如果你愿意赞助几欧元，我的银行账户会高兴得跳舞。别有压力：应用会一直免费、无广告，并保持 GPL 许可。
Mit der Handy-Kamera scannen¦用手机相机扫码
Folgen werden geladen …¦正在加载分集 …
Die Folgen konnten nicht geladen werden. Verbindung oder Zugang prüfen.¦分集加载失败，请检查网络或账号。
Dieser QR-Code ist der PayPal-Link für meine Spende. Mit der Handy-Kamera scannen, dann öffnet sich PayPal.¦这个二维码是我的 PayPal 捐赠链接，用手机相机扫描即可打开 PayPal。
Übersicht¦概览
Spielplan¦赛程
Teams¦球队
Top-Spiel¦焦点战
Demnächst¦即将开始
Beliebte Ligen¦热门联赛
Sportarten¦运动项目
Verschoben¦延期
Abgesagt¦取消
Statistik¦数据统计
Aufstellung¦首发阵容
Ereignisse¦事件
Kommentar¦文字直播
Bank¦替补
Tore¦进球
Zuschauer¦观众人数
Sender starten¦起始频道
▶ Auf deinen Sendern¦▶ 在你的频道中
Lade Spiele …¦正在加载比赛 …
Heute keine Spiele gefunden.¦今天没有比赛。
Keine Spiele für diese Auswahl.¦此筛选下没有比赛。
Dieser Bereich folgt im nächsten Schritt.¦此版块将在下一步推出。
Lade Details …¦正在加载详情 …
Keine Details verfügbar.¦暂无详情。
Keine Liga aktiv.¦当前未选择联赛
Lade Tabelle …¦正在加载积分榜 …
Für diese Liga gibt es keine Tabelle.¦此联赛没有积分榜。
Lade Teams …¦正在加载球队 …
Keine Teams gefunden.¦未找到球队。
Nächste Spiele¦接下来的比赛
Letzte Spiele¦近期比赛
Nächstes Spiel¦下一场
Letztes Spiel¦上一场
Kader¦阵容
Trainer¦教练
Spiele¦比赛
Team merken¦保存球队
Nicht mehr merken¦取消保存
Meine Ligen¦我的联赛
Meine Sportarten¦我的运动项目
Heute für dich¦今日为你推荐
Heute keine Spiele deiner Favoriten.¦今天没有你关注球队的比赛。
Noch keine Teams gemerkt. Im Team Center gibt es „Team merken“.¦还没有保存球队，可在「球队中心」使用「保存球队」。
Design und Farbe¦外观与配色
Berechtigung erteilt. Der Autostart ist bereit.¦已授予权限，自启动已就绪。
Berechtigung fehlt noch. Bitte „Einstellung öffnen“ wählen und IPVIO Stream erlauben.¦权限仍未授予，请选择「打开设置」并允许 IPVIO Stream。
Hauptliste¦主列表
zusätzlich angezeigt¦同时显示
Nicht mehr zusätzlich anzeigen¦停止同时显示
Zusätzlich anzeigen¦同时显示
Zur Hauptliste machen¦设为主列表
Die Hauptliste ist die Playlist, mit der die App arbeitet. „Zusätzlich anzeigen“ blendet eine weitere Playlist dazu ein; sie ersetzt die Hauptliste nicht.¦主列表是应用当前使用的播放列表。「同时显示」会在其上叠加另一个播放列表，不会替换主列表。
Sender suchen¦搜索频道
Sendername eingeben¦输入频道名称
OK auf dem Feld öffnet die Tastatur. Gesucht wird im Sendernamen und in der Kategorie.¦在输入框上按确认键可打开键盘，搜索范围包括频道名称和分类。
Im TV-Guide suchen¦搜索节目单
Sender oder Sendung¦频道或节目
Handy¦手机
Zuletzt gesucht (OK lange gedrückt = Eintrag entfernen)¦最近搜索（长按确认键 = 删除条目）
Zuletzt gesucht¦最近搜索
Suche nach einem Titel: hier erscheinen Beschreibung, Bewertung und Format des markierten Treffers.¦搜索片名：高亮结果的简介、评分和格式会显示在这里。
SportsHub¦体育中心
Sports Hub öffnen¦打开体育中心
Sender-Qualitäten bündeln¦合并同名频道的画质版本
Derselbe Sender in 8K, 4K, FHD, HD und SD erscheint als eine Zeile. OK startet die beste Qualität, die anderen wählst du im Menü des Senders (OK gedrückt halten).¦同一频道的 8K、4K、FHD、HD、SD 版本合并为一行。按确认键播放最佳画质；其他画质在频道菜单（长按确认键）中选择。
Rand: „Schatten“ legt einen weichen dunklen Schatten unter die Schrift, „Kontur“ zeichnet einen schwarzen Rand rundherum (besser lesbar auf hellem Bild), „Kein“ lässt die Schrift pur.¦描边：「阴影」在文字下方加柔和暗影，「轮廓」在四周描黑边（亮色画面上更清晰），「无」则保持纯文字。
Bildschirmrand einstellen¦调整屏幕边界
Zeigt einen Rahmen mit vier Ecken am Fernseher: Stelle den Rand ein, bis alle vier Ecken sichtbar sind. Hilft, wenn dein Fernseher Menü oder Texte am Rand abschneidet.¦在电视上显示带四个角的边框：调整边距直到四个角都可见。适用于菜单或文字被电视边缘裁切的情况。
Du siehst einen weißen Rahmen mit vier grünen Punkten. Wenn eine Ecke fehlt, schneidet dein Fernseher den Rand ab. Rechts/Links ändert den Rand seitlich, Hoch/Runter oben und unten, bis alle vier Ecken sichtbar sind. OK speichert.¦你会看到一个白色边框和四个绿点。如果缺了某个角，说明电视裁切了边缘。右/左调整左右边距，上/下调整上下边距，直到四个角都可见，按确认键保存。
Seitlich¦左右
Oben/unten¦上下
Karten: So sehen einzelne Filme und Serien in den Listen aus (Poster oder breites Bild, Größe, Infos).¦卡片：单部电影和剧集在列表中的样式（海报或宽幅图、尺寸、信息）。
Seitenleiste: Das Menü am Rand mit Startseite, Filme, Serien und Live TV. Du kannst sie links, rechts oder oben anzeigen.¦侧栏：位于边缘、包含首页、电影、剧集和直播电视的菜单，可显示在左、右或顶部。
Raster & Ansicht: Wie viele Titel nebeneinander erscheinen und ob Details zum markierten Titel gezeigt werden.¦网格与视图：一行显示多少节目，以及是否显示选中节目的详情。
Startseite: Aussehen der ersten Seite nach dem Start. Welche Reihen erscheinen, stellst du über „Startseite anpassen“ ein.¦首页：启动后第一页的样子。显示哪些行由「定制首页」设置。
Kategorien: Die Ordner deines Anbieters oder automatische Genres und ihre Darstellung.¦分类：服务商的文件夹或自动识别的类型，以及它们的显示方式。
Wiedergabe¦播放
Intro und nächste Folge¦片头与下一集
Uhr im Bild¦屏上时钟
Netzwerk und Bildrate¦网络与帧率
Liste importiert¦列表已导入
Titel gelesen¦已读取节目
in deiner Playlist gefunden¦在你的播放列表中找到
Mehrfachansicht¦多画面
Vier gleich groß¦四个等大画面
Einer groß, drei klein¦一大三小
OK = Ton hierher, OK lange = Sender wechseln¦确认键 = 此画面出声，长按确认键 = 换台
So sieht die Reihenfolge aus¦这是最终的排列顺序
Nichts gezeigt¦未显示任何内容
Listen importieren¦导入列表
Letterboxd, MyAnimeList, Trakt¦Letterboxd、MyAnimeList、Trakt
Hole deine Listen von Letterboxd, MyAnimeList oder Trakt in die App. Die Titel werden mit deiner Playlist abgeglichen: Merkliste und Gesehen werden übernommen.¦把 Letterboxd、MyAnimeList 或 Trakt 的列表导入应用。节目会与你的播放列表匹配：待看和已看都会导入。
Letterboxd: ZIP oder CSV · MyAnimeList: XML · Trakt: JSON¦Letterboxd：ZIP 或 CSV · MyAnimeList：XML · Trakt：JSON
Dieses Gerät hat keinen sicheren Schlüsselspeicher: Playlist-Adressen mit Zugangsdaten werden unverschlüsselt gespeichert.¦此设备没有安全密钥存储：含登录信息的播放列表地址将未加密保存。
Datei vom Gerät wählen¦从设备选择文件
Auf diesem Gerät gibt es keine Dateiauswahl. Nutze den Import im WLAN.¦此设备没有文件选择器，请使用 Wi-Fi 导入。
Import im WLAN starten¦通过 Wi-Fi 开始导入
Import beenden¦停止导入
Am Handy „Liste importieren“ wählen, den Code eingeben und die Exportdatei hochladen.¦在手机上选择「导入列表」，输入验证码并上传导出文件。
Direkt auf der Startseite bearbeiten¦直接在首页编辑
Öffnet die echte Startseite mit deinen Titeln. Über jeder Reihe erscheint eine Leiste zum Verschieben und Ein- oder Ausblenden.¦打开带你真实节目的首页，每行上方都有工具条，可移动或显示/隐藏该行。
Oder hier in der Liste: Schalte Reihen und Widgets ein oder aus und schiebe sie mit „Hoch“ und „Runter“ an die Stelle, die du möchtest. Die Änderung gilt sofort.¦或在此列表中：开关各行与小组件，用「上」「下」把它们移到想要的位置，改动立即生效。
Immer oben¦始终置顶
Einblenden¦显示
Bearbeiten: Über jeder Reihe kannst du sie verschieben oder ausblenden.¦编辑中：每行上方可移动或隐藏该行。
Auf der echten Startseite ansehen¦在真实首页中查看
Live-Vorschau¦实时预览
Eine Auswahl wirkt sofort, ohne OK.¦选择即刻生效，无需按确认键。
Weiter nach unten wechselt den Bereich, die App dahinter springt mit.¦继续向下移动会切换区域，背后的应用也会跟随。
Raster: Wie viele Spalten und Zeilen die Rasteransicht (zum Beispiel „Alle Filme“) zeigt.¦网格：网格视图（例如「全部电影」）显示多少列、多少行。
Score-Overlay: An¦比分浮层：开
Score-Overlay: Aus¦比分浮层：关
Setzt Ansicht, Menü, Player, Ton und Start auf die Standardwerte. Playlists, Favoriten und Verlauf bleiben erhalten. Das Design hat einen eigenen Knopf.¦将视图、菜单、播放器、声音和启动项恢复默认。播放列表、收藏和历史会保留。外观有单独的按钮。
Live-Vorschau starten¦启动实时预览
Zeigt die echte App mit deinen Titeln; unten erscheinen die Einstellungen. Jede Auswahl wirkt sofort, ohne OK.¦显示带你真实节目的应用界面；设置显示在底部，每个选择即刻生效，无需按确认键。
Letterboxd · Gesehen¦Letterboxd · 已看
Letterboxd · Merkliste¦Letterboxd · 待看
Trakt · Gesehen¦Trakt · 已看
Trakt · Merkliste¦Trakt · 待看
MyAnimeList · Gesehen¦MyAnimeList · 已看
MyAnimeList · Merkliste¦MyAnimeList · 待看
1 Spiel¦1 场比赛
{#0} Spiele¦{#0} 场比赛
Spiel¦比赛
Formel 1¦一级方程式
1. Training¦第一次自由练习
2. Training¦第二次自由练习
3. Training¦第三次自由练习
Qualifying¦排位赛
Sprint-Qualifying¦冲刺排位赛
Sprint¦冲刺赛
Rennen¦正赛
Aktuelle Position¦当前位置
Alle Ligen¦全部联赛
Alle Wettbewerbe¦全部赛事
Als Favorit merken¦加入收藏
Anderen Sender wählen¦选择其他频道
Sender zuordnen¦指定频道
Automatisch zuordnen¦自动指定
Auf deinen Sendern¦在你的频道中
Live ansehen¦观看直播
Kein passender Sender gefunden¦未找到匹配的频道
ESPN liefert keine Streams. Gesucht wird in deinem TV-Guide und in deinen Sendernamen. Du kannst den Sender auch selbst zuordnen; die App merkt sich das für dieses Spiel.¦ESPN 不提供视频流。应用会搜索你的节目单和频道名称；你也可以自己指定频道，应用会为这场比赛记住它。
Von dir zugeordnet¦由你指定
Laut TV-Guide¦依据节目单
Sendername passt¦频道名称匹配
Verfügbar¦可用
Keine Sender in der Playlist.¦播放列表中没有频道。
Match Center¦比赛中心
Daten konnten nicht geladen werden¦数据加载失败
Die Daten konnten nicht geladen werden. Bitte versuche es erneut.¦数据加载失败，请重试。
ESPN antwortet gerade nicht. Bitte versuche es erneut.¦ESPN 当前无响应，请重试。
ESPN antwortet gerade nicht. Prüfe die Internetverbindung und versuche es erneut.¦ESPN 当前无响应，请检查网络后重试。
ESPN hat für dieses Team gerade keine Daten geliefert.¦ESPN 当前没有返回此球队的数据。
ESPN liefert für diesen Spieler keine Daten.¦ESPN 没有此球员的数据。
Keine Spielerdaten¦无球员数据
Ergebnisse von heute¦今日赛果
Ergebnisse¦赛果
Letzte Ergebnisse¦最新赛果
Keine Ergebnisse¦无结果
Noch keine Ergebnisse¦暂无赛果
Live-Ergebnisse sind aus¦实时比分已关闭
Ergebnisse, Tabellen und Spielpläne kommen von ESPN (inoffizielle Quelle). Schalte sie ein, um den Sports Hub zu nutzen.¦赛果、积分榜和赛程来自 ESPN（非官方来源）。开启后才能使用体育中心。
Einschalten¦开启
Gerade läuft nichts¦当前没有直播
Sobald ein Spiel deiner Ligen beginnt, erscheint es hier.¦你关注的联赛一开始比赛，就会显示在这里。
Ändere die Filter oder schau in den Spielplan.¦调整筛选条件或查看赛程。
Spielplan öffnen¦打开赛程
In deinen Ligen ist heute nichts angesetzt. Im Spielplan siehst du die nächsten Tage.¦今天你关注的联赛没有安排比赛，赛程中会显示接下来几天。
An diesem Tag ist in deinen Ligen nichts angesetzt. Tennis, Formel 1, UFC, Golf und Cricket zeigt ESPN nur für die laufenden Turniere und Wochenenden.¦这一天你关注的联赛没有安排比赛。ESPN 对网球、一级方程式、UFC、高尔夫和板球只提供当前赛事和周末的数据。
Hinweis: Die letzte Aktualisierung hat nicht geklappt. Angezeigt wird der letzte Stand.¦注意：上次更新失败，当前显示的是最后一次已知状态。
In den nächsten Tagen keine Spiele deiner Favoriten.¦未来几天没有你关注球队的比赛。
Kein Spiel geplant¦未安排比赛
Keine Spiele geplant¦没有安排比赛
Heute keine Spiele.¦今天没有比赛。
Gerade keine Ereignisse.¦当前没有赛事。
In diesem Zeitraum liefert ESPN keine Spiele.¦ESPN 没有此时间段的比赛。
Ligen schaltest du im Reiter „Sender“ unter „Ligen“ ein.¦联赛在「频道」标签页的「联赛」中开启。
Lade Daten …¦正在加载数据 …
Meine Favoriten¦我的收藏
Merke dir Teams, Wettbewerbe, Spieler oder Sportarten. Sie erscheinen hier mit ihren Spielen.¦保存球队、赛事、球员或运动项目，它们会连同比赛一起显示在这里。
Sport entdecken¦发现运动项目
Nur Favoriten¦仅看收藏
OK auf eine Sportart merkt sie oder entfernt sie wieder.¦在某个运动项目上按确认键可保存或取消保存。
Wichtige Ereignisse¦关键事件
Spielbericht¦比赛报道
Direktvergleich¦交锋记录
Form¦近期状态
Klassement¦积分榜
Kampfabend¦对阵卡
Spieler¦球员
Startelf¦首发
Ersatzbank¦替补
eingewechselt¦替补上场
ausgewechselt¦被换下
Satz¦盘
Sätze¦盘数
Spitze¦领先者
Torjäger¦射手榜
Topspieler¦最佳球员
Wettbewerbe¦赛事
Aktuell¦当前
Geboren¦出生
Gewicht¦体重
Nation¦国籍
Nummer¦号码
Team¦球队
Beginn: {0}¦开始：{0}
{0} führt {1} gegen {2}.¦{0} 以 {1} 领先 {2}。
{0} gewinnt {1} gegen {2}.¦{0} 以 {1} 战胜 {2}。
Es steht {0} zwischen {1} und {2}.¦{1} 与 {2} 战成 {0}。
{0} und {1} trennen sich {2}.¦{0} 与 {1} 以 {2} 战平。
Pkt¦积分
Sp¦场
S¦胜
U¦平
N¦负
TD¦净胜球
OTN¦加时负
Serie¦连胜/连败
J.¦岁
Weiter ohne Playlist¦不添加播放列表继续
MAC-Adresse¦MAC 地址
Seriennummer (optional)¦序列号（可选）
Device ID (optional)¦设备 ID（可选）
Device ID 2 (optional)¦设备 ID 2（可选）
Seriennummer und Device IDs nur eintragen, wenn dein Anbieter sie verlangt (wie bei TiviMate oder STB Emu). Leer = die App erzeugt eigene Werte.¦仅当服务商要求时才填写序列号和设备 ID（如 TiviMate 或 STB Emu）。留空 = 应用自行生成。
Update verfügbar¦有可用更新
Neue Version {0} (installiert: {1})¦新版本 {0}（已安装：{1}）
Deine Playlists und Einstellungen bleiben erhalten. Android fragt vor der Installation einmal nach.¦播放列表和设置会保留，安装前 Android 会询问一次。
Jetzt aktualisieren¦立即更新
Später¦稍后
Auf Updates prüfen¦检查更新
Suche nach Updates …¦正在检查更新 …
Du hast die neueste Version ({0}).¦已是最新版本（{0}）。
Update anzeigen¦显示更新
Keine Verbindung. Prüfe das Netzwerk und versuche es erneut.¦无网络连接，请检查网络后重试。
Dieser Hinweis erscheint nur einmal.¦此提示只显示一次。
Staffel gesehen¦本季已看完
Staffel zurücksetzen¦重置本季
Serie gesehen¦全剧已看完
Serie zurücksetzen¦重置全剧
Als gesehen markieren¦标记为已看
Als ungesehen markieren¦标记为未看
Aus „Zuletzt geschaut“ entfernen¦从「最近观看」中移除
OK lange drücken = als gesehen markieren¦长按确认键 = 标记为已看
OK lange drücken = als ungesehen markieren¦长按确认键 = 标记为未看
Tastatur des Geräts in der Suche¦搜索时使用设备键盘
Aus: die eingebaute Tastatur der App. An: die Tastatur deines Geräts (z. B. Gboard) mit allen Sprachen, Sprachfunktion und Einfügen. Mit OK auf das Suchfeld wird sie geöffnet.¦关：使用应用内置键盘。开：使用设备键盘（如 Gboard），支持多语言、语音输入和粘贴。在搜索框上按确认键即可打开。
Nach oben verschieben¦上移
Nach unten verschieben¦下移
An den Anfang verschieben¦移到最前
Kategorien sortieren …¦排序分类 …
Anbieter-Reihenfolge¦服务商顺序
Eigene Reihenfolge¦自定义顺序
Prüfsumme stimmt nicht. Die Datei ist beschädigt oder verändert. Versuche es erneut.¦校验和不匹配，文件已损坏或被改动，请重试。
Download unvollständig. Versuche es erneut.¦下载不完整，请重试。
Die Datei ist keine gültige App-Datei.¦文件不是有效的应用安装包。
Die Datei gehört zu einer anderen App.¦该文件属于另一个应用。
Die Datei ist nicht neuer als die installierte Version.¦该文件并不比已安装版本更新。
Die Versionsnummer passt nicht zur Update-Datei.¦版本号与更新文件不一致。
Die Signatur passt nicht zur installierten App. Android würde das Update ablehnen. Erstelle ein Backup, installiere die App neu und stelle das Backup wieder her.¦签名与已安装应用不匹配，Android 会拒绝更新。请先创建备份，重新安装应用后再恢复备份。
Die Signatur konnte nicht geprüft werden.¦无法校验签名。
Update-Datei: ungültige Prüfsumme¦更新文件：校验和无效
Update-Datei: Download-Adresse nicht sicher¦更新文件：下载地址不安全
Dieses Update ist erforderlich.¦此更新为强制更新。
Untertitel online suchen¦在线搜索字幕
OpenSubtitles API-Schlüssel¦OpenSubtitles API 密钥
SubDL API-Schlüssel¦SubDL API 密钥
TMDB, IMDb, Rotten Tomatoes, Untertitel¦TMDB、IMDb、烂番茄、字幕
Kein Schlüssel für die Untertitel-Suche. Einstellungen > API-Schlüssel¦没有字幕搜索所需的密钥，请到「设置 > API 密钥」。
Titel unbekannt¦片名未知
Suche fehlgeschlagen¦搜索失败
Download fehlgeschlagen¦下载失败
Tageslimit erreicht¦已达每日上限
Keine Untertitel gefunden¦未找到字幕
Untertitel konnte nicht geladen werden¦字幕加载失败
Untertitel geladen¦字幕已加载
Kostenlos unter opensubtitles.com: Konto anlegen, dann unter „API consumers“ eine App anlegen und den API-Schlüssel eintragen. Ohne Anmeldung sind nur wenige Downloads pro Tag möglich.¦在 opensubtitles.com 免费注册：创建账号后在「API consumers」下创建应用并填入其 API 密钥。不登录的话每天只能下载少量字幕。
Kostenlos unter subdl.com > Konto > API. Im Player unter „Audio & Untertitel“ > „Untertitel online suchen“ werden beide Dienste gefragt.¦在 subdl.com > 账户 > API 免费获取。播放器中「音频与字幕」>「在线搜索字幕」会同时查询这两个服务。
Diese Seite zurücksetzen¦重置此页面
Diese Seite wurde zurückgesetzt.¦此页面已重置。
Version, Quellen, Updates¦版本、来源、更新
Alles zurücksetzen¦全部重置
Alle Einstellungen auf Standard¦所有设置恢复默认
Alle Einstellungen zurücksetzen?¦重置所有设置？
Alle Einstellungen gehen auf die Standardwerte zurück. Playlists, Favoriten, Verlauf und Fortsetzen-Positionen bleiben erhalten. Das lässt sich nicht rückgängig machen.¦所有设置将恢复默认值。播放列表、收藏、历史和续播位置会保留，此操作无法撤销。
Alle Einstellungen wurden zurückgesetzt.¦所有设置已重置。
Kategorien ein- und ausblenden¦显示/隐藏分类
Blende einzelne Kategorien aus oder schalte alle aus und nur die gewünschten wieder ein. Ausgeblendete Kategorien verschwinden sofort überall, ohne die Playlist neu zu laden.¦可单独隐藏分类，也可先全部关闭、再只开启需要的分类。被隐藏的分类会立即在所有位置消失，无需重新加载播放列表。
„Zusätzlich anzeigen“ zeigt eine Playlist zusätzlich zur aktiven: Filme und Serien erscheinen zusammen (doppelte Filme nur einmal, gleiche Serien als eine Serie mit mehreren Quellen). In Filme und Serien filterst du über „Quelle“, im Live TV wechselst du die Quelle mit dem Knopf oben. Jede weitere Playlist braucht zusätzlich Speicher.¦「同时显示」会把一个播放列表叠加到当前列表上：电影和剧集一起出现（重复电影只显示一次，同一剧集合并为一个、含多个来源）。在「电影」和「剧集」中可按「来源」筛选，在「直播电视」中用顶部按钮切换来源。每增加一个播放列表都会多占内存。
Zugangsdaten werden nur auf diesem Gerät gespeichert und hier nicht angezeigt. Es ist immer eine Playlist aktiv. Mit „Zur Hauptliste machen“ wechselst du die Liste.¦登录信息仅保存在本设备，不在此显示。始终只有一个播放列表处于启用状态，用「设为主列表」切换。
Beim Markieren einer Auswahl zeigt die Vorschau sofort die Wirkung, OK übernimmt sie.¦选中某项时会立即预览效果，按确认键应用。
Tippe rechts mit der Tastatur. Gesucht wird im Sendernamen und in der Kategorie.¦在右侧键盘输入，将按频道名称和分类搜索。
Der Server des Anbieters lehnt die Verbindung ab (512). Meist ist die Server-Adresse nicht mehr aktuell oder gesperrt. Beim Anbieter nach der aktuellen Adresse fragen.¦服务商服务器拒绝连接（512）。通常是服务器地址已过期或被封，请向服务商索取最新地址。
Der Server des Anbieters lehnt die Verbindung ab (512). Meist ist die Server-Adresse nicht mehr aktuell oder gesperrt.¦服务商服务器拒绝连接（512）。通常是服务器地址已过期或被封。
Das Portal hat die Anmeldung abgelehnt. Adresse und MAC-Adresse prüfen.¦门户拒绝登录，请检查门户地址和 MAC 地址。
Erstelle ein Backup, installiere die App neu und stelle das Backup wieder her.¦请备份数据、重新安装应用，再恢复备份。
Boomer Mode¦长辈模式
Hello, I'm Claude. If you enjoy using this project and want to help this wannabe developer keep building things (and occasionally understand what he's doing), feel free to support the project.¦你好，我是 Claude。如果你喜欢这个项目，想帮这个半路出家的开发者继续把它做下去（顺便偶尔看懂他在干什么），欢迎支持这个项目。
IPVIO Stream is free, ad-free and open source (GPL-3.0). Donations are voluntary and do not unlock any features.¦IPVIO Stream 免费、无广告、开源（GPL-3.0）。捐赠完全自愿，不会解锁任何功能。
Resets all settings to their defaults: view, design, menu, player, sound, genres, Live TV and more. Playlists, favorites, history, resume positions and the parental-control PIN are kept. Clear the cache under Storage.¦将把视图、外观、菜单、播放器、声音、类型、直播电视等所有设置恢复默认。播放列表、收藏、历史、续播位置和家长控制 PIN 都会保留。如需清除缓存，请到「存储」。
Reset just one page? Every settings page has its own reset button at the bottom.¦只想重置某一页？每个设置页底部都有自己的重置按钮。
Live TV + Filme & Serien¦直播电视 + 电影和剧集
Nur Live TV¦仅直播电视
Nur Filme & Serien¦仅电影和剧集
There are no movies or series. If there should be some, check Settings > Playlists: the content mode of this playlist may be set to live TV only.¦没有电影或剧集。如果本该有，请检查「设置 > 播放列表」：这个播放列表的内容模式可能被设成了「仅直播电视」。
There are no movies. If there should be some, check Settings > Playlists: the content mode of this playlist may be set to live TV only.¦没有电影。如果本该有，请检查「设置 > 播放列表」：这个播放列表的内容模式可能被设成了「仅直播电视」。
There are no series. If there should be some, check Settings > Playlists: the content mode of this playlist may be set to live TV only.¦没有剧集。如果本该有，请检查「设置 > 播放列表」：这个播放列表的内容模式可能被设成了「仅直播电视」。
There are no live channels. If there should be some, check Settings > Playlists: the content mode of this playlist may be set to movies & series only.¦没有直播频道。如果本该有，请检查「设置 > 播放列表」：这个播放列表的内容模式可能被设成了「仅电影和剧集」。
Alle paar Stunden¦每隔几小时
4K does not suit this device – playing FHD¦这台设备不适合 4K，正以 FHD 播放
All movies¦全部电影
Datei konnte nicht umbenannt werden¦文件重命名失败
Das Zertifikat des Anbieters passt nicht zur Server-Adresse. Beim Anbieter nach einer aktuellen Adresse fragen.¦服务商的证书与服务器地址不匹配。请向服务商索取最新的地址。
Apps from unknown sources¦安装未知来源应用
No description found.¦暂无简介。
Go to episode¦跳到该集
From the beginning¦从头开始
no longer available¦已不可用
English¦英语
Türkçe¦土耳其语
العربية¦阿拉伯语
Français¦法语
Español¦西班牙语
Italiano¦意大利语
Nederlands¦荷兰语
Polski¦波兰语
Português¦葡萄牙语
Русский¦俄语
हिन्दी¦印地语
日本語¦日语
한국어¦韩语
Shqip¦阿尔巴尼亚语
Ex-YU / Balkan¦前南 / 巴尔干
Ελληνικά¦希腊语
Română¦罗马尼亚语
Magyar¦匈牙利语
Čeština¦捷克语
Svenska¦瑞典语
Dansk¦丹麦语
Norsk¦挪威语
Suomi¦芬兰语
فارسی¦波斯语
Kurdî¦库尔德语
עברית¦希伯来语
Українська¦乌克兰语
Български¦保加利亚语
اردو¦乌尔都语
Tiếng Việt¦越南语
ไทย¦泰语
Filipino¦菲律宾语
"""

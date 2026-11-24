package com.ipvio.stream

// T-055: UI texts that had no row in any language (search, sports, VPN, downloads, diagnostics).
// Format: key¦English¦Turkish¦Bosnian¦Croatian¦Serbian¦Spanish¦French¦Italian. Dutch, Arabic and Polish: own columns below.
// Drafted, not reviewed by a native speaker.
val DictT055 = """
Show all ›¦Show all ›¦Tümünü göster ›¦Prikaži sve ›¦Prikaži sve ›¦Prikaži sve ›¦Ver todo ›¦Tout afficher ›¦Mostra tutto ›
Last searches¦Last searches¦Son aramalar¦Posljednja pretraživanja¦Posljednja pretraživanja¦Poslednje pretrage¦Búsquedas recientes¦Recherches récentes¦Ricerche recenti
Best match¦Best match¦En iyi eşleşme¦Najbolje podudaranje¦Najbolje podudaranje¦Najbolje podudaranje¦Mejor coincidencia¦Meilleure correspondance¦Migliore corrispondenza
Not in your playlist¦Not in your playlist¦Listenizde yok¦Nije na tvojoj listi¦Nije na tvom popisu¦Nije na tvojoj listi¦No está en tu lista¦Absent de ta liste¦Non nella tua lista
Upcoming¦Upcoming¦Yaklaşan¦Nadolazeće¦Nadolazeće¦Predstojeće¦Próximamente¦À venir¦In arrivo
Choose a league¦Choose a league¦Bir lig seç¦Odaberi ligu¦Odaberi ligu¦Izaberi ligu¦Elige una liga¦Choisir une ligue¦Scegli un campionato
Liga wählen¦Choose league¦Lig seç¦Odaberi ligu¦Odaberi ligu¦Izaberi ligu¦Elegir liga¦Choisir la ligue¦Scegli la lega
All sports¦All sports¦Tüm sporlar¦Svi sportovi¦Svi sportovi¦Svi sportovi¦Todos los deportes¦Tous les sports¦Tutti gli sport
Wettbewerb¦Competition¦Turnuva¦Takmičenje¦Natjecanje¦Takmičenje¦Competición¦Compétition¦Competizione
Filmography¦Filmography¦Filmografi¦Filmografija¦Filmografija¦Filmografija¦Filmografía¦Filmographie¦Filmografia
Datei hochladen¦Upload file¦Dosya yükle¦Pošalji datoteku¦Pošalji datoteku¦Pošalji datoteku¦Subir archivo¦Importer un fichier¦Carica file
Zuletzt geschaut · Live TV¦Recently watched · Live TV¦Son izlenenler · Canlı TV¦Nedavno gledano · Live TV¦Nedavno gledano · Live TV¦Nedavno gledano · Live TV¦Visto recientemente · TV en directo¦Vus récemment · TV en direct¦Visti di recente · TV in diretta
WireGuard configuration (VPN)¦WireGuard configuration (VPN)¦WireGuard yapılandırması (VPN)¦WireGuard konfiguracija (VPN)¦WireGuard konfiguracija (VPN)¦WireGuard konfiguracija (VPN)¦Configuración de WireGuard (VPN)¦Configuration WireGuard (VPN)¦Configurazione WireGuard (VPN)
Wähle die .conf-Datei deines VPN-Anbieters. Sie enthält einen privaten Schlüssel: sende sie nur über dein eigenes WLAN.¦Choose the .conf file of your VPN provider. It contains a private key: only send it over your own Wi-Fi.¦VPN sağlayıcının .conf dosyasını seç. Özel anahtar içerir: yalnızca kendi Wi-Fi'nden gönder.¦Izaberi .conf datoteku svog VPN provajdera. Sadrži privatni ključ: šalji je samo preko svoje Wi-Fi mreže.¦Odaberi .conf datoteku svog VPN pružatelja. Sadrži privatni ključ: šalji je samo preko vlastite Wi-Fi mreže.¦Izaberi .conf datoteku svog VPN provajdera. Sadrži privatni ključ: šalji je samo preko sopstvene Wi-Fi mreže.¦Elige el archivo .conf de tu proveedor VPN. Contiene una clave privada: envíalo solo por tu propia Wi-Fi.¦Choisis le fichier .conf de ton fournisseur VPN. Il contient une clé privée : ne l'envoie que par ton propre Wi-Fi.¦Scegli il file .conf del tuo provider VPN. Contiene una chiave privata: inviala solo tramite il tuo Wi-Fi.
Choose the .conf file of your VPN provider. It contains a private key: only send it over your own Wi-Fi.¦Choose the .conf file of your VPN provider. It contains a private key: only send it over your own Wi-Fi.¦VPN sağlayıcının .conf dosyasını seç. Özel anahtar içerir: yalnızca kendi Wi-Fi'nden gönder.¦Izaberi .conf datoteku svog VPN provajdera. Sadrži privatni ključ: šalji je samo preko svoje Wi-Fi mreže.¦Odaberi .conf datoteku svog VPN pružatelja. Sadrži privatni ključ: šalji je samo preko vlastite Wi-Fi mreže.¦Izaberi .conf datoteku svog VPN provajdera. Sadrži privatni ključ: šalji je samo preko sopstvene Wi-Fi mreže.¦Elige el archivo .conf de tu proveedor VPN. Contiene una clave privada: envíalo solo por tu propia Wi-Fi.¦Choisis le fichier .conf de ton fournisseur VPN. Il contient une clé privée : ne l'envoie que par ton propre Wi-Fi.¦Scegli il file .conf del tuo provider VPN. Contiene una chiave privata: inviala solo tramite il tuo Wi-Fi.
Tippe einen Sendungstitel ein, z. B. Formula 1.¦Type a programme title, e.g. Formula 1.¦Bir program adı yaz, ör. Formula 1.¦Upiši naziv emisije, npr. Formula 1.¦Upiši naziv emisije, npr. Formula 1.¦Upiši naziv emisije, npr. Formula 1.¦Escribe el título de un programa, p. ej. Formula 1.¦Tape le titre d'une émission, par ex. Formula 1.¦Digita il titolo di un programma, ad es. Formula 1.
Type a programme title, e.g. Formula 1.¦Type a programme title, e.g. Formula 1.¦Bir program adı yaz, ör. Formula 1.¦Upiši naziv emisije, npr. Formula 1.¦Upiši naziv emisije, npr. Formula 1.¦Upiši naziv emisije, npr. Formula 1.¦Escribe el título de un programa, p. ej. Formula 1.¦Tape le titre d'une émission, par ex. Formula 1.¦Digita il titolo di un programma, ad es. Formula 1.
"""

val DictT055Nl = """
Show all ›¦Alles tonen ›
Last searches¦Recente zoekopdrachten
Best match¦Beste match
Not in your playlist¦Niet in je playlist
Upcoming¦Binnenkort
Choose a league¦Kies een competitie
Liga wählen¦Competitie kiezen
All sports¦Alle sporten
Wettbewerb¦Competitie
Filmography¦Filmografie
Datei hochladen¦Bestand uploaden
Zuletzt geschaut · Live TV¦Recent bekeken · Live-tv
WireGuard configuration (VPN)¦WireGuard-configuratie (VPN)
Wähle die .conf-Datei deines VPN-Anbieters. Sie enthält einen privaten Schlüssel: sende sie nur über dein eigenes WLAN.¦Kies het .conf-bestand van je VPN-aanbieder. Het bevat een privésleutel: stuur het alleen via je eigen wifi.
Choose the .conf file of your VPN provider. It contains a private key: only send it over your own Wi-Fi.¦Kies het .conf-bestand van je VPN-aanbieder. Het bevat een privésleutel: stuur het alleen via je eigen wifi.
Tippe einen Sendungstitel ein, z. B. Formula 1.¦Typ een programmatitel, bijv. Formula 1.
Type a programme title, e.g. Formula 1.¦Typ een programmatitel, bijv. Formula 1.
"""

val DictT055Ar = """
Show all ›¦عرض الكل ›
Last searches¦عمليات البحث الأخيرة
Best match¦أفضل تطابق
Not in your playlist¦غير موجود في قائمتك
Upcoming¦قادم
Choose a league¦اختر دوريًا
Liga wählen¦اختيار الدوري
All sports¦كل الرياضات
Wettbewerb¦المسابقة
Filmography¦سيرة الأفلام
Datei hochladen¦رفع ملف
Zuletzt geschaut · Live TV¦شوهد مؤخرًا · البث المباشر
WireGuard configuration (VPN)¦إعداد WireGuard (VPN)
Wähle die .conf-Datei deines VPN-Anbieters. Sie enthält einen privaten Schlüssel: sende sie nur über dein eigenes WLAN.¦اختر ملف .conf الخاص بمزوّد VPN. يحتوي على مفتاح خاص: أرسله عبر شبكة Wi-Fi الخاصة بك فقط.
Choose the .conf file of your VPN provider. It contains a private key: only send it over your own Wi-Fi.¦اختر ملف .conf الخاص بمزوّد VPN. يحتوي على مفتاح خاص: أرسله عبر شبكة Wi-Fi الخاصة بك فقط.
Tippe einen Sendungstitel ein, z. B. Formula 1.¦اكتب عنوان برنامج، مثل Formula 1.
Type a programme title, e.g. Formula 1.¦اكتب عنوان برنامج، مثل Formula 1.
"""

val DictT055Pl = """
Show all ›¦Pokaż wszystko ›
Last searches¦Ostatnie wyszukiwania
Best match¦Najlepsze dopasowanie
Not in your playlist¦Nie ma na twojej playliście
Upcoming¦Nadchodzące
Choose a league¦Wybierz ligę
Liga wählen¦Wybierz ligę
All sports¦Wszystkie sporty
Wettbewerb¦Rozgrywki
Filmography¦Filmografia
Datei hochladen¦Prześlij plik
Zuletzt geschaut · Live TV¦Ostatnio oglądane · Live TV
WireGuard configuration (VPN)¦Konfiguracja WireGuard (VPN)
Wähle die .conf-Datei deines VPN-Anbieters. Sie enthält einen privaten Schlüssel: sende sie nur über dein eigenes WLAN.¦Wybierz plik .conf swojego dostawcy VPN. Zawiera klucz prywatny: wysyłaj go tylko przez własne Wi-Fi.
Choose the .conf file of your VPN provider. It contains a private key: only send it over your own Wi-Fi.¦Wybierz plik .conf swojego dostawcy VPN. Zawiera klucz prywatny: wysyłaj go tylko przez własne Wi-Fi.
Tippe einen Sendungstitel ein, z. B. Formula 1.¦Wpisz tytuł programu, np. Formula 1.
Type a programme title, e.g. Formula 1.¦Wpisz tytuł programu, np. Formula 1.
"""

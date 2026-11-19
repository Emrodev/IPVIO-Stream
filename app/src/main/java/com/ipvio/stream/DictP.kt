package com.ipvio.stream

// Translations part P: consent dialog for a server certificate that does not match its address (#69).
// Format: de ¦ en ¦ tr ¦ bs ¦ hr ¦ sr ¦ es ¦ fr ¦ it ¦ nl   (hr/sr empty = same as bs)
// "Abbrechen" is already in DictA.
val DictP = """
Unverschlüsselte Verbindung?¦Unencrypted connection?¦Şifresiz bağlantı?¦Nešifrovana veza?¦¦¦¿Conexión sin cifrar?¦Connexion non chiffrée ?¦Connessione non cifrata?¦Onversleutelde verbinding?
Das Zertifikat dieses Servers passt nicht zu seiner Adresse. Die Verbindung kann ohne Verschlüsselung fortgesetzt werden. Zugangsdaten und Inhalte können dann im Netzwerk mitgelesen werden.¦This server's certificate does not match its address. The connection can continue without encryption. Login data and content can then be read on the network.¦Bu sunucunun sertifikası adresiyle eşleşmiyor. Bağlantı şifrelenmeden sürdürülebilir. Bu durumda giriş bilgileri ve içerik ağda okunabilir.¦Certifikat ovog servera ne odgovara njegovoj adresi. Veza se može nastaviti bez šifriranja. Tada se podaci za prijavu i sadržaj mogu čitati na mreži.¦¦¦El certificado de este servidor no coincide con su dirección. La conexión puede continuar sin cifrado. Los datos de acceso y el contenido podrían leerse en la red.¦Le certificat de ce serveur ne correspond pas à son adresse. La connexion peut continuer sans chiffrement. Les identifiants et le contenu peuvent alors être lus sur le réseau.¦Il certificato di questo server non corrisponde al suo indirizzo. La connessione può continuare senza crittografia. Credenziali e contenuti possono così essere letti in rete.¦Het certificaat van deze server past niet bij het adres. De verbinding kan zonder versleuteling doorgaan. Inloggegevens en inhoud kunnen dan op het netwerk worden meegelezen.
Ohne Verschlüsselung verbinden¦Connect without encryption¦Şifresiz bağlan¦Poveži bez šifriranja¦¦¦Conectar sin cifrado¦Se connecter sans chiffrement¦Connetti senza crittografia¦Verbinden zonder versleuteling
Kennung: VLC¦Identifier: VLC¦Kimlik: VLC¦Identifikacija: VLC¦¦¦Identificador: VLC¦Identifiant: VLC¦ID: VLC¦ID: VLC
Kennung: IPVIO¦Identifier: IPVIO¦Kimlik: IPVIO¦Identifikacija: IPVIO¦¦¦Identificador: IPVIO¦Identifiant: IPVIO¦ID: IPVIO¦ID: IPVIO
"""

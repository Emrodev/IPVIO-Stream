package com.ipvio.stream

// T-081: texts of the phone report on the diagnostics page (Settings > Diagnostics).
// Format: key¦English¦Turkish¦Bosnian¦Croatian¦Serbian¦Spanish¦French¦Italian. Dutch, Arabic and Polish: own columns below.
// Drafted, not reviewed by a native speaker.
val DictT081 = """
Am Handy öffnen¦Open on phone¦Telefonda aç¦Otvori na telefonu¦Otvori na telefonu¦Otvori na telefonu¦Abrir en el móvil¦Ouvrir sur le téléphone¦Apri sul telefono
Am Handy beenden¦Stop phone access¦Telefon erişimini durdur¦Zaustavi pristup telefonu¦Zaustavi pristup telefonu¦Zaustavi pristup telefonu¦Detener acceso desde el móvil¦Arrêter l'accès depuis le téléphone¦Interrompi accesso dal telefono
Auf dem Handy: „Letzten Absturzbericht anzeigen“ öffnen. Dort steht der ganze Bericht als Text und lässt sich markieren und kopieren.¦On the phone, open „Show last crash report“. The whole report is there as text; select and copy it.¦Telefonda „Son çökme raporunu göster“ seçeneğini aç. Tüm rapor metin olarak orada; seçip kopyalayabilirsin.¦Na telefonu otvori „Prikaži zadnji izvještaj o padu“. Cijeli izvještaj je tu kao tekst; označi ga i kopiraj.¦Na telefonu otvori „Prikaži zadnji izvještaj o padu“. Cijeli izvještaj je tu kao tekst; označi ga i kopiraj.¦Na telefonu otvorite „Prikaži poslednji izveštaj o padu“. Ceo izveštaj je tu kao tekst; označite ga i kopirajte.¦En el móvil, abre „Ver último informe de error“. El informe completo está como texto; selecciónalo y cópialo.¦Sur le téléphone, ouvrez « Afficher le dernier rapport de plantage ». Le rapport complet s'affiche en texte ; sélectionnez-le et copiez-le.¦Sul telefono apri „Mostra ultimo rapporto di arresto“. Il rapporto completo è qui come testo; selezionalo e copialo.
Der Bericht enthält keine Adressen, Benutzernamen oder Passwörter. „Bericht kopieren“ legt ihn nur in die Zwischenablage des Fernsehers. Zum Schicken am Handy nutze „Am Handy öffnen“.¦The report contains no addresses, user names or passwords. „Copy report“ only puts it on the TV's clipboard. To send it from a phone, use „Open on phone“.¦Rapor adres, kullanıcı adı veya parola içermez. „Raporu kopyala“ yalnızca televizyonun panosuna koyar. Telefondan göndermek için „Telefonda aç“ kullan.¦Izvještaj ne sadrži adrese, korisnička imena ni lozinke. „Kopiraj izvještaj“ ga samo stavlja u međuspremnik televizora. Za slanje s telefona koristi „Otvori na telefonu“.¦Izvještaj ne sadrži adrese, korisnička imena ni lozinke. „Kopiraj izvještaj“ ga samo stavlja u međuspremnik televizora. Za slanje s telefona koristi „Otvori na telefonu“.¦Izveštaj ne sadrži adrese, korisnička imena ni lozinke. „Kopiraj izveštaj“ ga samo stavlja u memoriju televizora. Za slanje s telefona koristi „Otvori na telefonu“.¦El informe no contiene direcciones, nombres de usuario ni contraseñas. „Copiar informe“ solo lo pone en el portapapeles de la TV. Para enviarlo desde el móvil, usa „Abrir en el móvil“.¦Le rapport ne contient ni adresses, ni noms d'utilisateur, ni mots de passe. « Copier le rapport » le place seulement dans le presse-papiers de la TV. Pour l'envoyer depuis le téléphone, utilisez « Ouvrir sur le téléphone ».¦Il rapporto non contiene indirizzi, nomi utente o password. „Copia rapporto“ lo mette solo negli appunti della TV. Per inviarlo dal telefono usa „Apri sul telefono“.
"""

val DictT081Nl = """
Am Handy öffnen¦Openen op telefoon
Am Handy beenden¦Toegang via telefoon stoppen
Auf dem Handy: „Letzten Absturzbericht anzeigen“ öffnen. Dort steht der ganze Bericht als Text und lässt sich markieren und kopieren.¦Open op de telefoon „Laatste crashrapport tonen“. Het volledige rapport staat er als tekst; selecteer en kopieer het.
Der Bericht enthält keine Adressen, Benutzernamen oder Passwörter. „Bericht kopieren“ legt ihn nur in die Zwischenablage des Fernsehers. Zum Schicken am Handy nutze „Am Handy öffnen“.¦Het rapport bevat geen adressen, gebruikersnamen of wachtwoorden. „Rapport kopiëren“ zet het alleen op het klembord van de tv. Om het vanaf de telefoon te versturen, gebruik „Openen op telefoon“.
"""

val DictT081Ar = """
Am Handy öffnen¦فتح على الهاتف
Am Handy beenden¦إيقاف الوصول من الهاتف
Auf dem Handy: „Letzten Absturzbericht anzeigen“ öffnen. Dort steht der ganze Bericht als Text und lässt sich markieren und kopieren.¦افتح على الهاتف „عرض آخر تقرير عن الأعطال“. التقرير الكامل موجود هناك كنص؛ حدّده وانسخه.
Der Bericht enthält keine Adressen, Benutzernamen oder Passwörter. „Bericht kopieren“ legt ihn nur in die Zwischenablage des Fernsehers. Zum Schicken am Handy nutze „Am Handy öffnen“.¦لا يحتوي التقرير على عناوين أو أسماء مستخدمين أو كلمات مرور. „نسخ التقرير“ يضعه فقط في الحافظة على التلفاز. لإرساله من الهاتف استخدم „فتح على الهاتف“.
"""

val DictT081Pl = """
Am Handy öffnen¦Otwórz na telefonie
Am Handy beenden¦Zakończ dostęp z telefonu
Auf dem Handy: „Letzten Absturzbericht anzeigen“ öffnen. Dort steht der ganze Bericht als Text und lässt sich markieren und kopieren.¦Na telefonie otwórz „Pokaż ostatni raport o awarii“. Cały raport jest tam jako tekst; zaznacz go i skopiuj.
Der Bericht enthält keine Adressen, Benutzernamen oder Passwörter. „Bericht kopieren“ legt ihn nur in die Zwischenablage des Fernsehers. Zum Schicken am Handy nutze „Am Handy öffnen“.¦Raport nie zawiera adresów, nazw użytkowników ani haseł. „Kopiuj raport“ umieszcza go tylko w schowku telewizora. Aby wysłać go z telefonu, użyj „Otwórz na telefonie“.
"""

package com.ipvio.stream

// Translations for sound levelling (T-028).
// Format as in DictA–J: de ¦ en ¦ tr ¦ bs ¦ hr ¦ sr ¦ es ¦ fr ¦ it (hr/sr empty = same as bs)
val DictAudio = """
Ton angleichen¦Sound levelling¦Ses dengeleme¦Ujednačavanje zvuka¦¦¦Nivelación de sonido¦Égalisation du son¦Livellamento audio
Klare Sprache¦Clear speech¦Net konuşma¦Jasan govor¦¦¦Voz clara¦Voix claire¦Voce chiara
Nachtmodus¦Night mode¦Gece modu¦Noćni način¦¦¦Modo nocturno¦Mode nuit¦Modalità notte
Eigene Einstellung¦Custom¦Özel¦Prilagođeno¦¦¦Personalizado¦Personnalisé¦Personalizzato
Maximale Anhebung¦Maximum boost¦Maksimum artırma¦Maksimalno pojačanje¦¦¦Aumento máximo¦Amplification maximale¦Amplificazione massima
Dynamik angleichen¦Even out loud and quiet¦Yüksek ve alçak sesi dengele¦Izjednači glasno i tiho¦¦¦Igualar fuerte y suave¦Égaliser fort et faible¦Uniforma forte e piano
Dialoge hervorheben¦Boost dialogue¦Diyalogları öne çıkar¦Istakni dijaloge¦¦¦Realzar diálogos¦Renforcer les dialogues¦Esalta i dialoghi
Stark¦Strong¦Güçlü¦Jako¦¦¦Fuerte¦Fort¦Forte
Viele Sender sind leiser gemischt als YouTube oder Netflix (Fernsehnorm −23 LUFS statt etwa −16). „Standard“ hebt leise Sender automatisch an, gleicht laute und leise Stellen an und macht Dialoge etwas deutlicher – ohne Übersteuern.¦Many channels are mixed quieter than YouTube or Netflix (TV standard −23 LUFS instead of about −16). “Standard” raises quiet channels automatically, evens out loud and quiet parts and makes dialogue a little clearer – without distortion.¦Birçok kanal YouTube veya Netflix’ten daha düşük seviyede miksajlanır (TV standardı yaklaşık −16 yerine −23 LUFS). “Standart” düşük sesli kanalları otomatik olarak yükseltir, yüksek ve alçak bölümleri dengeler ve diyalogları biraz daha net yapar – bozulma olmadan.¦Mnogi kanali su tiše miksani od YouTubea ili Netflixa (TV norma −23 LUFS umjesto oko −16). „Standardno“ automatski pojačava tihe kanale, ujednačava glasne i tihe dijelove i čini dijaloge malo jasnijim – bez izobličenja.¦¦¦Muchos canales están mezclados más bajos que YouTube o Netflix (norma de TV −23 LUFS en lugar de unos −16). “Estándar” sube automáticamente los canales bajos, iguala las partes fuertes y suaves y hace los diálogos un poco más claros, sin distorsión.¦De nombreuses chaînes sont mixées plus bas que YouTube ou Netflix (norme TV −23 LUFS au lieu d’environ −16). « Standard » relève automatiquement les chaînes faibles, égalise les passages forts et faibles et rend les dialogues un peu plus clairs – sans distorsion.¦Molti canali sono mixati a un volume più basso di YouTube o Netflix (norma TV −23 LUFS invece di circa −16). “Standard” alza automaticamente i canali bassi, uniforma le parti forti e deboli e rende i dialoghi un po’ più chiari, senza distorsione.
„Klare Sprache“ hebt Stimmen stärker hervor. „Nachtmodus“ dämpft laute Stellen stark – gut zum leisen Hören. Wird der Ton unverändert an einen Receiver durchgeleitet (Dolby, DTS), bleibt er unbearbeitet.¦“Clear speech” brings voices further forward. “Night mode” strongly tames loud parts – good for listening quietly. Sound passed through unchanged to a receiver (Dolby, DTS) is not processed.¦“Net konuşma” sesleri daha fazla öne çıkarır. “Gece modu” yüksek bölümleri güçlü şekilde bastırır – sessiz dinlemek için iyidir. Bir alıcıya değiştirilmeden aktarılan ses (Dolby, DTS) işlenmez.¦„Jasan govor“ jače ističe glasove. „Noćni način“ snažno prigušuje glasne dijelove – dobro za tiho slušanje. Zvuk koji se nepromijenjen prosljeđuje risiveru (Dolby, DTS) ne obrađuje se.¦¦¦“Voz clara” resalta más las voces. “Modo nocturno” atenúa mucho las partes fuertes, ideal para escuchar a bajo volumen. El sonido que se envía sin cambios a un receptor (Dolby, DTS) no se procesa.¦« Voix claire » met davantage les voix en avant. « Mode nuit » atténue fortement les passages forts – idéal pour écouter à bas volume. Le son transmis tel quel à un récepteur (Dolby, DTS) n’est pas traité.¦“Voce chiara” mette più in risalto le voci. “Modalità notte” attenua molto le parti forti, ideale per ascoltare a basso volume. L’audio inviato invariato a un ricevitore (Dolby, DTS) non viene elaborato.
"""

@Volatile private var audioTextsLoaded = false

/** Loads the rows above (kept out of Lang.kt like the Wishbox texts, see DictW.kt). Loading is additive and runs once. */
fun loadAudioLevelTexts() {
    if (audioTextsLoaded) return
    audioTextsLoaded = true
    I18nCore.load(DictAudio)
    I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictNlAudio)
}

// Dutch column for the rows above
val DictNlAudio = """
Ton angleichen¦Geluid egaliseren
Klare Sprache¦Heldere spraak
Nachtmodus¦Nachtmodus
Eigene Einstellung¦Aangepast
Maximale Anhebung¦Maximale versterking
Dynamik angleichen¦Hard en zacht gelijktrekken
Dialoge hervorheben¦Dialogen versterken
Stark¦Sterk
Viele Sender sind leiser gemischt als YouTube oder Netflix (Fernsehnorm −23 LUFS statt etwa −16). „Standard“ hebt leise Sender automatisch an, gleicht laute und leise Stellen an und macht Dialoge etwas deutlicher – ohne Übersteuern.¦Veel zenders zijn zachter gemixt dan YouTube of Netflix (tv-norm −23 LUFS in plaats van ongeveer −16). „Standaard” versterkt zachte zenders automatisch, trekt harde en zachte passages gelijk en maakt dialogen iets duidelijker – zonder vervorming.
„Klare Sprache“ hebt Stimmen stärker hervor. „Nachtmodus“ dämpft laute Stellen stark – gut zum leisen Hören. Wird der Ton unverändert an einen Receiver durchgeleitet (Dolby, DTS), bleibt er unbearbeitet.¦„Heldere spraak” zet stemmen sterker naar voren. „Nachtmodus” dempt harde passages sterk – goed om zacht te luisteren. Geluid dat ongewijzigd naar een receiver gaat (Dolby, DTS) wordt niet bewerkt.
"""

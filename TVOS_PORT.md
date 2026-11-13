# IPVIO Stream — نسخة tvOS

تمت إضافة نسخة أصلية لـ Apple TV داخل مجلد [`tvOS/`](tvOS/README.md)، مبنية بـ SwiftUI و**KSPlayer 5.6.2**.

- **البناء:** افتح `tvOS/IPVIOStream.xcodeproj` على macOS مع Xcode 27، واختر Apple TV حقيقياً.
- **البناء عبر GitHub:** [Actions](https://github.com/GpGp700/IPVIO-Stream/actions/workflows/tvos-build.yml) — الناتج غير موقّع ويُنشر في [Releases](https://github.com/GpGp700/IPVIO-Stream/releases).
- **آخر بناء Release:** [tvOS 1.1.0 (build 11)](https://github.com/GpGp700/IPVIO-Stream/releases/tag/tvos-build-43526238384) — شاشة افتتاحية متحركة + دليل قنوات XMLTV + مركز مفضلة؛ تم التحقق: arm64، KSPlayer + 22 إطار FFmpeg، ATS صحيح، وبدون ملفات Debug.
- **التحميل السريع:** [IPA مباشر](https://github.com/GpGp700/IPVIO-Stream/releases/download/tvos-build-43526238384/IPVIOStream-tvOS-unsigned.ipa) — [QR](https://github.com/GpGp700/IPVIO-Stream/releases/download/tvos-build-43526238384/download-ipa-qr.png) — SHA256: `3e37ac5b3c16e10054adbb348bca7cbe054df3552bd35b9d0a6f105710e0f9a4`.
- **التوقيع والتثبيت:** [`tvOS/SIGNING.md`](tvOS/SIGNING.md).
- **المتطلب المهم:** حزمة KSPlayer المجانية لا تحتوي شريحة محاكي tvOS؛ لا بد من جهاز Apple TV.
- **الميزات المنفذة:** Xtream + M3U، مصادر متعددة، أفلام/مسلسلات/حلقات، بث مباشر مع EPG قصير، دليل قنوات XMLTV زمني، مركز مفضلة (قنوات/أفلام/مسلسلات)، بحث بفلاتر، متابعة مشاهدة، مشغل KSPlayer مع FFmpeg ومسارات صوت/ترجمة/جودة وسرعة ومؤقت نوم، تخطي المقدمة، شاشة افتتاحية متحركة، واجهة عربية/إنجليزية.
- **غير المنقول بعد:** Stalker، الرياضة، التسجيل/Timeshift/Catch-up الكامل، الملفات الشخصية/الرقابة الأبوية، النسخ الاحتياطي، تحديثات داخل التطبيق، Top Shelf. التفاصيل في `tvOS/README.md`.

هذا المجلد لا يغيّر تطبيق Android الأصلي؛ التطبيقان يمكن أن يتعايشا في المستودع نفسه.

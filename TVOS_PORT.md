# IPVIO Stream — نسخة tvOS

تمت إضافة نسخة أصلية لـ Apple TV داخل مجلد [`tvOS/`](tvOS/README.md)، مبنية بـ SwiftUI و**KSPlayer 5.6.2**.

- **البناء:** افتح `tvOS/IPVIOStream.xcodeproj` على macOS مع Xcode 27، واختر Apple TV حقيقياً.
- **البناء عبر GitHub:** [Actions](https://github.com/GpGp700/IPVIO-Stream/actions/workflows/tvos-build.yml) — الناتج غير موقّع ويُنشر في [Releases](https://github.com/GpGp700/IPVIO-Stream/releases).
- **آخر بناء Release:** [tvOS unsigned build 6](https://github.com/GpGp700/IPVIO-Stream/releases/tag/tvos-build-43521081627) — واجهة بطابع SWIPTV + إصلاح ATS؛ تم التحقق: arm64، KSPlayer + 22 إطار FFmpeg، وبدون ملفات Debug.
- **التوقيع والتثبيت:** [`tvOS/SIGNING.md`](tvOS/SIGNING.md).
- **المتطلب المهم:** حزمة KSPlayer المجانية لا تحتوي شريحة محاكي tvOS؛ لا بد من جهاز Apple TV.
- **الميزات المنفذة:** Xtream + M3U، مصادر متعددة، أفلام/مسلسلات/حلقات، بث مباشر مع EPG قصير، بحث، مفضلة، متابعة مشاهدة، مشغل KSPlayer مع FFmpeg ومسارات صوت/ترجمة/جودة، تخطي المقدمة، واجهة عربية/إنجليزية.
- **غير المنقول بعد:** Stalker، الرياضة، التسجيل/Timeshift، الملفات الشخصية/الرقابة الأبوية، النسخ الاحتياطي، تحديثات داخل التطبيق. التفاصيل في `tvOS/README.md`.

هذا المجلد لا يغيّر تطبيق Android الأصلي؛ التطبيقان يمكن أن يتعايشا في المستودع نفسه.

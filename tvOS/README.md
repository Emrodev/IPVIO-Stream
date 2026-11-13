# IPVIO Stream — tvOS

نسخة أصلية من **IPVIO Stream** لـ Apple TV، مبنية بـ **SwiftUI** و**KSPlayer** بدلاً من Kotlin/Media3.
هذا المجلد مستقل عن تطبيق Android الأصلي ولا يغيّر أي ملف من ملفاته.

> **تنبيه:** التطبيق لا يوفّر أي قنوات أو أفلام أو قوائم. هو مشغّل لقوائم تضيفها أنت (Xtream Codes أو M3U). استخدم فقط مصادر تملك حق مشاهدتها.

---

## الحالة

| العنصر | الحالة |
| --- | --- |
| تطبيق tvOS أصلي (SwiftUI) | ✅ |
| Xtream Codes (player_api) | ✅ |
| قوائم M3U / M3U8 | ✅ |
| عدة قوائم/مصادر معاً | ✅ |
| أفلام / مسلسلات / مواسم / حلقات | ✅ |
| بث مباشر + دليل EPG قصير | ✅ |
| بحث وتصنيفات ومفضلة وقائمة | ✅ |
| متابعة المشاهدة (Resume) | ✅ |
| مشغل KSPlayer 5.6.2 مدمج | ✅ |
| مسارات صوت/ترجمة/جودة | ✅ |
| تخطي المقدمة/النهاية (TheIntroDB) | ✅ |
| TMDB + OMDb اختياريان | ✅ |
| واجهة عربية/إنجليزية RTL | ✅ |
| Stalker Portal | ⏳ لم يُنقل بعد |
| الملفات الشخصية/الرقابة الأبوية | ⏳ |
| التسجيل / Timeshift / Catch-up UI | ⏳ |
| الرياضة / الدليل الكامل / استيراد الهاتف | ⏳ |

الهدف من هذه النسخة هو نواة كاملة وقابلة للتشغيل: استيراد القائمة، تصفحها، البحث، التفاصيل، البث المباشر، والمشغل. بقية ميزات تطبيق Android الضخم (42 ألف سطر Kotlin) يمكن نقلها تدريجياً فوق هذه البنية.

---

## المتطلبات

- **macOS** مع **Xcode 27** أو أحدث (حزمة KSPlayer 5.6.2 تستخدم Swift tools 6.2).
- **Apple TV** يعمل بـ **tvOS 27** (تم ضبط هدف النشر على 27.0).
- حساب مطوّر Apple للتوقيع على الجهاز.
- تفعيل **وضع المطوّر** على Apple TV: الإعدادات ← الخصوصية والأمان ← وضع المطوّر.
- اتصال إنترنت لتنزيل حزم Swift Package عند أول فتح.

> **مهم:** حزمة KSPlayer المجانية (GPL) تأتي كـ `xcframework` يحتوي شريحة **tvos-arm64 فقط**، ولا تحتوي شريحة محاكي tvOS. لذلك **شغّل التطبيق على Apple TV حقيقي، وليس على المحاكي**. المشروع نفسه مضبوط على `SUPPORTED_PLATFORMS = appletvos`.

### إذا كان Xcode لديك أقدم

افتح `project.yml` و`IPVIOStream.xcodeproj/project.pbxproj` وغيّر:

```text
TVOS_DEPLOYMENT_TARGET = 27.0
```

إلى نسخة tvOS المتوفرة لديك (مثلاً `18.0`). لا تحتاج تغيير أي كود.

---

## البناء

### الطريقة 1: Xcode مباشرة

```bash
cd tvOS
open IPVIOStream.xcodeproj
```

1. اختر Target **IPVIOStream**.
2. تبويب **Signing & Capabilities** → اختر **Team** الخاص بك، وغيّر Bundle Identifier إذا لزم.
3. اختر **Apple TV** الحقيقي من قائمة الأجهزة.
4. اضغط **Run** (⌘R). أول بناء ينزّل KSPlayer وFFmpegKit تلقائياً.

### الطريقة 2: سطر الأوامر

```bash
cd tvOS
xcodebuild \
  -project IPVIOStream.xcodeproj \
  -scheme IPVIOStream \
  -destination 'generic/platform=tvOS' \
  -allowProvisioningUpdates \
  DEVELOPMENT_TEAM=XXXXXXXXXX \
  build
```

استبدل `XXXXXXXXXX` بمعرّف فريقك. الناتج في `~/Library/Developer/Xcode/DerivedData/.../Build/Products/`.

### الطريقة 3: GitHub Actions (بدون Mac)

النسخة مرفوعة في Fork داخل الحساب:

- **المستودع:** https://github.com/GpGp700/IPVIO-Stream
- **Workflow:** https://github.com/GpGp700/IPVIO-Stream/actions/workflows/tvos-build.yml
- **الفرع:** `tvos-ci`

[![Build tvOS](https://github.com/GpGp700/IPVIO-Stream/actions/workflows/tvos-build.yml/badge.svg?branch=tvos-ci)](https://github.com/GpGp700/IPVIO-Stream/actions/workflows/tvos-build.yml)

يشغّل الـ Workflow بناءً حقيقياً على `macos-26` مع Xcode 26.6 وtvOS SDK 26.5. لأن هدف المشروع المحلي هو tvOS 27، يمرّر CI تجاوزاً آمناً:

```text
TVOS_DEPLOYMENT_TARGET=18.0
```

الكود لا يستخدم أي API أحدث من tvOS 18، لذلك يعمل البناء على SDK 26 ويظل التطبيق قابلاً للتشغيل على tvOS 27. الناتج يُرفع كـ **Artifact باسم `IPVIOStream-tvOS-unsigned`** (ملف `.app` غير موقّع). هذا يثبت أن المشروع يُبنى، لكنه **غير قابل للتثبيت** على Apple TV قبل توقيعه بحسابك.

> لتوقيع تلقائي في CI تحتاج إضافة شهادة Apple Distribution وProvisioning Profile إلى GitHub Secrets؛ راجع دليل التوقيع الكامل: [`SIGNING.md`](SIGNING.md).

عند نجاح البناء ينشر الـ Workflow أيضاً **GitHub Release** باسم `tvOS unsigned build N` مع ملف `.zip`؛ ستجده هنا:

- https://github.com/GpGp700/IPVIO-Stream/releases



المشروع مولّد بسكربت Python ولا يعتمد على XcodeGen:

```bash
cd tvOS
python3 tools/generate_xcodeproj.py
```

يوجد أيضاً `project.yml` لمن يفضّل [XcodeGen](https://github.com/yonaskolb/XcodeGen):

```bash
brew install xcodegen
cd tvOS
xcodegen generate
```

> عند إضافة ملف Swift جديد، أعد تشغيل `generate_xcodeproj.py` (أو استخدم XcodeGen) حتى يدخل الملف في Target.

---

## الاستخدام

1. عند أول تشغيل اختر **إضافة قائمة**.
2. **Xtream Codes**: أدخل اسم السيرفر (مثل `http://host:8080`) واسم المستخدم وكلمة المرور. يمكنك أيضاً لصق رابط `get.php` كاملاً في خانة السيرفر.
3. **M3U**: الصق رابط القائمة، أو رابط `get.php` وسيُكتشف كـ Xtream تلقائياً.
4. بعد الاستيراد تظهر الرئيسية: متابعة المشاهدة، مباشر الآن، أفلام، مسلسلات، الأعلى تقييماً، المفضلة.
5. من **الإعدادات** يمكنك:
   - إضافة قائمة ثانية وتفعيل «كل المصادر» تلقائياً.
   - ضبط أقصى جودة (تلقائي / HD / Full HD / 4K).
   - إضافة مفاتيح TMDB وOMDb (اختيارية) للوصف العربي والملصقات والتقييمات.
   - تصفية اللغات، لغة الصوت والترجمة، حجم التخزين المؤقت.
   - اختيار لغة الواجهة (النظام/العربية/English).

### أزرار جهاز التحكم في المشغل

| الزر | الوظيفة |
| --- | --- |
| Play/Pause | تشغيل/إيقاف |
| Menu | إخفاء الأدوات أولاً، ثم إغلاق المشغل |
| يسار/يمين (والأدوات مخفية) | رجوع/تقدّم 15 ثانية |
| أعلى/أسفل (والأدوات مخفية) | إظهار الأدوات |
| Select على شريط التقدم | سحب/تقديم |

---

## الميزات المنقولة

### المكتبة
- استيراد Xtream عبر `player_api.php` مع تصنيفات وأفلام ومسلسلات.
- حلقات المسلسلات تُحمّل عند فتح المسلسل فقط (Lazy)، لتسريع الاستيراد.
- قوائم M3U ضخمة: قراءة سطراً بسطر، دمج الحلقات المتكررة كمصادر بديلة، تجاهل الفواصل.
- كشف اللغة من اسم القناة/المجموعة (AR/EN/FR/DE/TR/ES/IT/NL/PT/RU/HI/UR/FA...) ومن النص العربي/السيريلي.
- كشف الجودة (4K/FHD/HD) والاحتفاظ بها للعرض.
- عدة قوائم مفعّلة تُدمج مع شارة مصدر، مع إمكانية تصفية المصدر في التصفح.

### الواجهة
- شريط جانبي بتصميم IPVIO (أخضر على كحلي) متوافق مع RTL.
- رئيسية بصورة متغيرة تلقائياً كل 10 ثوانٍ، وصفوف أفقية أصلية tvOS.
- تصفح شبكي مع تصنيفات وترتيب (اسم/تقييم/سنة).
- تفاصيل مع غلاف، وصف، تقييمات IMDb/RT/TMDB، طاقم العمل، المواسم والحلقات.
- بحث فوري مع تأخير 250ms.
- بث مباشر مع تصنيفات، شعارات، ومعلومات «الآن/التالي» عند التركيز على القناة.

### المشاهدة
- **KSPlayer 5.6.2** مع محرك FFmpeg (KSMEPlayer) لدعم MPEG-TS وAC3/E-AC3/DTS وMKV.
- واجهة مشغل مخصصة tvOS: شريط تقدم، 15 ثانية، حلقة تالية، جودة، صوت، ترجمة، سرعة، ملء الشاشة، معلومات، مؤقت نوم.
- استئناف من موضع المشاهدة، وحفظ تلقائي كل 8 ثوانٍ.
- تشغيل الحلقة التالية تلقائياً مع عدّاد.
- التراجع التلقائي إلى مصدر آخر عند فشل الجودة، وإعادة اتصال تلقائية للبث المباشر.
- تخطي المقدمة/الملخص/النهاية عبر TheIntroDB عند توفر TMDB ID.
- اختيار مسار الصوت المفضل تلقائياً حسب اللغة.
- اختيار الترجمة الأولى تلقائياً عند تفعيل الترجمة.

### الخصوصية
- ملف حالة التطبيق (القوائم وبيانات الدخول والمفضلة والسجل) مشفّر بـ **AES-GCM**، والمفتاح في **Keychain**.
- لا يوجد حساب أو سحابة أو إعلانات. المفاتيح تُحفظ على الجهاز فقط.
- ذاكرة القوائم المؤقتة (روابط القنوات) تُخزّن داخل صندوق التطبيق لتسريع الإقلاع، وقد تتضمن بيانات الدخول داخل الروابط. احمِ Apple TV برمز دخول.

---

## بنية المشروع

```text
tvOS/
├── IPVIOStream.xcodeproj/          مشروع Xcode + Scheme مشترك
├── project.yml                      بديل XcodeGen
├── IPVIOStream/
│   ├── App/
│   │   ├── IPVIOStreamApp.swift     نقطة الدخول
│   │   └── AppModel.swift           الحالة، المكتبة، الحفظ، المهام
│   ├── Core/
│   │   ├── Models.swift             Item/Episode/Variant/Playlist...
│   │   ├── Settings.swift           الإعدادات واللغة والجودة
│   │   ├── ContentLogic.swift       كشف اللغة/الجودة/العنوان + مدخلات القوائم
│   │   ├── Localization.swift       نصوص عربية/إنجليزية
│   │   ├── Utilities.swift          أدوات النص/الوقت/المعرّفات
│   │   └── Theme.swift              ألوان وتكوينات IPVIO
│   ├── Services/
│   │   ├── Storage.swift            Keychain + AES + ملفات الحالة
│   │   ├── M3UParser.swift          محلل M3U
│   │   ├── XtreamTypes.swift        نماذج JSON
│   │   ├── XtreamClient.swift       تسجيل الدخول، القوائم، الحلقات، EPG
│   │   ├── PlaylistLoader.swift     تنسيق التنزيل/الاستيراد
│   │   ├── MetadataService.swift    TMDB/OMDb مع كاش
│   │   ├── EPGService.swift         دليل قصير مع كاش
│   │   └── IntroDBService.swift     مقدمة/ملخص/نهاية
│   ├── Player/
│   │   ├── PlayerViewModel.swift    جسر KSPlayer + منطق التشغيل
│   │   ├── PlayerScreen.swift       واجهة المشغل tvOS
│   │   └── PlayerOptionsView.swift  الصوت/الترجمة/الجودة/المعلومات
│   ├── UI/
│   │   ├── RootView.swift           Shell + الشريط الجانبي + Onboarding
│   │   ├── HomeView.swift
│   │   ├── BrowseView.swift
│   │   ├── DetailView.swift
│   │   ├── LiveView.swift
│   │   ├── SearchView.swift
│   │   ├── SettingsView.swift
│   │   ├── AddPlaylistView.swift
│   │   └── UIComponents.swift
│   └── Resources/
│       ├── Info.plist               ATS للـ HTTP + الشبكة المحلية
│       └── Assets.xcassets
└── tools/                           توليد المشروع وسكربتات فحص
```

### تدفق البيانات

```text
AddPlaylistView
   → AppModel.addPlaylist
      → PlaylistLoader (XtreamClient / M3UParser)
         → [MediaItem] + EPG URL
            → Storage.saveCatalog (JSON)
            → AppModel.rebuildDerived
               → Home/Browse/Detail/Live
                  → PlayerViewModel (KSVideoPlayer)
                     → KSPlayer / KSMEPlayer (FFmpeg)
```

---

## تكامل KSPlayer (5.6.2)

- المصدر: `https://github.com/kingslay/KSPlayer.git`
- الإصدار المثبّت: **5.6.2** (Commit `3df1658`، 2 نوفمبر 2026) — أحدث إصدار وقت إنشاء النسخة.
- الحزم: `KSPlayer` + `KSPlayerUI`.
- نستخدم `KSVideoPlayer` (UIViewRepresentable) مع `KSVideoPlayer.Coordinator`، ونبني واجهة تحكم SwiftUI خاصة tvOS بدلاً من واجهة KSPlayer الجاهزة، للتحكم الكامل في:
  - `KSOptions.startPlayTime` للاستئناف.
  - `KSOptions.appendHeader` و`userAgent` للتوافق مع المزوّدين.
  - `KSOptions.firstPlayerType = KSMEPlayer.self` لمحرك FFmpeg.
  - `player.tracks(mediaType: .audio)` و`select(track:)` للصوت.
  - `subtitleModel.subtitleInfos` و`select(subtitleInfo:)` للترجمة.
  - `coordinator.seek(time:)` و`skip(interval:)`.
  - `coordinator.isScaleAspectFill` لملء الشاشة.
  - `layer.player.duration`, `playableTime`, `naturalSize` للمعلومات.

### لماذا FFmpeg افتراضياً؟

قنوات IPTV غالباً MPEG-TS أو MKV مع AC3/E-AC3/DTS. مشغل AVPlayer لا يفك هذه الصيغ، لذلك نضع `KSMEPlayer` كأول محرك. يمكن تعطيل ذلك من **الإعدادات ← استخدام محرك FFmpeg** بعد إعادة تشغيل التطبيق.

### الترخيص

KSPlayer نسخة GPL-3.0 (النسخة المجانية) وFFmpegKit تحت تراخيصها. هذا المشروع يبقى **GPL-3.0** متوافقاً مع التطبيق الأصلي. راجع `LICENSE` في جذر المستودع و`THIRD_PARTY_NOTICES.md`.

> الاستخدام التجاري قد يتطلب ترخيصاً تجارياً من KSPlayer (LGPL) بحسب حالة مشروعك.

---

### التحقق من حزمة `.app`

قبل التوقيع يمكن فحص البناء محلياً:

```bash
python3 tools/verify_app.py /path/to/IPVIOStream.app --require-release
```

يفحص السكربت:
- `CFBundleIdentifier` و`MinimumOSVersion` و`CFBundleSupportedPlatforms`.
- أن الملف التنفيذي **Mach-O arm64** وليس محاكياً.
- وجود `KSPlayer.framework` و22 إطار FFmpeg داخلياً.
- خلو نسخة Release من `IPVIOStream.debug.dylib` و`__preview.dylib`.

هذا هو نفس الفحص الذي يشغّله CI قبل نشر الـ Artifact.

## اختبارات مقترحة بعد البناء

1. **Xtream**: استيراد حساب، التأكد من ظهور القنوات والأفلام والمسلسلات.
2. **M3U كبيرة**: استيراد قائمة > 50 ألف عنصر، قياس زمن الفتح والتصفح.
3. **حلقة مسلسل**: فتح مسلسل، تحميل الحلقات، تشغيل حلقة، تنقّل بين المواسم.
4. **متابعة**: إيقاف فيلم في المنتصف ثم إعادة فتحه من «متابعة المشاهدة».
5. **صوت/ترجمة**: تبديل مسار الصوت AC3 والترجمة أثناء التشغيل.
6. **فشل مصدر**: تشغيل رابط معطّل والتأكد من تجربة المصدر التالي أو رسالة الخطأ.
7. **مباشر**: ترك قناة تعمل، فصل الشبكة وإعادة وصلها، مراقبة إعادة الاتصال.
8. **RTL**: تبديل لغة التطبيق للعربية وإعادة فتح الواجهات للتأكد من الاتجاه.
9. **الأمان**: إعادة تشغيل التطبيق والتأكد من بقاء القوائم (فك تشفير Keychain).
10. **جودة**: ضبط «HD» والتأكد من اختيار مصدر HD عند توفر بدائل.

---

## استكشاف الأخطاء

### «Dependencies could not be resolved»
- تأكد من اتصال الإنترنت.
- Xcode 27 مطلوب لأن KSPlayer 5.6.2 يستخدم Swift tools 6.2.
- أعد فتح Xcode أو `File → Packages → Reset Package Caches`.

### «Building for tvOS Simulator, but the linked framework KSPlayer was built for tvOS»
- هذه هي الحالة المتوقعة. اختر **Apple TV** حقيقياً. لا توجد شريحة محاكي في حزمة KSPlayer المجانية.

### البث لا يبدأ مع HTTP
- `Info.plist` يسمح بـ ATS للوسائط. إذا كان المزوّد يستخدم منفذاً/شهادة غير قياسية، جرّب HTTPS أو تحقق من الحظر الجغرافي.
- بعض المزوّدين يرفضون أجهزة Apple TV أو يطلبون User-Agent معيناً؛ الافتراضي `VLC/3.0.21 LibVLC/3.0.21`.

### «Could not save state / keychain»
- قد يكون الجهاز مقفلاً عند أول تشغيل. افتح القفل وأعد المحاولة. يوجد احتياطي داخل صندوق التطبيق إذا فشل Keychain.

### استيراد بطيء
- القوائم الضخمة تحتاج وقتاً لتنزيل JSON وتحليله. شريط التقدم يوضح المرحلة. لا تغلق التطبيق أثناء الاستيراد.

### لا تظهر القنوات بعد إضافة قائمة
- اضغط **تحديث** في الإعدادات.
- افتح القائمة وتحقق من آخر خطأ أسفل اسمها.

### الشاشة سوداء والمشغل يعمل
- جرّب مصدر جودة آخر من قائمة المشغل.
- فعّل/عطّل محرك FFmpeg من الإعدادات وأعد التشغيل.
- بعض القنوات HLS تحتاج تجربة `HLS` بدلاً من `TS` تلقائياً عند فشل الأول.

---

## خطة النقل التالية (Roadmap)

1. **Stalker Portal** (MAG): handshake، profile، itv/vod/series، create_link.
2. **Catch-up / Timeshift**: قائمة أرشيف القناة وواجهة اختيار الوقت.
3. **الدليل الكامل**: XMLTV + شبكة زمنية أسبوعية.
4. **الملفات الشخصية + الرقابة الأبوية**: PIN وحجب التصنيفات.
5. **الرياضة**: مباريات اليوم/القادمة وربطها بالقنوات.
6. **التسجيل**: تسجيل البث إلى التخزين الداخلي/الخارجي (KSPlayer يدعم startRecord).
7. **استيراد من الهاتف**: خادم HTTP صغير داخل التطبيق + QR.
8. **النسخ الاحتياطي/الاستعادة** وGoogle Drive.
9. **تحديثات داخل التطبيق** من GitHub Releases.
10. **تغطية ترجمة كاملة** للغات العشر الأصلية.
11. **واجهة تفاصيل أغنى**: الإعلان، صفحات الممثلين، أعمال مشابهة.
12. **صورة تطبيق tvOS** (App Icon brand assets) وتجربة Top Shelf.

---

## الترخيص والاعتمادات

- IPVIO Stream: **GPL-3.0** (راجع `../LICENSE`).
- KSPlayer: GPL-3.0 — https://github.com/kingslay/KSPlayer
- FFmpegKit: راجع تراخيص FFmpeg — https://github.com/kingslay/FFmpegKit
- TMDB API: هذا المنتج يستخدم TMDB API لكنه غير معتمد أو مصادق عليه من TMDB.
- OMDb: تقييمات IMDb وRotten Tomatoes عبر OMDb API بمفتاح المستخدم.
- TheIntroDB: بيانات المقدمة/النهاية عبر `api.theintrodb.org` بدون مفتاح.

التطبيق لا يستضيف أو يوفّر أي محتوى. المستخدم مسؤول عن قانونية المصادر التي يضيفها.

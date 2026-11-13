# التوقيع والتثبيت على Apple TV

ناتج GitHub Actions هو **`.app` غير موقّع**. هذا يثبت أن المشروع يُبنى، لكن Apple TV يرفض تشغيل أي تطبيق غير موقّع من حساب مطوّر. هذا الملف يشرح الطريقتين.

---

## الطريقة 1: Xcode (الأسهل)

1. على macOS افتح:

   ```bash
   cd tvOS
   open IPVIOStream.xcodeproj
   ```

2. اختر Target **IPVIOStream** ← تبويب **Signing & Capabilities**.
3. فعّل **Automatically manage signing**.
4. اختر **Team** (حساب Apple Developer المدفوع).
5. غيّر **Bundle Identifier** إذا كان `com.ipvio.stream.tv` محجوزاً:
   `com.yourname.ipvio.tv`
6. وصّل Apple TV، وفعّل عليه:
   **Settings ← Privacy & Security ← Developer Mode**.
7. اختر الجهاز في Xcode واضغط **Run**.

سيقوم Xcode بإنشاء شهادة Development وProvisioning Profile تلقائياً.

> إذا ظهر أن tvOS 27 SDK غير مثبت: نزّله من Xcode ← Settings ← Platforms، أو غيّر `TVOS_DEPLOYMENT_TARGET` إلى 18.0 (الكود متوافق).

---

## الطريقة 2: سطر الأوامر / CI بتوقيع يدوي

تحتاج:

- شهادة **Apple Distribution** أو **Apple Development** بصيغة `.p12`.
- **Provisioning Profile** لتطبيق tvOS يحمل Bundle ID نفسه.
- `TEAM_ID` من Apple Developer.

### تجهيز الملفات

على Mac:

```bash
base64 -i Certificates.p12 | pbcopy          # TVOS_CERT_P12_BASE64
base64 -i profile.mobileprovision | pbcopy   # TVOS_PROFILE_BASE64
```

أضف في GitHub ← Settings ← Secrets and variables ← Actions:

| Secret | القيمة |
| --- | --- |
| `TVOS_CERT_P12_BASE64` | محتوى ملف الشهادة |
| `TVOS_CERT_PASSWORD` | كلمة مرور `.p12` |
| `TVOS_PROFILE_BASE64` | محتوى ملف الـ profile |
| `TVOS_TEAM_ID` | Team ID |
| `TVOS_BUNDLE_ID` | اختياري، افتراضياً `com.ipvio.stream.tv` |

### أوامر التوقيع داخل runner

```bash
KEYCHAIN=build.keychain
KEYCHAIN_PASS=actions

security create-keychain -p "$KEYCHAIN_PASS" "$KEYCHAIN"
security default-keychain -s "$KEYCHAIN"
security unlock-keychain -p "$KEYCHAIN_PASS" "$KEYCHAIN"

echo "$TVOS_CERT_P12_BASE64" | base64 -D > cert.p12
security import cert.p12 -k "$KEYCHAIN" -P "$TVOS_CERT_PASSWORD" \
  -T /usr/bin/codesign
security set-key-partition-list -S apple-tool:,apple: \
  -k "$KEYCHAIN_PASS" "$KEYCHAIN"

mkdir -p "$HOME/Library/MobileDevice/Provisioning Profiles"
echo "$TVOS_PROFILE_BASE64" | base64 -D > profile.mobileprovision
UUID=$(security cms -D -i profile.mobileprovision | plutil -extract UUID raw -)
cp profile.mobileprovision \
  "$HOME/Library/MobileDevice/Provisioning Profiles/$UUID.mobileprovision"

xcodebuild archive \
  -project IPVIOStream.xcodeproj \
  -scheme IPVIOStream \
  -configuration Release \
  -destination 'generic/platform=tvOS' \
  -archivePath "$RUNNER_TEMP/IPVIOStream.xcarchive" \
  DEVELOPMENT_TEAM="$TVOS_TEAM_ID" \
  PRODUCT_BUNDLE_IDENTIFIER="$TVOS_BUNDLE_ID" \
  CODE_SIGN_STYLE=Manual \
  CODE_SIGN_IDENTITY="Apple Distribution" \
  PROVISIONING_PROFILE_SPECIFIER="$UUID"
```

### تصدير IPA

`ExportOptions.plist`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key>
  <string>app-store</string>
  <key>teamID</key>
  <string>YOUR_TEAM_ID</string>
  <key>signingStyle</key>
  <string>manual</string>
  <key>provisioningProfiles</key>
  <dict>
    <key>com.ipvio.stream.tv</key>
    <string>PROFILE_UUID</string>
  </dict>
  <key>uploadSymbols</key>
  <true/>
</dict>
</plist>
```

```bash
xcodebuild -exportArchive \
  -archivePath "$RUNNER_TEMP/IPVIOStream.xcarchive" \
  -exportPath "$RUNNER_TEMP/export" \
  -exportOptionsPlist ExportOptions.plist \
  -allowProvisioningUpdates
```

الناتج `IPVIOStream.ipa` يمكن رفعه إلى **TestFlight/App Store** أو تثبيته بأدوات إدارة أجهزة Apple TV.

> بالنسبة لتطبيق tvOS على App Store، قد يلزم `method` = `app-store` ورفع عبر Transporter/`altool`. راجع متطلبات حسابك.

---

## لماذا لا يكفي ملف `.app` غير الموقّع؟

- tvOS يتحقق من توقيع التطبيق وProvisioning Profile قبل التثبيت.
- الشهادة الذاتية (self-signed) لا تُقبل.
- حساب Apple Developer المجاني له قيود زمنية/أجهزة؛ Apple TV غالباً يتطلب حساباً مدفوعاً.

---

## التحقق من البناء في CI

الـ Workflow:

https://github.com/GpGp700/IPVIO-Stream/actions/workflows/tvos-build.yml

عند نجاحه:

- Artifact: `IPVIOStream-tvOS-unsigned`
- Release: https://github.com/GpGp700/IPVIO-Stream/releases

اختبر داخل الـ Artifact:

- وجود `IPVIOStream.app/IPVIOStream` (Mach-O arm64).
- وجود `Info.plist` و`Assets.car`.
- وجود أطر KSPlayer/FFmpeg داخل الحزمة.

ثم وقّع النسخة بنفسك بالطريقة 1 أو 2.

---

## ملاحظات أمان

- لا ترفع `.p12` أو `.mobileprovision` إلى Git.
- استخدم GitHub Secrets فقط.
- بعد البناء، يمكن حذف الـ Keychain المؤقت في runner (ينتهي مع الجلسة).
- لا تشارك كلمة مرور الشهادة في logs.

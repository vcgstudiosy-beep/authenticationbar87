# Authentication Bar 87 🔐

تطبيق مصادقة ثنائية (2FA / TOTP-HOTP Authenticator) احترافي مبني بلغة **Java** وتخطيطات **XML**، متوافق من **Android 8.0 (API 26)** إلى **Android 15/16 (API 35)**.

---

## ✨ الميزات

- توليد رموز **TOTP** (RFC 6238) و **HOTP** (RFC 4226) بخوارزميات SHA1 / SHA256 / SHA512.
- **مسح رمز QR** مباشرة لإضافة الحسابات (يدعم روابط `otpauth://` القياسية المستخدمة من Google/Microsoft/GitHub وغيرها).
- إدخال يدوي للمفتاح السري (Base32) مع خيارات متقدمة (عدد الأرقام، مدة الصلاحية، الخوارزمية).
- تخزين الحسابات **مشفّرًا بالكامل على الجهاز** عبر `EncryptedSharedPreferences` (AES-256) — لا اتصال إنترنت إطلاقًا، ولا صلاحية Internet في البيان (Manifest).
- عرض شريط تقدم (Progress) لحساب الوقت المتبقي لكل رمز TOTP، وزر تحديث يدوي لحسابات HOTP.
- نسخ الرمز للحافظة بضغطة واحدة.
- عرض رمز QR الخاص بأي حساب لنقله لجهاز آخر.
- بحث فوري داخل قائمة الحسابات.
- تعديل وحذف الحسابات، مع رسالة تأكيد قبل الحذف.
- واجهة عربية بالكامل، بتصميم داكن أزرق (Material 3) مطابق لهوية التطبيق.
- دعم الألوان الديناميكية (Material You) على Android 12 فأعلى.

---

## 🗂️ هيكل المشروع

```
AuthenticationBar87/
├── .github/workflows/build.yml     # بناء APK تلقائيًا عبر GitHub Actions
├── app/
│   ├── build.gradle                 # إعدادات التطبيق (minSdk 26 → targetSdk 35)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/authbar87/authenticator/
│       │   ├── AuthApp.java
│       │   ├── MainActivity.java
│       │   ├── AddAccountActivity.java
│       │   ├── SettingsActivity.java
│       │   ├── model/Account.java
│       │   ├── adapter/AccountAdapter.java
│       │   └── util/
│       │       ├── Base32.java
│       │       ├── TotpGenerator.java
│       │       ├── OtpAuthUri.java
│       │       └── SecureStorage.java
│       └── res/                     # تخطيطات XML، ألوان، سلاسل نصية، أيقونات متجهة
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🛠️ البناء محليًا

المتطلبات: **Android Studio (Koala أو أحدث)** أو JDK 17 + Android SDK مثبت يدويًا.

1. استنسخ المستودع:
   ```bash
   git clone https://github.com/USERNAME/AuthenticationBar87.git
   cd AuthenticationBar87
   ```
2. افتح المشروع في Android Studio وانتظر مزامنة Gradle، **أو** ابنِ من الطرفية:
   ```bash
   gradle wrapper --gradle-version 8.9   # ينشئ gradlew لمرة واحدة فقط
   ./gradlew assembleDebug
   ```
   ملف الـ APK الناتج يكون في:
   `app/build/outputs/apk/debug/app-debug.apk`

> **ملاحظة:** هذا المستودع لا يتضمن ملف `gradle-wrapper.jar` الثنائي (لأنه لا يمكن توليده في بيئة إنشاء هذا المشروع). نفّذ الأمر `gradle wrapper` مرة واحدة بعد الاستنساخ، أو استخدم Android Studio الذي يولّده تلقائيًا عند الفتح.

---

## 🤖 البناء التلقائي عبر GitHub Actions

يقوم ملف `.github/workflows/build.yml` تلقائيًا بما يلي عند كل `push` إلى الفرع `main` أو عند فتح Pull Request:

1. تجهيز JDK 17 و Android SDK.
2. بناء نسخة **Debug APK** و **Release APK (غير موقّعة)**.
3. رفعهما كـ **Artifacts** يمكن تحميلها من تبويب **Actions** في المستودع.
4. عند دفع وسم إصدار مثل `v1.0.0`، يتم إنشاء **GitHub Release** تلقائيًا مرفقًا به ملفات الـ APK.

### لإصدار نسخة موقّعة (Release موقّع للنشر على المتجر)

أضف الأسرار التالية في **Settings → Secrets and variables → Actions** بالمستودع، ثم عدّل `app/build.gradle` لإضافة `signingConfig` يقرأ منها:

- `KEYSTORE_BASE64` (ملف keystore مُرمّز Base64)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

---

## 🔒 ملاحظات أمنية

- التطبيق **لا يطلب صلاحية الإنترنت إطلاقًا** — كل المفاتيح السرية تبقى على الجهاز فقط.
- المفاتيح مخزّنة عبر `EncryptedSharedPreferences` باستخدام `AES256_GCM` / `AES256_SIV`.
- ميزة "تصدير الحسابات" تضع روابط `otpauth://` (تتضمن المفتاح السري بشكل واضح) في الحافظة مؤقتًا — يُنصح باستخدامها فقط على جهاز موثوق ومسح الحافظة بعد الاستخدام.
- يُفضّل تفعيل "قفل التطبيق" من الإعدادات لحماية إضافية.

---

## 📋 الرخصة

هذا المشروع مُقدَّم كقالب برمجي مفتوح للتخصيص والاستخدام الحر ضمن مشاريعك.

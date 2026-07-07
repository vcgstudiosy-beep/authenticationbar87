# Keep model classes (used for serialization)
-keep class com.authbar87.authenticator.model.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# ZXing n'utilise pas de réflexion sur ces classes, mais on garde les writers
# référencés dynamiquement par MultiFormatWriter.
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Google Play Services Pay
-keep class com.google.android.gms.pay.** { *; }

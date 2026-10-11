# kotlinx.serialization keeps its generated serializers on the companion.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.giwu.bible.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.giwu.bible.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Tink (behind EncryptedSharedPreferences) is annotated with Error Prone
# annotations that are compile-time only and never shipped.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# OkHttp ships optional platform integrations it only uses when present.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

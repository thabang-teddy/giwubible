# kotlinx.serialization keeps its generated serializers on the companion.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.giwu.bible.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.giwu.bible.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp ships optional platform integrations it only uses when present.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

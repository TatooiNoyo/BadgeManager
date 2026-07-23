# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson / reflection models
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
  @com.google.gson.annotations.Expose <fields>;
}
-keep class io.github.tatooinoyo.star.badge.data.** { *; }
-keep class io.github.tatooinoyo.star.badge.utils.export.BadgeShareEnvelope { *; }
-keep class io.github.tatooinoyo.star.badge.utils.update.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# BouncyCastle (LAN sync crypto)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Compose / ViewModel
-dontwarn androidx.compose.**
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Enums used across serialization / channels
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

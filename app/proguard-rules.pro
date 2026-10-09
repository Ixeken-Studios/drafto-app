# ProGuard / R8 rules for Drafto
# By default, flags in this file are appended to flags specified
# in getDefaultProguardFile('proguard-android-optimize.txt').

# ==============================================================================
# 1. Kotlin & Coroutines
# ==============================================================================
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-dontwarn java.lang.invoke.**
-dontwarn kotlinx.coroutines.**

# ==============================================================================
# 2. Kotlinx Serialization
# ==============================================================================
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$$serializer {
    *;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# Preserve pure domain models, backups, and serializable navigation routes
-keep class com.ixeken.drafto.domain.model.** { *; }
-keep class com.ixeken.drafto.navigation.NavRoutes** { *; }
-keepclassmembers class com.ixeken.drafto.util.GitHubUpdateChecker$GitHubReleaseDto { *; }

# ==============================================================================
# 3. Room Database & SQLite
# ==============================================================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomOpenHelper$Delegate { *; }
-keep class **_Impl { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}
-keep @androidx.room.Entity class * {
    <fields>;
    <init>(...);
}
-keep @androidx.room.Dao interface * { *; }
-keep class com.ixeken.drafto.data.local.entity.** { *; }
-keep class com.ixeken.drafto.data.local.converter.** { *; }

# ==============================================================================
# 4. WorkManager & Background Tasks
# ==============================================================================
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ==============================================================================
# 5. Haze Blur & Rendering
# ==============================================================================
-keep class dev.chrisbanes.haze.** { *; }

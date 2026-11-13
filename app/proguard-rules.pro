# Add project specific ProGuard rules here.
# Keep source line numbers for readable crash reports without exposing local paths
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson serialization models (Room entities & domain models)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.dur1al.controlate.porteria.data.local.entities.** { *; }
-keep class com.dur1al.controlate.porteria.domain.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.debug.**

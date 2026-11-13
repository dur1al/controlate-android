# Add project specific ProGuard rules here.
# Keep source line numbers for readable crash reports without exposing local paths
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson: conservar la firma genérica de TypeToken y sus subclases anónimas (requerido con R8
# en modo completo; Gson 2.10 no incluye estas reglas).
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

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

# Tink (transitiva de androidx.security:security-crypto) referencia anotaciones de solo
# compilación (JSR-305) que no están en el classpath de runtime.
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy

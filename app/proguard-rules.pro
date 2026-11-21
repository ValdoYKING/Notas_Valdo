# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== REGLAS PARA ROOM =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ===== REGLAS PARA KOTLIN COROUTINES =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== REGLAS PARA COMPOSE =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ===== REGLAS PARA DATASTORE =====
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ===== REGLAS PARA COIL (carga de imágenes) =====
-dontwarn coil.**
-keep class coil.** { *; }

# ===== REGLAS PARA CANHUB CROPPER =====
-keep class com.canhub.cropper.** { *; }
-dontwarn com.canhub.cropper.**

# ===== REGLAS PARA RICHTEXT Y COMMONMARK (MARKDOWN) =====
-keep class com.halilibo.richtext.** { *; }
-dontwarn com.halilibo.richtext.**
-keep class org.commonmark.** { *; }
-dontwarn org.commonmark.**

# ===== REGLAS PARA BIOMETRIC AUTH =====
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# ===== MANTENER ATRIBUTOS PARA DEBUGGING =====
-keepattributes *Annotation*, InnerClasses
-keepattributes Signature, Exception

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Hilt
-keep class * { @dagger.hilt.android.internal.lifecycle.HiltViewModelMap *; }
-keep class * { @dagger.hilt.internal.aggregatedroot.AggregatedRoot *; }
-keep class * { @dagger.hilt.android.HiltAndroidApp *; }
-keep class * { @dagger.hilt.android.AndroidEntryPoint *; }
-keep class * extends androidx.lifecycle.ViewModel

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-keep class * { @androidx.room.Entity *; }
-keep class * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Database *; }
-keep class * { @androidx.room.TypeConverter *; }

# Kotlin Serialization
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class **$serializer {
    public static final **$serializer INSTANCE;
}
-keep class kotlinx.serialization.json.** { *; }

# Compose
-keepclassmembers class  ** {
    @androidx.compose.runtime.Composable *;
}

# Coil
-keep class coil.** { *; }

# Retrofit
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Billing Library
-keep class com.android.billingclient.** { *; }

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified in AIDL files
# and those generated from the dependency rules.

# You can add more rules here if you want to configure ProGuard for your project.

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name of your Application class:
#-keepclassmembers class fqcn.of.your.app.Application {
#    <methods>;
#}

-keepattributes InnerClasses

# For Room Persistence Library
-keepnames class * extends androidx.room.RoomDatabase
-keep public class * extends androidx.room.Dao
-keep class * implements androidx.room.migration.Migration
-keepfields class androidx.room.Metadata {
    java.lang.String DATABASE_NAME;
    int DATABASE_VERSION;
}

# For TensorFlow Lite
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }

# For Kotlin
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class kotlin.Metadata { *; }

# For Jetpack Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keep class * extends androidx.compose.ui.tooling.preview.PreviewParameterProvider { *; }

# For Nordic BLE Library (adjust if specific classes are causing issues)
-dontwarn no.nordicsemi.android.ble.**
-keep class no.nordicsemi.android.ble.** { *; }

# For ARCore and Sceneform
-keep class com.google.ar.core.** { *; }
-keep class com.google.ar.sceneform.** { *; }
-keep class com.google.ar.sceneform.ux.** { *; }
-keep class com.google.ar.sceneform.rendering.** { *; }

# Keep all classes that are annotated with @Keep
-keep @androidx.annotation.Keep class * {
    <fields>;
    <methods>;
}

# Suppress warnings for missing or unused classes/members that are part of the Android SDK
-dontwarn android.support.**
-dontwarn android.hardware.camera.**
-dontwarn android.opengl.**
-dontwarn android.bluetooth.**

# Keep all public methods of classes that extend Activity, Fragment, Service, etc.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.lifecycle.ViewModel

# Keep setters and getters of classes that are accessed by data binding
-keepclassmembers class * {
    @androidx.databinding.Bindable <methods>;
}

# Keep names of enum values
-keepclassmembers enum * {
    <fields>;
}

# Keep the R and BuildConfig classes
-keep class **.R$*
-keep class **.BuildConfig {
    static final boolean DEBUG;
}
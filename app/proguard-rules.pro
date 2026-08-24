# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Administrator\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and targets by changing the proguardFiles
# directive in build.gradle.

# Keep all Android components (Activities, Services, Receivers, Providers)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.work.Worker
-keep public class * extends androidx.work.ListenableWorker

# Keep Compose UI entry points
-keep class com.momin.japanesestudyappn5.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.momin.japanesestudyappn5.**$$serializer { *; }
-keepclassmembers class com.momin.japanesestudyappn5.** {
    *** Companion;
}
-keepclasseswithmembers class com.momin.japanesestudyappn5.** {
    kotlinx.serialization.KSerializer serializer(...);
}

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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Suppress warnings for missing optional dependencies required by PDFBox
-dontwarn com.gemalto.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.pdfbox.**
-dontwarn com.tom_roush.pdfbox.**

# Keep data entities for Database/Room
-keep class com.mybudget.data.local.entity.** { *; }

# Room creates generated *_Impl classes via reflection in release builds.
# Keep those implementations and their constructors so minification doesn't
# remove the no-arg constructor Room expects.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.mybudget.data.local.**_Impl { <init>(...); *; }
-keep class * extends androidx.room.RoomDatabase_Impl { *; }

# Keep domain models and use cases
-keep class com.mybudget.domain.** { *; }

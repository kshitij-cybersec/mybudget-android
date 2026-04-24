# ProGuard rules for Mybudget

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Suppress warnings for missing optional dependencies required by PDFBox
-dontwarn com.gemalto.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.pdfbox.**
-dontwarn com.tom_roush.pdfbox.**

# Keep data entities for Database/Room
-keep class com.mybudget.data.local.entity.** { *; }

# Room creates generated *_Impl classes via reflection in release builds.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.mybudget.data.local.**_Impl { <init>(...); *; }
-keep class * extends androidx.room.RoomDatabase_Impl { *; }

# Keep domain models and use cases
-keep class com.mybudget.domain.** { *; }


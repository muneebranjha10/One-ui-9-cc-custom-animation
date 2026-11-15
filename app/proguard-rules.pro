# Keep LSPosed / Xposed module entry point and callbacks
-keep class com.example.xposed.** { *; }
-keepclassmembers class com.example.xposed.** { *; }
-keep class com.example.motion.** { *; }
-dontwarn de.robv.android.xposed.**

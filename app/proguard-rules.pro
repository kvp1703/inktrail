# Keep ML Kit text recognition classes (release build has minification disabled,
# but these rules are harmless and useful if minification is enabled later).
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

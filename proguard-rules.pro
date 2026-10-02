-keep class com.overlay.codm.**   { *; }
-keep class rikka.shizuku.**      { *; }
-keep class rikka.system.**       { *; }

-keepattributes *Annotation*
-keepattributes Signature

-keep class kotlinx.coroutines.android.** { *; }

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

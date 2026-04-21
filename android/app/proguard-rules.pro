# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.smartwatch.monitor.model.** { *; }
-keep class com.amap.api.** { *; }
-dontwarn com.amap.api.**

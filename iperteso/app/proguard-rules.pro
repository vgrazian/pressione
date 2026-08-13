# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Supabase
-keep class io.github.jan_tennert.supabase.** { *; }
-keep class kotlinx.serialization.** { *; }

# Room
-keep class com.pressione.iperteso.data.local.entity.** { *; }

# Koin
-keep class org.koin.** { *; }

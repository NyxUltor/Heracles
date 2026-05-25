# Add rules here as needed. Keep entry points used via reflection and
# serialization to avoid removal. These are conservative examples.
-keepattributes *
-keepclassmembers class ** {
    @androidx.annotation.Keep *;
}

# Keep data classes used by kotlinx.serialization
-keep class kotlinx.serialization.** { *; }

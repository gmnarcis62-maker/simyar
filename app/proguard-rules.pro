# Rules for SimYar (RedLine Soft Alborz)
# Industrial Electrical CAD and PLC Ladder Simulator
# Package: red.line.bargh / com.example

# Keep Kotlinx Serialization classes and members
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room Database, Entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.SharedSQLiteStatement { *; }
-dontwarn androidx.room.paging.**

# Keep Application Models, Simulation Engine & Data
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }
-keep class com.example.simulation.** { *; }
-keep class com.example.billing.** { *; }
-keep class com.example.util.** { *; }
-keep class red.line.bargh.** { *; }

# Keep Jetpack Compose runtime
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Myket AIDL InAppBillingService
-keep class com.android.vending.billing.** { *; }

# Optimize and remove unused verbose logs in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

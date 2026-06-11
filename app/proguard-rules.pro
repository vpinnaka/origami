# Keep MediaPipe / LiteRT classes
-keep class com.google.mediapipe.** { *; }
-keep class com.google.ai.edge.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.ai.edge.**

# Keep Moshi adapters
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface * { @retrofit2.http.* <methods>; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Keep Gemma model-related native libs
-keepclasseswithmembernames class * {
    native <methods>;
}

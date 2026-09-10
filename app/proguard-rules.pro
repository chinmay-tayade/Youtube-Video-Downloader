# ---------------------------------------------------------------------------
# Chaquopy / Python bridge
# ---------------------------------------------------------------------------
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# The Python code calls these back by name – keep them and their members.
-keep class com.chinmay.tayade.mp3downloader.data.ProgressSink { *; }
-keep class * implements com.chinmay.tayade.mp3downloader.data.ProgressSink { *; }
-keep class com.chinmay.tayade.mp3downloader.data.PythonDownloader { *; }

# Data classes crossing the JSON / Python boundary.
-keep class com.chinmay.tayade.mp3downloader.data.model.** { *; }
-keep class com.chinmay.tayade.mp3downloader.data.DownloadResult { *; }

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Kotlin coroutines / misc
# ---------------------------------------------------------------------------
-dontwarn org.jetbrains.annotations.**
-keepclassmembers class kotlin.Metadata { *; }

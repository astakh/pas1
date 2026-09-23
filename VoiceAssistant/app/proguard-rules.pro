# Porcupine / Vosk используют JNI — сохраняем классы обёрток
-keep class ai.picovoice.** { *; }
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**

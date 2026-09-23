СЮДА НУЖНО ПОЛОЖИТЬ ФАЙЛЫ (приложение без них не заработает):

1) app/src/main/assets/vosk/vosk-model-small-ru-0.22/
   Скачайте и РАСПАКУЙТЕ архив модели:
   https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip  (~46 МБ, быстрая)
   или качественную vosk-model-ru-0.42.zip (~1,5 ГБ — тогда поменяйте
   имя каталога в Config.VOSK_MODEL_ASSET_DIR).
   ИТОГ: должен получиться путь
   app/src/main/assets/vosk/vosk-model-small-ru-0.22/am/final.mdl и т.д.
   (имя внутренней папки из архива должно совпадать со значением
   Config.VOSK_MODEL_ASSET_DIR — сейчас "vosk-model-small-ru-0.22")

2) app/src/main/assets/vnimanie_russian.ppn
   Кастомное кодовое слово для Porcupine. Сгенерируйте на
   https://console.picovoice.ai  (Porcupine → Training Keyword,
   транслит: "vnimanie", язык Russian).
   ЛИБО НЕ НУЖНО: закомментируйте CustomKeyword в Config.kt и раскомментируйте
   строку с BuiltInKeyword JARVIS — тогда .ppn не требуется вообще.

После добавления файлов: git add -A && ./gradlew assembleDebug (или Build APK в Android Studio).

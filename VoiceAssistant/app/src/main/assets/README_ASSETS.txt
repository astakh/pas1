СЮДА НУЖНО ПОЛОЖИТЬ ФАЙЛЫ (приложение без них не заработает):

1) app/src/main/assets/vosk/vosk-model-ru-0.42/
   Распакуйте архив модели:
   https://alphacephei.com/vosk/models/vosk-model-ru-0.42.zip
   (или маленькую быструю vosk-model-small-ru-0.22 — тогда поменяйте
    имя каталога в Config.VOSK_MODEL_ASSET_DIR)

2) app/src/main/assets/vnimanie_russian.ppn
   Кастомное кодовое слово для Porcupine. Сгенерируйте на
   https://console.picovoice.ai  (Porcupine → Training Keyword,
   транслит: "vnimanie" или "dva raza", язык Russian).
   Либо закомментируйте CustomKeyword в Config.kt и используйте
   встроенное JARVIS — тогда .ppn не нужен.

# Voice Assistant (MVP, полностью офлайн)

Голосовой ассистент для Android: фоновое прослушивание микрофона → активация
кодовым словом → распознавание команды (STT) → озвучивание ответа (TTS).
Всё на устройстве, без интернета. **Никаких ключей и регистраций не требуется.**

## Стек
| Компонент | Библиотека | Версия |
|---|---|---|
| Язык/UI | Kotlin + Jetpack Compose | 1.9.24 / BOM 2024.06 |
| Wake word + STT | Vosk (одна модель на оба) | com.alphacephei:vosk-android:0.3.47 |
| TTS | системный android.speech.tts | — |
| Фон | foreground-сервис (тип microphone) | — |

Кодовое слово детектируется потоковым распознаванием Vosk: список слов
настраивается в `Config.WAKE_KEYWORDS` (по умолчанию «джарвис/jarvis/computer»).

## Что нужно перед сборкой
Только модель Vosk (~46 МБ): распакуйте `vosk-model-small-ru-0.22.zip`
(https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip) в
`app/src/main/assets/vosk/`. Подробности и пошаговая сборка — в ../SETUP_AND_BUILD.md.

## Структура
```
app/src/main/java/si/unisa/sss/pocketvoiceassistant/
├── Config.kt                  # кодовые слова, модель, таймауты
├── MainActivity.kt            # разрешения + запуск сервиса + UI
├── wakeword/WakeWordDetector.kt  # цикл микрофон+Vosk, поиск кодового слова
├── stt/VoskStt.kt             # запись одной фразы, распознавание по тишине
├── tts/TtsEngine.kt           # ru-RT озвучка, ожидание конца речи
├── service/AssistantService.kt   # конвейер IDLE→LISTENING→SPEAKING
├── state/AssistantState.kt    # общий StateFlow статуса
└── ui/AssistantScreen.kt      # экран со статусом
```

## Сборка
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Ограничения MVP
- Wake через STT может срабатывать на фоновую речь — берите редкое слово.
- Ответ — эхо-заглушка: проверяется конвейер, а не логика ассистента.

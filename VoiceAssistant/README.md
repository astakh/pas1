# Голосовой ассистент (Android, полностью офлайн)

Конвейер: **микрофон в фоне → кодовое слово (Porcupine) → распознавание команды (Vosk STT) → озвучка ответа (системный TTS)**. Всё выполняется на устройстве, интернет приложению не нужен (и не запрошен в манифесте).

## Стек
| Часть | Библиотека | Версия |
|---|---|---|
| Язык / UI | Kotlin 1.9.24, Jetpack Compose (Material3) | BOM 2024.06 |
| Wake word | Picovoice Porcupine (`ai.picovoice:porcupine-android`) | 3.0.0 |
| STT | Vosk (`com.alphacephei:vosk-android`, пакет `org.vosk.*`) | 0.3.47 |
| TTS | Android `TextToSpeech` (системный, ru-локаль) | — |
| Фон | Foreground Service (`foregroundServiceType="microphone"`) | AGP 8.5.2, Gradle 8.7, compileSdk 34, minSdk 24 |

Версии зависимостей проверены против Maven Central (24.09.2026).

## Архитектура
```
MainActivity (Compose UI, запрос разрешений)
   └─ AssistantService (LifecycleService, foreground)
        ├─ wakeword/WakeWordDetector.kt  — PorcupineManager, слушает 24/7
        ├─ stt/VoskStt.kt                — AudioRecord+Recognizer, одна фраза по пробуждению
        ├─ tts/TtsEngine.kt              — speakAndWait (пока говорит — микрофон выключен)
        └─ state/AssistantState.kt       — StateFlow для отображения фазы в UI
```
Логика конвейера в `AssistantService.onKeywordHeard()`:
wake → `pause()` детектора (освобождает микрофон) → `listenOnce()` (Vosk, до 10 с или до тишины) → заглушка-ответ «Вы сказали: …» → TTS → `resume()` детектора.

## Что нужно подготовить перед сборкой (5–10 минут)

### 1. AccessKey Picovoice (обязательно)
1. Зарегистрируйтесь бесплатно на https://console.picovoice.ai
2. Скопируйте **AccessKey** (Porcupine → AccessKey).
3. Вставьте его в `app/src/main/java/.../Config.kt` → `PICOVOICE_ACCESS_KEY`.
   ⚠️ Ключ привязан к вашему аккаунту — **не коммитьте его в публичный репозиторий**.

### 2. Кодовое слово
По умолчанию настроено русское кастомное слово **«Внимание»**:
- На https://console.picovoice.ai → Porcupine → *Training Keyword*: язык **Russian**, текст транслитом `vnimanie` (2–4 слога), скачайте `.ppn` и положите в `app/src/main/assets/vnimanie_russian.ppn`.

Быстрый вариант для проверки без генерации — встроенное слово **JARVIS**: в `Config.kt` закомментируйте блок `CustomKeyword` и раскомментируйте строку с `BuiltInKeyword.JARVIS` (файл `.ppn` тогда не нужен).

### 3. Модель Vosk (русская, офлайн)
Скачайте и **распакуйте** в `app/src/main/assets/vosk/`:
- маленькая для тестов (~46 МБ, по умолчанию настроена именно она): https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip
- полная (~1,8 ГБ, точнее): https://alphacephei.com/vosk/models/vosk-model-ru-0.42.zip — тогда поменяйте в `Config.kt`: `VOSK_MODEL_ASSET_DIR = "vosk-model-ru-0.42"`.

Каталог должен выглядеть так: `assets/vosk/vosk-model-small-ru-0.22/{am,conf,graph,ivector,...}`.

## Сборка APK

### Вариант А: Android Studio (проще всего)
1. Откройте папку `VoiceAssistant/` (File → Open).
2. Дождитесь синхронизации Gradle (Studio сама скачает Gradle 8.7 и SDK 34).
3. Build → Build App Bundle(s)/APK(s) → Build APK(s).
4. APK: `app/build/outputs/apk/debug/app-debug.apk`. Установка: `adb install app-debug.apk`.

### Вариант Б: командная строка
Нужны JDK 17 и Android SDK (platform-tools, platforms;android-34, build-tools;34.0.0).

```bash
export ANDROID_HOME=/path/to/Android/Sdk      # или создайте local.properties: sdk.dir=...
cd VoiceAssistant

# если gradlew нет/не работает — используйте установленный Gradle >= 8.5:
gradle wrapper --gradle-version 8.7           # один раз, создаст gradlew + jar
./gradlew assembleDebug
# результат: app/build/outputs/apk/debug/app-debug.apk
```

Подпись релизного APK:
```bash
keytool -genkey -v -keystore release.jks -alias asst -keyalg RSA -keysize 2048 -validity 10000
./gradlew assembleRelease
jarsigner -keystore release.jks app/build/outputs/apk/release/app-release-unsigned.apk asst
# (AGP 8: лучше добавить signingConfig в build.gradle.kts и собрать сразу подписанный)
```

## Первый запуск / проверка работоспособности
1. Разрешите приложению **микрофон** и **уведомления**.
2. Нажмите «Запустить прослушивание» — в уведомлении и на экране будет фаза: сначала «Загрузка модели распознавания…» (первый запуск копирует модель из assets — 1–3 мин для small, дольше для полной), затем «Слушаю кодовое слово…».
3. Скажите кодовое слово → «Слышу команду…» → произнесите фразу → приложение озвучит «Вы сказали: …».
4. Проверка офлайна: включите авиарежим — всё должно работать (кроме загрузки чего-либо — приложение ничего не качает).

## Известные ограничения / что дальше
- Ответ сейчас — эхо-заглушка; реальную логику ассистента добавляйте в `AssistantService.onKeywordHeard()` вместо строки `val reply = ...`.
- Porcupine требует отдельный захват микрофона, поэтому на время STT детектор ставится на паузу (реализовано через pause/resume).
- Эмуляторы x86 32-bit не поддерживаются (в build.gradle.kts оставлены armeabi-v7a, arm64-v8a, x86_64) — тестируйте на реальном телефоне или x86_64-образе.
- На некоторых OEM-прошивках для вечной фоновой работы отключите оптимизацию батареи для приложения (Настройки → Приложения → игнорировать оптимизацию батареи).
- Русские встроенные слова у Porcupine отсутствуют — только кастомные .ppn (см. шаг 2).

## Структура проекта
```
VoiceAssistant/
├── settings.gradle.kts / build.gradle.kts
├── gradle/wrapper/gradle-wrapper.properties   (Gradle 8.7)
└── app/
    ├── build.gradle.kts                       (зависимости, ABI, noCompress)
    └── src/main/
        ├── AndroidManifest.xml                (разрешения, service)
        ├── assets/README_ASSETS.txt           (куда класть модель и .ppn)
        ├── java/si/unisa/sss/pocketvoiceassistant/
        │   ├── Config.kt                      ← сюда вставляете AccessKey и настройки
        │   ├── MainActivity.kt
        │   ├── wakeword/WakeWordDetector.kt
        │   ├── stt/VoskStt.kt
        │   ├── tts/TtsEngine.kt
        │   ├── service/AssistantService.kt
        │   ├── state/AssistantState.kt
        │   └── ui/AssistantScreen.kt
        └── res/values/{strings.xml,themes.xml}
```

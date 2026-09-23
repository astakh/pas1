# Настройка и пересборка APK (Voice Assistant)

Инструкция для проекта `VoiceAssistant/`. Весь процесс — ~15 минут.
Нужен компьютер с **Android Studio** (или JDK 17 + Android SDK для сборки из CLI).

---

## Шаг 0. Получить AccessKey Picovoice (2 мин)

1. Зарегистрируйтесь бесплатно на https://console.picovoice.ai
2. На главной консоли скопируйте **AccessKey** (длинная строка букв/цифр).
3. Откройте файл:
   `VoiceAssistant/app/src/main/java/si/unisa/sss/pocketvoiceassistant/Config.kt`
4. Замените заглушку:
   ```kotlin
   const val PICOVOICE_ACCESS_KEY: String = "ВСТАВЬТЕ_ВАШ_ACCESSKEY"
   // было  →  стало, например:
   const val PICOVOICE_ACCESS_KEY: String = "ab12cd34ef56..."
   ```

⚠️ Не коммитьте реальный ключ в публичный репозиторий!

---

## Шаг 1. Скачать модель Vosk (~5 мин, ~46 МБ)

1. Скачайте русскую модель:
   https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip
2. Распакуйте архив — внутри папка `vosk-model-small-ru-0.22`.
3. Переместите ЭТУ ПАПКУ в:
   `VoiceAssistant/app/src/main/assets/vosk/`
   (папку `vosk` создайте, если её нет)

Итог — должен существовать путь:
```
app/src/main/assets/vosk/vosk-model-small-ru-0.22/am/final.mdl
```
Имя каталога обязано совпадать со значением `VOSK_MODEL_ASSET_DIR` в `Config.kt`
(сейчас там `vosk-model-small-ru-0.22` — всё сходится).

---

## Шаг 2. Кодовое слово — выберите ОДИН вариант

### Вариант А — «JARVIS» (проще всех, .ppn НЕ нужен)

В `Config.kt` поменяйте блок `WAKE_KEYWORD` местами:
```kotlin
// ЗАКОММЕНТИРУЙТЕ этот блок:
// val WAKE_KEYWORD: WakeWord = WakeWord.CustomKeyword(
//     label = "внимание",
//     ppnAssetPath = "vnimanie_russian.ppn"
// )

// РАСКОММЕНТИРУЙТЕ эту строку:
val WAKE_KEYWORD: WakeWord = WakeWord.BuiltInKeyword(Porcupine.BuiltInKeyword.JARVIS)
```
Не забудьте добавить импорт в начало файла (если IDE не предложит сам):
```kotlin
import ai.picovoice.porcupine.Porcupine
```
Переходите сразу к Шагу 3.

### Вариант Б — русское «Внимание» (нужен .ppn файл)

1. В той же консоли https://console.picovoice.ai откройте
   **Porcupine → Training Keyword**.
2. Language: **Russian**, Keyword (транслит): `vnimanie`, желаемое произношение — по подсказкам.
3. Нажмите **Train** → дождитесь готовности → **Download** — получите архив
   с файлом вида `vnimanie_russian.ppn`.
4. Положите файл `.ppn` прямо в `VoiceAssistant/app/src/main/assets/`
   (рядом с папкой `vosk`). Имя файла должно совпадать с `ppnAssetPath`
   в `Config.kt` — сейчас это `vnimanie_russian.ppn`.

---

## Шаг 3. Пересборка APK

### Способ 1 — Android Studio (рекомендуется)
1. File → Open → выберите папку `VoiceAssistant`.
2. Дождитесь окончания Gradle Sync (первый раз 2–5 мин, качаются зависимости).
3. Menu → **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. По завершении нажмите **locate** во всплывающем уведомлении.
   Готовый файл: `VoiceAssistant/app/build/outputs/apk/debug/app-debug.apk`

### Способ 2 — командная строка
```bash
cd VoiceAssistant
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```
APK появится там же: `app/build/outputs/apk/debug/app-debug.apk`.

Требования для CLI: установленный JDK 17 и Android SDK; путь к SDK пропишите
в файле `local.properties`: `sdk.dir=/путь/к/Android/Sdk`.

---

## Шаг 4. Установка на телефон

Через ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Или просто скопируйте APK на телефон и откройте его
(Настройки → разрешить установку из неизвестных источников).

---

## Шаг 5. Проверка работоспособности

1. Откройте приложение → разрешите **микрофон** и **уведомления**
   (для фоновой работы нужен foreground-сервис).
2. Нажмите кнопку запуска ассистента. Первый запуск копирует модель Vosk
   на устройство — **1–3 минуты**, подождите появления статуса «Слушаю».
3. Скажите кодовое слово («Jarvis» или «Внимание»).
4. После сигнала скажите команду, напр.: «привет как дела».
5. Текст распознается и будет озвучен через TTS (проверьте, что в системных
   настройках Android установлен русский голос TTS:
   Настройки → Язык и ввод → Синтез речи).

---

## Частые проблемы

| Симптом | Причина / решение |
|---|---|
| Ошибка при сборке `SDK location not found` | Создайте/поправьте `local.properties` (`sdk.dir=...`) или откройте проект в Android Studio |
| Приложение падает при старте сервиса | Не вставлен AccessKey / неверное имя `.ppn` / модель Vosk лежит не по пути из Шага 1 |
| Не слышит кодовое слово | Понизьте `WAKE_SENSITIVITY` (0.5–0.7); проверьте разрешение на микрофон; говорите в тишине |
| TTS молчит или говорит по-английски | Установите русский голос в системном TextToSpeech (Google TTS → языки → Русский) |
| Обрывается распознавание длинной фразы | Увеличьте `COMMAND_TIMEOUT_MS` в `Config.kt` |
| Батарея убивает фоновый сервис | В настройках батареи телефона разрешите приложению фоновую работу / «не оптимизировать» |
| APK >100 МБ после добавления большой модели ru-0.42 | Используйте small-модель или AAB + Play Console |

---

## Итого: где что менять

- Ключ и настройки — `app/src/main/java/.../Config.kt`
- Модель — `app/src/main/assets/vosk/vosk-model-small-ru-0.22/`
- .ppn (для варианта Б) — `app/src/main/assets/vnimanie_russian.ppn`
- Готовый APK — `app/build/outputs/apk/debug/app-debug.apk`

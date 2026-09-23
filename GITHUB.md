# Как загрузить проект на GitHub

## Вариант A — через сайт (без консоли)
1. Зайдите на https://github.com/new
2. Имя репозитория: `voice-assistant`, видимость: Private (чтобы не светить ключи), **Create repository** (галочку README НЕ ставить).
3. На странице репозитория нажмите **"uploading files"** → перетащите все файлы из папки `VoiceAssistant/`
   (можно целыми папками; `app/src/main/java` и вложенные пути GitHub создаст сам).
   Файл `app/build/outputs/apk/debug/app-debug.apk` (~36 МБ) можно тоже перетащить — GitHub пропускает до 25 МБ на файл в web UI, поэтому APK лучше залить через Releases (см. вариант B, шаг 5).

## Вариант B — через git (рекомендуется)
```bash
# 1. Создайте пустой репозиторий на https://github.com/new (без README)

# 2. В папке проекта:
cd VoiceAssistant
git init -b main
git add .
git commit -m "Voice assistant MVP: Porcupine + Vosk + TTS"

# 3. Привяжите удалённый репозиторий и отправьте:
git remote add origin https://github.com/<ВАШ_ЛОГИН>/voice-assistant.git
git push -u origin main
```

### Аутентификация
GitHub **не принимает пароли** по HTTPS. Нужен Personal Access Token:
- GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
- Скопировать scopes: только `repo` → Generate token
- При запросе логина/пароля при push: логин = ваш username, пароль = **токен**

Или используйте SSH: `git remote set-url origin git@github.com:<логин>/voice-assistant.git`
(предварительно добавив публичный ключ в Settings → SSH and GPG keys).

## 4. Проверка
Откройте репозиторий в браузере — должны быть видны: `app/`, `gradle/`, `build.gradle.kts`,
`settings.gradle.kts`, `gradle.properties`, `gradlew`, `README.md`.

⚠️ В `.gitignore` уже исключены: модели Vosk, `.ppn`-файлы, `local.properties` и `secrets.properties`.
Свой Picovoice AccessKey не коммитьте в открытом репозитории — держите плейсхолдер в Config.kt
и вставляйте ключ только локально перед сборкой.

## 5. Выложить APK в Releases
1. Репозиторий → **Deployments → Releases → Draft a new release**
2. Tag: `v0.1` → название релиза → **Attach binaries** → перетащите `app-debug.apk` (до 2 ГБ)
3. Publish release → ссылка вида `https://github.com/<логин>/voice-assistant/releases/download/v0.1/app-debug.apk`
   — её можно открыть прямо на телефоне, чтобы установить приложение.

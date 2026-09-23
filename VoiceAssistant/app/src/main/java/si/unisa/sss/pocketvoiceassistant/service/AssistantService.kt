package si.unisa.sss.pocketvoiceassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import si.unisa.sss.pocketvoiceassistant.Config
import si.unisa.sss.pocketvoiceassistant.MainActivity
import si.unisa.sss.pocketvoiceassistant.R
import si.unisa.sss.pocketvoiceassistant.state.AssistantState
import si.unisa.sss.pocketvoiceassistant.stt.BooleanHolder
import si.unisa.sss.pocketvoiceassistant.stt.ModelNotLoadedException
import si.unisa.sss.pocketvoiceassistant.stt.VoskStt
import si.unisa.sss.pocketvoiceassistant.tts.TtsEngine
import si.unisa.sss.pocketvoiceassistant.wakeword.WakeWordDetector

/**
 * Foreground-сервис с типом microphone. Жизненный цикл конвейера:
 *
 *   IDLE -> (кодовое слово) -> LISTENING -> THINKING(заглушка) -> SPEAKING -> IDLE
 *
 * Все компоненты офлайн и бесплатны: wake и STT — Vosk, озвучка — системный TTS.
 */
class AssistantService : LifecycleService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wake: WakeWordDetector? = null
    private lateinit var stt: VoskStt
    private lateinit var tts: TtsEngine
    private val cancelFlag = BooleanHolder(false)

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        stt = VoskStt(this)
        tts = TtsEngine(this)

        // Тяжёлая загрузка модели Vosk — в фоне, UI не блокируем
        scope.launch {
            try {
                AssistantState.update { it.copy(phase = "Загрузка модели распознавания…") }
                stt.prepareModel()
                wake = WakeWordDetector(this@AssistantService, stt.modelOrThrow())
                startWakeLoop()
            } catch (e: Exception) {
                AssistantState.update { it.copy(phase = "Ошибка загрузки модели: ${e.message}") }
            }
        }
    }

    private fun startWakeLoop() {
        wake?.start(
            onWake = { onKeywordHeard() },
            onError = { e -> AssistantState.update { it.copy(phase = "Ошибка wake-детектора: ${e.message}") } }
        )
        AssistantState.update { it.copy(phase = "Слушаю кодовое слово «${keywordLabel()}»…") }
    }

    private fun keywordLabel(): String = Config.WAKE_KEYWORDS.joinToString("/")

    private fun onKeywordHeard() {
        AssistantState.update { it.copy(phase = "Слышу команду…", heardText = "", replyText = "") }
        wake?.stop() // освобождаем микрофон для Vosk

        scope.launch {
            try {
                val text = try {
                    stt.listenOnce(Config.COMMAND_TIMEOUT_MS, cancelFlag)
                } catch (e: ModelNotLoadedException) {
                    // Модель ещё грузится или отсутствует в assets — не роняем сервис
                    AssistantState.update { it.copy(phase = e.message ?: "Модель Vosk недоступна") }
                    wake?.resume()
                    return@launch
                }
                AssistantState.update { it.copy(phase = "Обработка…", heardText = text) }

                // Здесь будет логика ассистента; пока — эхо-ответ
                val reply = if (text.isBlank()) "Я вас не расслышала." else "Вы сказали: $text"

                AssistantState.update { it.copy(phase = "Говорю…", replyText = reply) }
                tts.speakAndWait(reply)
            } catch (e: Exception) {
                AssistantState.update { it.copy(phase = "Ошибка: ${e.message}") }
            } finally {
                wake?.resume()
                AssistantState.update { it.copy(phase = "Слушаю кодовое слово «${keywordLabel()}»…") }
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "assistant_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.channel_desc) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Фоновое прослушивание активно")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    // LifecycleService в androidx 2.8+ объявляет onBind(final Intent) — переопределять нельзя.
    override fun onDestroy() {
        cancelFlag.value = true
        wake?.release()
        stt.release()
        tts.shutdown()
        AssistantState.update { it.copy(phase = "Остановлено") }
        super.onDestroy()
    }

    companion object {
        /** Вызывается из UI при нажатии «Стоп». */
        @Volatile var stopRequested = false
    }
}

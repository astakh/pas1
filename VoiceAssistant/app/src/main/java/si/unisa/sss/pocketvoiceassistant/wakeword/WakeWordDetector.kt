package si.unisa.sss.pocketvoiceassistant.wakeword

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import si.unisa.sss.pocketvoiceassistant.Config
import java.util.Locale

/**
 * Детектор кодового слова БЕЗ сторонних сервисов (Picovoice не нужен).
 *
 * Реализация: постоянно открытый микрофон (AudioRecord 16 kHz mono) +
 * потоковый распознаватель Vosk (та же модель, что и для STT). Каждые ~200 мс
 * аудио подаётся в Recognizer; когда Vosk выдаёт финальный сегмент с текстом,
 * он проверяется на наличие слов из Config.WAKE_KEYWORDS.
 *
 * Ограничение метода: возможна фоновая речь — детектор слушает всё вокруг.
 * Для MVP это приемлемо; список ключевых слов настраивается в Config.kt.
 *
 * ВАЖНО: пока идёт распознавание команды, детектор нужно останавливать
 * (микрофон на устройстве один) — см. [stop]/[start].
 */
class WakeWordDetector(
    private val context: Context,
    private val model: Model, // переиспользуем модель Vosk — скачивать вторую не нужно
) {

    @Volatile private var running = false
    private var thread: Thread? = null
    private var onWakeCb: (() -> Unit)? = null
    private var onErrorCb: ((Throwable) -> Unit)? = null

    /** Запускает фоновый цикл прослушивания. onWake вызывается из рабочего потока. */
    @SuppressLint("MissingPermission") // RECORD_AUDIO запрашивается в MainActivity
    fun start(onWake: () -> Unit, onError: (Throwable) -> Unit) {
        if (running) return
        onWakeCb = onWake
        onErrorCb = onError
        running = true
        thread = Thread {
            var rec: Recognizer? = null
            var audio: AudioRecord? = null
            try {
                rec = Recognizer(model, SAMPLE_RATE.toFloat())
                val minBuf = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                audio = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuf, 3200)
                )
                if (audio.state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("Не удалось инициализировать микрофон")
                }
                audio.startRecording()
                val buf = ShortArray(3200) // 200 мс за раз
                var lastKeywordAt = 0L

                while (running) {
                    val n = audio.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    // API vosk-android 0.3.47: acceptWaveForm(short[], len): Boolean
                    if (rec.acceptWaveForm(buf, n)) {
                        val text = JSONObject(rec.result).optString("text", "")
                        if (containsKeyword(text)) {
                            val now = System.currentTimeMillis()
                            if (now - lastKeywordAt > Config.WAKE_DEBOUNCE_MS) {
                                lastKeywordAt = now
                                onWake()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (running) onError(e)
            } finally {
                runCatching { audio?.stop() }
                runCatching { audio?.release() }
                runCatching { rec?.close() }
            }
        }.apply {
            isDaemon = true
            name = "wakeword-loop"
            start()
        }
    }

    /** Повторный запуск после обслуживания команды (колбэки помнятся). */
    fun resume() {
        val ow = onWakeCb ?: return
        start(ow, onErrorCb ?: {})
    }

    /** Полная остановка цикла (освобождает микрофон для VoskStt). */
    fun stop() {
        running = false
        thread?.let {
            try {
                it.join(1500)
            } catch (_: InterruptedException) {
            }
        }
        thread = null
    }

    fun release() = stop()

    private fun containsKeyword(text: String): Boolean {
        if (text.isBlank()) return false
        val norm = text.lowercase(Locale.ROOT)
        return Config.WAKE_KEYWORDS.any { kw ->
            norm.split(Regex("[^\\p{L}]+")).contains(kw.lowercase(Locale.ROOT))
        }
    }

    companion object {
        const val SAMPLE_RATE = 16000
    }
}

package si.unisa.sss.pocketvoiceassistant.stt

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.IOException

/**
 * Офлайн распознавание речи (Vosk, 16 kHz mono PCM).
 * Используется ТОЛЬКО в активном состоянии (после кодового слова):
 * AudioRecord создаётся на время записи фразы и сразу освобождается,
 * чтобы не конфликтовать с детектором кодового слова.
 */
class VoskStt(private val context: Context) {

    @Volatile private var model: Model? = null

    companion object {
        const val SAMPLE_RATE = 16000
    }

    /** Долго (~1-3 мин при первом запуске): распаковывает модель из assets в filesDir. */
    @Throws(IOException::class)
    fun prepareModel() {
        if (model != null) return
        val modelDir = ensureModelCopied()
        model = Model(modelDir.absolutePath)
    }

    fun isModelLoaded(): Boolean = model != null

    /** Модель должна быть загружена (используется WakeWordDetector). */
    fun modelOrThrow(): Model =
        model ?: throw ModelNotLoadedException("Модель Vosk ещё не загружена")

    /**
     * Блокирующая запись+распознавание одной фразы.
     * Завершается по тишине (Vosk segment-end), по [Config.COMMAND_TIMEOUT_MS]
     * или когда вызовут [cancel]. Возвращает распознанный текст (может быть "").
     */
    @SuppressLint("MissingPermission") // RECORD_AUDIO запрошен в MainActivity
    fun listenOnce(timeoutMs: Long, cancelFlag: BooleanHolder): String {
        val m = model ?: throw ModelNotLoadedException(
            "Модель распознавания ещё загружается (первый запуск может занять 1–3 минуты) " +
                    "или отсутствует в assets/vosk/. Повторите кодовое слово позже."
        )
        val rec = Recognizer(m, SAMPLE_RATE.toFloat())
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        @Suppress("MissingPermission", "DEPRECATION")
        val audio = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, 3200)
        )
        val result = StringBuilder()
        try {
            audio.startRecording()
            val buf = ShortArray(1600) // 100 мс
            val start = System.currentTimeMillis()
            while (!cancelFlag.value && System.currentTimeMillis() - start < timeoutMs) {
                val n = audio.read(buf, 0, buf.size)
                if (n <= 0) continue
                if (rec.acceptWaveForm(buf, n)) {
                    result.append(textFromJson(rec.result))
                    break // фраза окончена (тишина после речи)
                }
            }
            if (result.isEmpty()) {
                result.append(textFromJson(rec.finalResult))
            }
        } finally {
            runCatching { audio.stop() }
            audio.release()
            rec.close()
        }
        return result.toString().trim()
    }

    fun release() {
        model?.close()
        model = null
    }

    private fun textFromJson(json: String): String =
        try { JSONObject(json).optString("text", "") } catch (_: Exception) { "" }

    /** Копирует каталог модели из assets во внутреннее хранилище (один раз). */
    private fun ensureModelCopied(): File {
        val assetRoot = "vosk/${si.unisa.sss.pocketvoiceassistant.Config.VOSK_MODEL_ASSET_DIR}"
        val dest = File(context.filesDir, assetRoot)
        if (!File(dest, "uuid").exists()) {
            dest.mkdirs()
            copyAssetTree(assetRoot, dest)
        }
        return dest
    }

    private fun copyAssetTree(path: String, destDir: File) {
        val am = context.assets
        val children = am.list(path) ?: return
        for (c in children) {
            val childPath = "$path/$c"
            val grand = am.list(childPath) ?: emptyArray()
            if (grand.isEmpty()) { // файл
                am.open(childPath).use { input ->
                    val out = File(destDir, c)
                    out.parentFile?.mkdirs()
                    out.outputStream().use { input.copyTo(it) }
                }
            } else { // каталог
                val sub = File(destDir, c)
                sub.mkdirs()
                copyAssetTree(childPath, sub)
            }
        }
    }
}

/** Простая мутируемая обёртка для флага отмены из другого потока. */
class BooleanHolder(@Volatile var value: Boolean = false)

/** Модель Vosk ещё не готова (грузится или отсутствует в assets). */
class ModelNotLoadedException(message: String) : Exception(message)

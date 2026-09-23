package si.unisa.sss.pocketvoiceassistant.wakeword

import android.content.Context
import ai.picovoice.porcupine.PorcupineManager
import si.unisa.sss.pocketvoiceassistant.Config
import si.unisa.sss.pocketvoiceassistant.WakeWord

/**
 * Обёртка над Picovoice Porcupine. Работает полностью офлайн.
 * PorcupineManager сам захватывает микрофон (16 kHz PCM) внутри библиотеки.
 *
 * ВАЖНО: пока идёт распознавание команды (Vosk тоже читает микрофон),
 * детектор кодового слова нужно останавливать — см. [pause]/[resume].
 */
class WakeWordDetector(private val context: Context) {

    private var manager: PorcupineManager? = null

    fun start(onWake: () -> Unit, onError: (Throwable) -> Unit) {
        if (manager != null) return
        try {
            val builder = PorcupineManager.Builder()
                .setAccessKey(Config.PICOVOICE_ACCESS_KEY)
                .setSensitivity(Config.WAKE_SENSITIVITY)
                .setErrorCallback { err -> onError(err) }

            when (val kw = Config.WAKE_KEYWORD) {
                is WakeWord.BuiltInKeyword -> builder.setKeyword(kw.keyword)
                is WakeWord.CustomKeyword ->
                    builder.setKeywordPath(copyAsset(context, kw.ppnAssetPath))
            }

            val m = builder.build(context) { _, _ -> onWake() }
            manager = m
            m.start()
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun pause() {
        try {
            manager?.stop()
        } catch (_: Exception) {
        }
    }

    fun resume() {
        try {
            manager?.start()
        } catch (_: Exception) {
        }
    }

    fun release() {
        pause()
        manager?.delete()
        manager = null
    }

    /** KMP-файл модели нельзя дать Porcupine напрямую из assets — копируем во internal storage. */
    private fun copyAsset(context: Context, assetPath: String): String {
        val out = java.io.File(context.filesDir, assetPath.substringAfterLast('/'))
        if (!out.exists()) {
            context.assets.open(assetPath).use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
        }
        return out.absolutePath
    }
}

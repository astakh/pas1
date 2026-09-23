package si.unisa.sss.pocketvoiceassistant.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import si.unisa.sss.pocketvoiceassistant.Config
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Системный Android TextToSpeech — озвучивание полностью на устройстве.
 * [speakAndWait] блокирует вызывающий поток до окончания произношения,
 * чтобы микрофон детектора не слушал голос самого ассистента.
 */
class TtsEngine(context: Context) {

    private var ready = false

    @Volatile private var latch: CountDownLatch? = null

    // Колбэк инициализации передаём в конструктор: поля к этому моменту уже готовы,
    // ссылка на `tts` внутри колбэка не используется.
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) onTtsReady()
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { latch?.countDown() }
            @Deprecated("deprecated in API 21")
            override fun onError(utteranceId: String?) { latch?.countDown() }
            override fun onError(utteranceId: String?, errorCode: Int) { latch?.countDown() }
        })
    }

    private fun onTtsReady() {
        val result = tts.setLanguage(Locale.forLanguageTag(Config.TTS_LANGUAGE))
        ready = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun isReady(): Boolean = ready

    /** Произносит текст и ждёт завершения (макс. [maxWaitMs]). */
    fun speakAndWait(text: String, maxWaitMs: Long = 30_000L) {
        if (!ready || text.isBlank()) return
        val l = CountDownLatch(1)
        latch = l
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cmd-reply")
        l.await(maxWaitMs, TimeUnit.MILLISECONDS)
        latch = null
    }

    fun shutdown() {
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }
}

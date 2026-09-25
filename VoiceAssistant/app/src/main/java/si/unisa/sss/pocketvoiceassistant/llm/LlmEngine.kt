package si.unisa.sss.pocketvoiceassistant.llm

import android.content.Context
import si.unisa.sss.pocketvoiceassistant.Config
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Офлайн-LLM на llama.cpp (GGUF). Файл модели пользователь кладёт в
 * «Загрузки» телефона, путь выбирается в настройках приложения.
 *
 * Все вызовы сериализуются в одном потоке-экзекьюторе: llama.cpp не
 * потокобезопасен, а инференс блокирующий (секунды) — UI и аудио от этого
 * не зависят.
 */
class LlmEngine(private val context: Context) {

    enum class State { NOT_CONFIGURED, LOADING, READY, FAILED }

    @Volatile var state: State = State.NOT_CONFIGURED
        private set
    @Volatile var lastError: String? = null
        private set

    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var libOk = false

    /** Путь к GGUF из настроек; null / файла нет -> NOT_CONFIGURED. */
    fun modelFile(): File? {
        val path = Config.modelPath(context) ?: return null
        val f = File(path)
        return if (f.isFile && f.length() > 1_000_000) f else null
    }

    /** Асинхронно загружает модель. Повторные вызовы во время загрузки игнорируются. */
    fun loadAsync() {
        if (state == State.LOADING || state == State.READY) return
        val f = modelFile()
        if (f == null) { state = State.NOT_CONFIGURED; return }
        state = State.LOADING
        lastError = null
        executor.submit {
            try {
                if (!libOk) { LlmBridge.loadLibrary(); libOk = true }
                val err = LlmBridge.nativeLoadModel(
                    f.absolutePath, Runtime.getRuntime().availableProcessors().coerceIn(1, 6),
                    Config.LLM_CTX_SIZE
                )
                if (err == null) state = State.READY
                else { lastError = err; state = State.FAILED }
            } catch (t: Throwable) {
                lastError = "Не удалось загрузить llama.cpp: ${'$'}{t.message}"
                state = State.FAILED
            }
        }

    /**
     * Синхронная генерация (вызывать только из фонового потока).
     * history — предыдущие реплики диалога, userText — текущая команда.
     */
    fun chat(history: List<Pair<String, String>>, userText: String): String {
        check(state == State.READY) { "LLM не готов: ${'$'}{lastError ?: state}" }
        return executor.submit {
            LlmBridge.chat(
                Config.LLM_SYSTEM_PROMPT, history, userText,
                Config.LLM_MAX_TOKENS, Config.LLM_TEMPERATURE
            )
        }.get()
    }

    private companion object {
        /** Спецмаркеры ChatML собираются из частей намеренно. */
        val IM_START: String = "<" + "|im_start|" + ">"
        val IM_END: String = "<" + "|im_end|" + ">\n"
    }
}

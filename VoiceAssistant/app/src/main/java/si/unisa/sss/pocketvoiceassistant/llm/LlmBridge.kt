package si.unisa.sss.pocketvoiceassistant.llm

/**
 * Мост к нативной библиотеке libassistant_llm.so (llama.cpp).
 * Реализация на C++ — см. app/src/main/cpp/assistant_llm.cpp.
 */
object LlmBridge {

    @Volatile private var loaded = false

    fun loadLibrary() {
        if (!loaded) {
            System.loadLibrary("assistant_llm")
            loaded = true
        }
    }

    /**
     * Загружает GGUF-модель. Возвращает текст ошибки или null, если всё ок.
     * @param nThreads число потоков инференса (обычно = ядрам big-кластера)
     * @param ctxSize  размер контекстного окна в токенах
     */
    external fun nativeLoadModel(modelPath: String, nThreads: Int, ctxSize: Int): String?

    /** Генерация ответа. prompt — уже размеченный ChatML-текст. Блокирующий вызов. */
    external fun nativeGenerate(prompt: String, maxTokens: Int, temperature: Float): String

    /** Освобождает память модели. */
    external fun nativeFree()

    // ---- ChatML-разметка и постобработка (в Kotlin, чтобы не возиться с юникодом в C++) ----

    private val imStart: String get() = "<" + "|im_start|" + ">"
    private val imEnd: String get() = "<" + "|im_end|" + ">\n"

    /** Полный цикл диалога: history — предыдущие реплики (role, text), userText — текущая. */
    fun chat(
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userText: String,
        maxTokens: Int,
        temperature: Float,
    ): String {
        val prompt = buildPrompt(systemPrompt, history, userText)
        return sanitize(nativeGenerate(prompt, maxTokens, temperature))
    }

    /** Разметка ChatML для Qwen2.5-Instruct. */
    private fun buildPrompt(
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userText: String,
    ): String {
        val sb = StringBuilder()
        sb.append(imStart).append("system\n").append(systemPrompt).append(imEnd)
        for ((role, text) in history) {
            sb.append(imStart).append(role).append("\n").append(text.trim()).append(imEnd)
        }
        sb.append(imStart).append("user\n").append(userText.trim()).append(imEnd)
        sb.append(imStart).append("assistant\n")
        return sb.toString()
    }

    /** Обрезка по стоп-токену и удаление «сырых» спецмаркеров из текста ответа. */
    private fun sanitize(raw: String): String {
        var out = raw
        val cut = out.indexOf("<" + "|im_end|")
        if (cut >= 0) out = out.substring(0, cut)
        out = out.replace(imStart, "").replace("<" + "|im_end|", "")
        return out.trim()
    }
}

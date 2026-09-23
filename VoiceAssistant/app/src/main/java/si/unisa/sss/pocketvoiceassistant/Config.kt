package si.unisa.sss.pocketvoiceassistant

/**
 * Единая точка конфигурации. Перед сборкой заполните значения ниже
 * (см. README.md, раздел «Что нужно подготовить»).
 */
object Config {
    /** AccessKey из консоли Picovoice (https://console.picovoice.ai) — бесплатный. */
    const val PICOVOICE_ACCESS_KEY: String = "ВСТАВЬТЕ_ВАШ_ACCESSKEY"

    /**
     * Кодовое слово. Варианты:
     *  - встроенное: BuiltInKeyword.JARVIS (или любой другой из enum)
     *  - кастомное (например русское): CustomKeyword("vnimanie", "assets:///vnimanie_russian.ppn")
     */
    // val WAKE_KEYWORD: WakeWord = WakeWord.BuiltInKeyword(Porcupine.BuiltInKeyword.JARVIS)
    val WAKE_KEYWORD: WakeWord = WakeWord.CustomKeyword(
        label = "внимание",
        ppnAssetPath = "vnimanie_russian.ppn" // файл лежит в app/src/main/assets/
    )

    /** Порог чувствительности wake-детектора 0..1 (выше — чаще срабатывает, больше ложных). */
    const val WAKE_SENSITIVITY: Float = 0.65f

    /** Модель Vosk для распознавания русской речи (копируется в filesDir при первом запуске). */
    const val VOSK_MODEL_ASSET_DIR: String = "vosk-model-ru-0.42"

    /** Язык системного TTS. */
    const val TTS_LANGUAGE: String = "ru-RU"

    /** Макс. длительность фразы после кодового слова, мс (Vosk обрывает раньше — по тишине). */
    const val COMMAND_TIMEOUT_MS: Long = 10_000L
}

sealed class WakeWord {
    data class BuiltInKeyword(val keyword: ai.picovoice.porcupine.Porcupine.BuiltInKeyword) : WakeWord()
    data class CustomKeyword(val label: String, val ppnAssetPath: String) : WakeWord()
}

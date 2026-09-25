package si.unisa.sss.pocketvoiceassistant.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AssistantUiState(
    val phase: String = "Не запущено",
    val heardText: String = "",
    val replyText: String = ""
)

/** Простой одностраничный стор: сервис пишет — Compose читает. */
object AssistantState {
    private val _state = MutableStateFlow(AssistantUiState())
    val state = _state.asStateFlow()

    fun update(transform: (AssistantUiState) -> AssistantUiState) {
        _state.value = transform(_state.value)
    }
}

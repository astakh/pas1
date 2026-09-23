package si.unisa.sss.pocketvoiceassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import si.unisa.sss.pocketvoiceassistant.state.AssistantState

@Composable
fun AssistantScreen(onStart: () -> Unit, onStop: () -> Unit) {
    val state by AssistantState.state.collectAsState()
    var running by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Голосовой ассистент", style = MaterialTheme.typography.headlineMedium)

            Text(
                text = state.phase,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            )

            if (state.heardText.isNotBlank()) {
                Text(
                    "Распознано: «${state.heardText}»",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
            if (state.replyText.isNotBlank()) {
                Text(
                    "Ответ: ${state.replyText}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (running) { onStop() } else { onStart() }
                    running = !running
                },
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(if (running) "Остановить" else "Запустить прослушивание")
            }

            OutlinedButton(
                onClick = { onStart() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Перезапустить сервис")
            }
        }
    }
}

package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DataGeneratorScreen(onGenerate: (Int) -> Unit, onBack: () -> Unit) {
    var count by remember { mutableStateOf("5") }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Text("← Back to Home")
        }

        OutlinedTextField(
            value = count,
            onValueChange = { count = it },
            label = { Text("How many items to generate?") }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onGenerate(count.toIntOrNull() ?: 0) }) {
            Text("Generate")
        }
    }
}

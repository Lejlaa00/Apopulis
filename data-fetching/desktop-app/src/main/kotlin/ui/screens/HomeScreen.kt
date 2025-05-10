package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to News App")
        Button(onClick = { onNavigate("newsList") }) { Text("News List") }
        Button(onClick = { onNavigate("addNews") }) { Text("Add News") }
        Button(onClick = { onNavigate("scraper") }) { Text("Scraper") }
        Button(onClick = { onNavigate("generator") }) { Text("Generate Data") }
    }
}

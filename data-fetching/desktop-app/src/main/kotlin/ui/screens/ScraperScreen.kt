package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*

import model.NewsItem

@Composable
fun ScraperScreen(
    news: List<NewsItem>,
    onRefresh: (String) -> Unit,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf("All") }
    val options = listOf("All", "24ur", "N1info")

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onBack) {
            Text("← Back to Home")
        }

        Spacer(Modifier.height(8.dp))

        // Dropdown za izvor
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("Selected: $selected ⌄")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(150.dp)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        selected = option
                        expanded = false
                    }) {
                        Text(option)
                    }
                }
            }
        }


        Spacer(Modifier.height(16.dp))

        Button(onClick = { onRefresh(selected) }) {
            Text("Run Scraper Manually")
        }

        Spacer(Modifier.height(16.dp))
        Text("Parsed items: ${news.size}")
    }
}


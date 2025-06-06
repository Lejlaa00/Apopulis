package ui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.NewsItem

@Composable
fun ScraperScreen(
    news: List<NewsItem>,
    onRefresh: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    SidebarWrapper(currentScreen = "scraper", onNavigate = onNavigate) {
        var selected by remember { mutableStateOf("All") }
        var expanded by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var parsedCount by remember { mutableStateOf(0) }

        val options = listOf("All", "24ur", "N1info")

        // Resetiraj broj kada počne skrejpanje
        LaunchedEffect(news) {
            if (isLoading) {
                parsedCount = news.size
                isLoading = false
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Spacer(Modifier.height(8.dp))

            // Dropdown za izbor izvora
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selected,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Source") },
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Open source menu")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
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

            Button(
                onClick = {
                    isLoading = true
                    parsedCount = 0
                    onRefresh(selected)
                },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Get Latest News")
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Scraping in progress...")
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

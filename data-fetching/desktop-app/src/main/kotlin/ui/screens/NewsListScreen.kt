package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.NewsItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.net.URL
import javax.imageio.ImageIO


@Composable
fun NewsListScreen(
    news: List<NewsItem>,
    onEdit: (NewsItem) -> Unit,
    onDelete: (NewsItem) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var toDeleteItem by remember { mutableStateOf<NewsItem?>(null) }

    val categories = listOf("All") + news.mapNotNull { it.category }.distinct()

    val filteredNews = news.filter { item ->
        (selectedCategory == "All" || item.category == selectedCategory) &&
                (selectedDate == null || item.publishedAt.toLocalDate() == selectedDate)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("← Back to Home")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DropdownMenuBox("Category: $selectedCategory", categories, selectedCategory) {
                selectedCategory = it
            }

            DatePickerBox(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )
        }

        Spacer(Modifier.height(16.dp))

        if (filteredNews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No news items match the selected filters.")
            }
        } else {
            LazyColumn {
                items(filteredNews) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Heading: ${item.heading}", style = MaterialTheme.typography.h6)
                            item.category?.let { Text("Category: $it") }
                            Text("Published At: ${item.publishedAt.toLocalDate()}")
                            Text("Source: ${item.source}")
                            item.author?.let { Text("Author: $it") }
                            Text("URL: ${item.url}")
                            item.imageUrl?.let { Text("Imagae URL: $it")}
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { onEdit(item) }) {
                                    Text("Edit")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { toDeleteItem = item }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (toDeleteItem != null) {
        AlertDialog(
            onDismissRequest = { toDeleteItem = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this news item?") },
            confirmButton = {
                TextButton(onClick = {
                    toDeleteItem?.let { onDelete(it) }
                    toDeleteItem = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { toDeleteItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DropdownMenuBox(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(label)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onSelect(option)
                    expanded = false
                }) {
                    Text(option)
                }
            }
        }
    }
}

@Composable
fun DatePickerBox(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var inputText by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Date (yyyy-MM-dd)") },
            modifier = Modifier.width(200.dp)
        )

        Row {
            Button(onClick = {
                try {
                    val parsed = LocalDate.parse(inputText, formatter)
                    onDateSelected(parsed)
                } catch (_: Exception) {
                    onDateSelected(null)
                }
            }) {
                Text("Set Date")
            }

            Spacer(Modifier.width(8.dp))

            Button(onClick = {
                onDateSelected(null)
                inputText = ""
            }) {
                Text("Clear")
            }
        }
    }
}

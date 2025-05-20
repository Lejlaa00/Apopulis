package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import model.NewsItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun NewsListScreen(
    news: List<NewsItem>,
    onEdit: (NewsItem) -> Unit,
    onDelete: (NewsItem) -> Unit,
    onNavigate: (String) -> Unit
) {
    SidebarWrapper(currentScreen = "newsList", onNavigate = onNavigate) {
        var selectedCategory by remember { mutableStateOf("All") }
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        var toDeleteItem by remember { mutableStateOf<NewsItem?>(null) }
        var expandedId by remember { mutableStateOf<String?>(null) }

        val categories = listOf("All") + news.mapNotNull { it.category }.distinct()

        val filteredNews = news.filter { item ->
            (selectedCategory == "All" || item.category == selectedCategory) &&
                    (selectedDate == null || item.publishedAt.toLocalDate() == selectedDate)
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DateInputField(
                        value = selectedDate,
                        onChange = { selectedDate = it },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryDropdownField(
                        categories = categories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filteredNews.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No news items match the selected filters.")
                }
            } else {
                LazyColumn {
                    items(filteredNews) { item ->
                        val isExpanded = expandedId == item.url

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { expandedId = if (isExpanded) null else item.url },
                            elevation = 6.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Naslov
                                Text(item.heading, style = MaterialTheme.typography.h6)

                                Spacer(Modifier.height(4.dp))

                                // Osnovne informacije
                                item.category?.let {
                                    Text("Category: $it", style = MaterialTheme.typography.body2, color = Color.Gray)
                                }
                                Text("Published at: ${item.publishedAt.toLocalDate()}", style = MaterialTheme.typography.body2, color = Color.Gray)
                                Text("Source: ${item.source}", style = MaterialTheme.typography.body2, color = Color.Gray)

                                if (isExpanded) {
                                    Spacer(Modifier.height(8.dp))
                                    Divider()
                                    Spacer(Modifier.height(8.dp))

                                    // Content
                                    item.content?.let {
                                        Text(it, style = MaterialTheme.typography.body2)
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    item.author?.let {
                                        InfoRow(label = "Author:", value = it)
                                    }

                                    InfoRow(label = "URL:", value = item.url)

                                    item.imageUrl?.let {
                                        InfoRow(label = "Image URL:", value = it)
                                    }

                                    if (!item.tags.isNullOrEmpty()) {
                                        InfoRow(label = "Tags:", value = item.tags.joinToString(", "))
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { onEdit(item) }) {
                                        Text("Edit")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    TextButton(onClick = { toDeleteItem = item }) {
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
}

@Composable
fun DateInputField(
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var text by remember { mutableStateOf(value?.format(formatter) ?: "") }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.toLocalDateOrNull())
        },
        label = { Text("Search by date") },
        singleLine = true,
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = {
                    text = ""
                    onChange(null)
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear date")
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun CategoryDropdownField(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open category menu")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach {
                DropdownMenuItem(onClick = {
                    onSelect(it)
                    expanded = false
                }) {
                    Text(it)
                }
            }
        }
    }
}

fun String.toLocalDateOrNull(): LocalDate? =
    try {
        LocalDate.parse(this)
    } catch (e: Exception) {
        null
    }

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.body2
        )
    }
}

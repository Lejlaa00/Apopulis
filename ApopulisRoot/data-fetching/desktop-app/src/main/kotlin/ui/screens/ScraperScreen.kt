package ui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.NewsItem
import ui.ui.theme.AppColors

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

        val options = listOf("All", "24ur", "n1info")

        LaunchedEffect(news) {
            if (isLoading) {
                parsedCount = news.size
                isLoading = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BgDarkest)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "News Scraper",
                style = MaterialTheme.typography.h5,
                color = AppColors.TextWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Select a source and retrieve the latest news from the web.",
                style = MaterialTheme.typography.body2,
                color = AppColors.TextMuted
            )

            Spacer(Modifier.height(24.dp))

            Card(
                elevation = 8.dp,
                backgroundColor = AppColors.BgLight,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Select Source", color = AppColors.TextLight)
                    Spacer(Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selected,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Source") },
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open menu", tint = AppColors.Icon)
                                }
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextWhite,
                                backgroundColor = AppColors.BgDarker,
                                focusedBorderColor = AppColors.Accent,
                                unfocusedBorderColor = AppColors.Divider,
                                focusedLabelColor = AppColors.TextLight,
                                unfocusedLabelColor = AppColors.TextMuted,
                                cursorColor = AppColors.Accent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(AppColors.BgDarker)
                        ) {
                            options.forEach { option ->
                                DropdownMenuItem(onClick = {
                                    selected = option
                                    expanded = false
                                }) {
                                    Text(option, color = AppColors.TextWhite)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = {
                                isLoading = true
                                parsedCount = 0
                                onRefresh(selected)
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Accent)
                        ) {
                            Text("Get Latest News")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = AppColors.Accent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Scraping in progress...", color = AppColors.TextMuted)
                        }
                    } else if (parsedCount > 0) {
                        Text("$parsedCount articles retrieved successfully.", color = AppColors.TextLight)
                    }
                }
            }
        }
    }
}



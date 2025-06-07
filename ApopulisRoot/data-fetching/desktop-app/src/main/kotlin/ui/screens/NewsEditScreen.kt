package ui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import model.NewsItem
import java.time.LocalDateTime
import ui.ui.theme.AppColors
import androidx.compose.ui.Alignment

@Composable
fun NewsEditScreen(
    item: NewsItem?,
    onSave: (NewsItem) -> Unit,
    onNavigate: (String) -> Unit,
    showBackButton: Boolean = true,
    screenKey: String = "editNews"
) {
    SidebarWrapper(currentScreen = screenKey, onNavigate = onNavigate) {
        val scrollState = rememberScrollState()

        var heading by remember { mutableStateOf(item?.title ?: "") }
        var content by remember { mutableStateOf(item?.content ?: "") }
        var author by remember { mutableStateOf(item?.author ?: "") }
        var source by remember { mutableStateOf(item?.source ?: "") }
        var url by remember { mutableStateOf(item?.url ?: "") }
        var imageUrl by remember { mutableStateOf(item?.imageUrl ?: "") }
        var category by remember { mutableStateOf(item?.category ?: "") }
        var location by remember { mutableStateOf(item?.location ?: "") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BgDarkest)
                .verticalScroll(scrollState)
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .widthIn(min = 0.dp, max = 700.dp) // bolje za responsivnost
            )
            {
                Text(
                    text = if (item == null) "Add New Article" else "Edit Article",
                    style = MaterialTheme.typography.h5,
                    color = AppColors.TextWhite
                )

                Spacer(Modifier.height(16.dp))

                Card(
                    backgroundColor = AppColors.BgLight,
                    elevation = 8.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        InputField("Title", heading) { heading = it }
                        InputField("Content", content, lines = 4) { content = it }
                        InputField("Author", author) { author = it }
                        InputField("Source", source) { source = it }
                        InputField("URL", url) { url = it }
                        InputField("Image URL", imageUrl) { imageUrl = it }
                        InputField("Category", category) { category = it }
                        InputField("Location", location) { location = it }

                        Spacer(Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (showBackButton) {
                                OutlinedButton(
                                    onClick = { onNavigate("newsList") },
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Accent)
                                ) {
                                    Text("← Back")
                                }
                            }

                            Button(
                                onClick = {
                                    onSave(
                                        NewsItem(
                                            id = item?.id,
                                            title = heading,
                                            content = content,
                                            author = author.ifBlank { null },
                                            source = source,
                                            url = url,
                                            publishedAt = item?.publishedAt ?: LocalDateTime.now(),
                                            imageUrl = imageUrl.ifBlank { null },
                                            category = category.ifBlank { null },
                                            location = location.ifBlank { null },
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Accent)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputField(label: String, value: String, lines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = AppColors.TextWhite,
            backgroundColor = AppColors.BgDarker,
            focusedBorderColor = AppColors.Accent,
            unfocusedBorderColor = AppColors.Divider,
            focusedLabelColor = AppColors.TextLight,
            unfocusedLabelColor = AppColors.TextMuted,
            cursorColor = AppColors.Accent
        ),
        maxLines = lines
    )
}

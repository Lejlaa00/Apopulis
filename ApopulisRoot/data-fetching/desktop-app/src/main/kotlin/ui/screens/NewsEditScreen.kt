package ui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.NewsItem
import java.time.LocalDateTime

@Composable
fun NewsEditScreen(
    item: NewsItem?,
    onSave: (NewsItem) -> Unit,
    onNavigate: (String) -> Unit,
    showBackButton: Boolean = true
) {
    SidebarWrapper(currentScreen = "editNews", onNavigate = onNavigate) {
        val scrollState = rememberScrollState()

        var heading by remember { mutableStateOf(item?.title ?: "") }
        var content by remember { mutableStateOf(item?.content ?: "") }
        var author by remember { mutableStateOf(item?.author ?: "") }
        var source by remember { mutableStateOf(item?.source ?: "") }
        var url by remember { mutableStateOf(item?.url ?: "") }
        var imageUrl by remember { mutableStateOf(item?.imageUrl ?: "") }
        var category by remember { mutableStateOf(item?.category ?: "") }
        var location by remember { mutableStateOf(item?.location ?: "") }
        var tags by remember { mutableStateOf(item?.tags?.joinToString(", ") ?: "") }


        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            if (showBackButton) {
                Button(onClick = { onNavigate("newsList") }, modifier = Modifier.padding(8.dp)) {
                    Text("← Back")
                }
            }

            OutlinedTextField(value = heading, onValueChange = { heading = it }, label = { Text("Title") })
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") })
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") })
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source") })
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") })
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") })
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
            Spacer(Modifier.height(8.dp))

            Button(onClick = {
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
                        tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                )
            }) {
                Text("Save")
            }
        }
    }
}

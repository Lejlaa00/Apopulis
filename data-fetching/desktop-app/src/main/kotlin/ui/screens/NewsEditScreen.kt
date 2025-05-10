package ui.screens

import androidx.compose.foundation.layout.*
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
    onBack: () -> Unit
) {
    var heading by remember { mutableStateOf(item?.heading ?: "") }
    var content by remember { mutableStateOf(item?.content ?: "") }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Text("← Back to Home")
        }

        OutlinedTextField(
            value = heading,
            onValueChange = { heading = it },
            label = { Text("Title") }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            onSave(
                NewsItem(
                    heading = heading,
                    content = content,
                    author = item?.author,
                    source = item?.source ?: "Manual Entry",
                    url = item?.url ?: "",
                    publishedAt = LocalDateTime.now(),
                    imageUrl = item?.imageUrl,
                    category = item?.category,
                    tags = item?.tags ?: emptyList()
                )
            )
        }) {
            Text("Save")
        }
    }
}

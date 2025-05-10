package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.NewsItem

@Composable
fun NewsListScreen(
    news: List<NewsItem>,
    onEdit: (NewsItem) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Text("← Back to Home")
        }

        if (news.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No news items available.")
            }
            return
        }

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(news) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Title: ${item.heading}", style = MaterialTheme.typography.h6)
                        Text("Source: ${item.source}")
                        Text("Category: ${item.category ?: "Unspecified"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onEdit(item) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Edit")
                        }
                    }
                }
            }
        }
    }
}

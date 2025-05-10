package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import model.NewsItem

@Composable
fun ScraperScreen(
    news: List<NewsItem>,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Text("← Back to Home")
        }

        Button(onClick = onRefresh) {
            Text("Run Scraper Manually")
        }
        Spacer(Modifier.height(16.dp))
        Text("Parsed items: {news.size}")
    }
}

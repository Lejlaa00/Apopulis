package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import model.NewsItem
import ui.screens.*

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf("home") }
    var allNews by remember { mutableStateOf(listOf<NewsItem>()) }
    var editingItem by remember { mutableStateOf<NewsItem?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                "home" -> HomeScreen(onNavigate = { currentScreen = it })
                "newsList" -> NewsListScreen(
                    news = allNews,
                    onEdit = {
                        editingItem = it
                        currentScreen = "editNews"
                    },
                    onBack = { currentScreen = "home" }
                )
                "addNews" -> AddNewsScreen(
                    onSave = {
                        allNews = allNews + it
                        currentScreen = "newsList"
                    },
                    onBack = { currentScreen = "home" }
                )
                "editNews" -> NewsEditScreen(
                    item = editingItem,
                    onSave = {
                        allNews = allNews.map { n -> if (n.url == it.url) it else n }
                        currentScreen = "newsList"
                    },
                    onBack = { currentScreen = "home" }
                )
                "scraper" -> ScraperScreen(
                    news = allNews,
                    onRefresh = {
                        // TODO: Replace with real scraper call
                        allNews = allNews + NewsItem(
                            heading = "Scraped Heading",
                            content = "Scraped content...",
                            author = "ScraperBot",
                            source = "scraper.si",
                            url = "http://scraper.si/article",
                            publishedAt = java.time.LocalDateTime.now()
                        )
                    },
                    onBack = { currentScreen = "home" }
                )
                "generator" -> DataGeneratorScreen(
                    onGenerate = { count ->
                        val generated = (1..count).map {
                            NewsItem(
                                heading = "Generated $it",
                                content = "This is fake content #$it",
                                author = "Generator",
                                source = "generator.si",
                                url = "http://generator.si/$it",
                                publishedAt = java.time.LocalDateTime.now()
                            )
                        }
                        allNews = allNews + generated
                        currentScreen = "newsList"
                    },
                    onBack = { currentScreen = "home" }
                )
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "News App") {
        App()
    }
}

package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import org.example.model.NewsItem
import org.example.nlp.Categorizer
import org.example.scraper.N1infoScraper
import org.example.scraper.U24urScraper
import ui.ui.screens.*
import ui.util.Storage

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf("home") }
    var allNews by remember { mutableStateOf(Storage.load()) }
    var editingItem by remember { mutableStateOf<NewsItem?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
                    onDelete = { toDelete ->
                        allNews = allNews.filter { it.url != toDelete.url }
                        Storage.save(allNews)
                    },
                    onNavigate = { currentScreen = it }
                )
                "addNews" -> AddNewsScreen(
                    onSave = {
                        allNews = allNews + it
                        Storage.save(allNews)
                        currentScreen = "newsList"
                    },
                    onNavigate = { currentScreen = it }
                )
                "editNews" -> NewsEditScreen(
                    item = editingItem,
                    onSave = {
                        allNews = allNews.map { n -> if (n.url == it.url) it else n }
                        currentScreen = "newsList"
                    },
                    onNavigate = { currentScreen = it }
                )
                "scraper" -> ScraperScreen(
                    news = allNews,
                    onRefresh = { source ->
                        coroutineScope.launch {
                            val scraped = when (source) {
                                "24ur" -> listOf(U24urScraper())
                                "N1info" -> listOf(N1infoScraper())
                                else -> listOf(U24urScraper(), N1infoScraper())
                            }.flatMap { it.scrape() }

                            val enrichedNews = scraped.map { item ->
                                item.copy(
                                    category = Categorizer.categorizeByText(item)
                                )
                            }

                            val newItems = enrichedNews.filter { scrapedItem ->
                                allNews.none { it.url == scrapedItem.url }
                            }

                            allNews = allNews + newItems
                            Storage.save(allNews)
                            currentScreen = "newsList"
                        }
                    }
                    ,
                    onNavigate = { currentScreen = it }
                )
                "generator" -> DataGeneratorScreen(
                    onGenerate = { generatedItems ->
                        allNews = allNews + generatedItems
                        Storage.save(allNews)
                        currentScreen = "newsList"
                    },
                    onNavigate = { currentScreen = it }
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

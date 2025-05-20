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
import model.NewsItem
import ui.screens.*
import ui.controller.Scraper

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf("home") }
    var allNews by remember { mutableStateOf(util.Storage.load()) }
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
                        util.Storage.save(allNews)
                    },
                    onNavigate = { currentScreen = it }
                )
                "addNews" -> AddNewsScreen(
                    onSave = {
                        allNews = allNews + it
                        util.Storage.save(allNews)
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
                                "24ur" -> listOf(scraper.U24urScraper())
                                "N1info" -> listOf(scraper.N1infoScraper())
                                else -> listOf(scraper.U24urScraper(), scraper.N1infoScraper())
                            }.flatMap { it.scrape() }

                            val enrichedNews = scraped.map { item ->
                                item.copy(
                                    category = nlp.Categorizer.categorizeByText(item)
                                )
                            }

                            val newItems = enrichedNews.filter { scrapedItem ->
                                allNews.none { it.url == scrapedItem.url }
                            }

                            allNews = allNews + newItems
                            util.Storage.save(allNews)
                            currentScreen = "newsList"
                        }
                    }
                    ,
                    onNavigate = { currentScreen = it }
                )
                "generator" -> DataGeneratorScreen(
                    onGenerate = { generatedItems ->
                        allNews = allNews + generatedItems
                        util.Storage.save(allNews)
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

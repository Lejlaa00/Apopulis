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
import org.example.nlp.Categorizer
import scraper.N1infoScraper
import scraper.U24urScraper
import ui.ui.screens.*
import ui.util.Storage
import scraper.NewsSender


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
                        scraper.NewsSender.delete(toDelete)
                        allNews = allNews.filter { it.url != toDelete.url }
                        Storage.save(allNews)
                    },
                    onNavigate = { currentScreen = it }
                )
                "addNews" -> AddNewsScreen(
                    onSave = { item ->
                        NewsSender.send(item)
                        allNews = allNews + item
                        Storage.save(allNews)
                        currentScreen = "newsList"
                    },
                    onNavigate = { currentScreen = it }
                )
                "editNews" -> NewsEditScreen(
                    item = editingItem,
                    onSave = {
                        val enriched = it.copy(
                            id = editingItem?.id,
                            category = it.category ?: Categorizer.categorizeByText(it),
                            location = Categorizer.extractLocationByText(it)
                        )
                        allNews = allNews.map { n -> if (n.url == enriched.url) enriched else n }
                        scraper.NewsSender.update(enriched)
                        currentScreen = "newsList"
                    },
                    onNavigate = { currentScreen = it }
                )
                "scraper" -> ScraperScreen(
                    news = allNews,
                    onRefresh = { source ->
                        coroutineScope.launch {
                            val scraped = when (source) {
                                "24ur" -> U24urScraper().scrape()
                                "N1info" -> N1infoScraper().scrape()
                                else -> {
                                    val u24News = U24urScraper().scrape()
                                    val n1News = N1infoScraper().scrape()
                                    u24News + n1News
                                }
                            }


                            val enrichedNews = scraped.map { item ->
                                item.copy(
                                    category = Categorizer.categorizeByText(item),
                                    location = Categorizer.extractLocationByText(item)

                                )
                            }

                            enrichedNews.forEach {
                                try {
                                    NewsSender.send(it)
                                } catch (e: Exception) {
                                    println("Failed to send: ${e.message}")
                                }
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
    Window(onCloseRequest = ::exitApplication, title = "Apopulis") {
        App()
    }
}

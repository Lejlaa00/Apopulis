package ui.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import model.NewsItem
import scraper.U24urScraper
import scraper.N1infoScraper

object Scraper {
    suspend fun scrapeAll(): List<NewsItem> = withContext(Dispatchers.IO) {
        val scrapers = listOf(
            U24urScraper(),
            N1infoScraper()
        )
        val allNews = mutableListOf<NewsItem>()
        for (scraper in scrapers) {
            try {
                val news = scraper.scrape()
                allNews += news
            } catch (e: Exception) {
                println("Failed to scrape from ${scraper.sourceName}: ${e.message}")
            }
        }
        allNews
    }
}

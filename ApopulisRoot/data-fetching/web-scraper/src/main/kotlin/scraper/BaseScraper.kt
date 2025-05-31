package org.example.scraper

import org.example.model.NewsItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseScraper(
    protected val baseUrl: String,
    val sourceName: String
) {
    /**
     * Fetches the HTML document from the given URL
     */
    protected suspend fun fetchDocument(url: String): Document = withContext(Dispatchers.IO) {
        Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .timeout(10000)
            .get()
    }

    /**
     * Abstract method that each specific scraper must implement
     * to scrape news from their respective websites
     */
    abstract suspend fun scrape(): List<NewsItem>

    /**
     * Helper method to parse date strings into LocalDateTime
     * Override this in specific scrapers if needed
     */
    protected open fun parseDate(dateStr: String): LocalDateTime {
        return LocalDateTime.now() // Default implementation, override in specific scrapers
    }

    /**
     * Helper method to clean text content
     */
    protected fun cleanText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[\\n\\r\\t]"), "")
    }
} 
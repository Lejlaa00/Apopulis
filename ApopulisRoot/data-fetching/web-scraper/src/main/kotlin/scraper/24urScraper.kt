package org.example.scraper

import org.example.model.NewsItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class U24urScraper : BaseScraper(
    baseUrl = "https://www.24ur.com/novice/slovenija",
    sourceName = "24ur"
) {
    override suspend fun scrape(): List<NewsItem> {
        val document = fetchDocument(baseUrl)
        val links = extractNewsLinks(document)
        println("Found ${links.size} news links")
        val newsItems = mutableListOf<NewsItem>()
        for (url in links) {
            println("Scraping: $url")
            val news = scrapeDetailedNews(url)
            if (news != null) newsItems.add(news)
            else println("Failed to scrape: $url")
        }
        return newsItems
    }

    private fun extractNewsLinks(document: Document): List<String> {
        // Extract all unique news links from the main page
        return document.select("a[href^='/novice/slovenija/']")
            .map { it.absUrl("href") }
            .distinct()
    }

    private suspend fun scrapeDetailedNews(url: String): NewsItem? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10000)
                .get()

            val heading = doc.selectFirst("h1")?.text() ?: return@withContext null
            var content = doc.select("div.contextual p").joinToString("\n\n") { it.text() }
            if (content.isBlank()) {
                content = doc.selectFirst("p.text-article-summary")?.text() ?: ""
            }
            val author = doc.selectFirst("div.flex-col.justify-center div[class*='text-black/80']")?.text()?.takeIf { it.isNotBlank() && it != "icon-user" }
            val publishedAtText = doc.selectFirst(".leading-caption")?.text()?.substringBefore("|")?.trim()?.split(",")?.getOrNull(1)?.trim() ?: ""
            val publishedAt = parseDate(publishedAtText)
            val imageUrl = doc.selectFirst("figure img")?.absUrl("src")
            NewsItem(
                title = cleanText(heading),
                content = cleanText(content),
                author = author,
                source = sourceName,
                url = url,
                publishedAt = publishedAt,
                imageUrl = imageUrl,
                tags = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun parseDate(dateStr: String): LocalDateTime {
        // Example: "07. 05. 2025 15.27"
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd. MM. yyyy HH.mm")
            LocalDateTime.parse(dateStr, formatter)
        } catch (e: Exception) {
            try {
                val formatter = DateTimeFormatter.ofPattern("dd. MM. yyyy")
                LocalDateTime.parse(dateStr, formatter)
            } catch (e: Exception) {
                LocalDateTime.now()
            }
        }
    }
} 
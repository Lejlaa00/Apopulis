package org.example.scraper

import org.example.model.NewsItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class N1infoScraper : BaseScraper(
    baseUrl = "https://n1info.si/novice/",
    sourceName = "n1info"
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
        return document.select("h3[data-testid='article-title'] a")
            .map { it.absUrl("href") }
            .distinct()
    }

    private suspend fun scrapeDetailedNews(url: String): NewsItem? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10000)
                .get()

            val heading = doc.selectFirst("h1.title[data-testid='article-main-title']")?.text() ?: return@withContext null
            var content = doc.select("div.rich-text-block p").joinToString("\n") { it.text() }
            if (content.isBlank()) {
                content = doc.selectFirst("article p[data-testid='article-lead-text']")?.text() ?: ""
            }
            val author = doc.selectFirst("span.author-name")?.text()?.takeIf { it.isNotBlank()}
            val publishedAtText = doc.selectFirst("div[data-testid='article-published-time']")?.text()?.substringBefore("|")?.trim()?.split(",")?.getOrNull(1)?.trim() ?: ""
            val publishedAt = parseDate(publishedAtText)
            val imgElement = doc.selectFirst("figure img")
            val imageUrl = imgElement?.absUrl("data-src").takeIf { it?.isNotBlank() == true }
                ?: imgElement?.absUrl("src")
            val category = doc.selectFirst("a.category")?.text()
            NewsItem(
                heading = cleanText(heading),
                content = cleanText(content),
                author = author,
                source = sourceName,
                url = url,
                publishedAt = publishedAt,
                imageUrl = imageUrl,
                category = category,
                tags = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun parseDate(dateStr: String): LocalDateTime {
        return try {
            val formatter = DateTimeFormatter.ofPattern("d. MMM yyyy. HH:mm", Locale("sl"))
            LocalDateTime.parse(dateStr, formatter)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}
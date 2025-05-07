package scraper

import model.NewsItem
import org.jsoup.nodes.Document
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class U24urScraper : BaseScraper(
    baseUrl = "https://www.24ur.com/novice/",
    sourceName = "24ur"
) {
    override suspend fun scrape(): List<NewsItem> {
        val document = fetchDocument(baseUrl)
        return parseNewsItems(document)
    }

    private fun parseNewsItems(document: Document): List<NewsItem> {
        return document.select("a[href^='/novice/']").mapNotNull { link ->
            val parent = link.parent()
            val heading = parent.selectFirst("h4")?.text() ?: return@mapNotNull null
            val content = parent.selectFirst("span.other__summary")?.text() ?: ""
            val url = "https://www.24ur.com" + link.attr("href")
            val publishedAtText = parent.selectFirst("span.text-black/60")?.text() ?: ""
            val publishedAt = parseDate(publishedAtText)
            val imageUrl = parent.selectFirst("img")?.attr("src")
            val category = parent.selectFirst("h2")?.text()
            NewsItem(
                heading = cleanText(heading),
                content = cleanText(content),
                author = null,
                source = sourceName,
                url = url,
                publishedAt = publishedAt,
                imageUrl = imageUrl,
                category = category,
                tags = emptyList()
            )
        }
    }

    override fun parseDate(dateStr: String): LocalDateTime {
        // Example: "07. 05. 2025"
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd. MM. yyyy")
            LocalDateTime.parse(dateStr, formatter)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
} 
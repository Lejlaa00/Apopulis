package org.example

import kotlinx.coroutines.*
import scraper.U24urScraper
import scraper.N1infoScraper
import nlp.TfidfCalculator
import nlp.Categorizer
import model.NewsItem

fun main() = runBlocking {
    val scrapers = listOf(
        U24urScraper(),
        N1infoScraper()
    )

    while (true) {
        println("\n--- Scraping cycle started at ${java.time.LocalDateTime.now()} ---")

        scrapers.forEach { scraper ->
            launch {
                println("\nRunning scraper: ${scraper.sourceName}")
                val news = scraper.scrape()

                val tfidfResults = TfidfCalculator.computeTopKeywords(news)

                val enrichedNews: List<NewsItem> = news.map { item ->
                    val keywords = tfidfResults[item] ?: emptyList()
                    item.copy(
                        tags = keywords,
                        category = Categorizer.categorizeByText(item)
                    )
                }

                enrichedNews.forEachIndexed { idx, item ->
                    println("\nNews #${idx + 1}")
                    println("Heading: ${item.heading}")
                    println("Category: ${item.category ?: "unknown"}")
                    println("Tags: ${item.tags.joinToString(", ")}")
                    println("URL: ${item.url}")
                    println("Image URL: ${item.imageUrl}")
                    println("Published At: ${item.publishedAt}")
                    println("Source: ${item.source}")
                    println("Author: ${item.author}")

                }

                println("\n────────────────────────────────────────────")
            }
        }

        delay(10 * 60 * 1000L)  // 10 minues
    }
}

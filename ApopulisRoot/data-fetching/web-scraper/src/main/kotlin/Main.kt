package org.example

import kotlinx.coroutines.*
import model.NewsItem
import scraper.U24urScraper
import scraper.N1infoScraper
import scraper.*
import org.example.nlp.TfidfCalculator
import org.example.nlp.Categorizer


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
                        category = Categorizer.categorizeByText(item),
                        location = Categorizer.extractLocationByText(item)
                    )
                }

                enrichedNews.forEachIndexed { idx, item ->
                    println("\nNews #${idx + 1}")
                    println("Heading: ${item.title}")
                    println("Content: ${item.content}")
                    println("Source: ${item.source}")
                    println("URL: ${item.url}")
                    println("Published At: ${item.publishedAt}")
                    println("Category: ${item.category}")
                    println("Image URL: ${item.imageUrl}")
                    println("Tags: ${item.tags}")
                    println("Author: ${item.author}")
                    println("")
                    println("===> Location: ${item.location ?: "unknown"}")
                    println("===> Category: ${item.category ?: "unknown"}")
                    println("===> Tags: ${item.tags?.joinToString(", ")}")
                    println("-------------------------------------")

                    NewsSender.send(item)
                }

                println("\n────────────────────────────────────────────")
            }
        }
        delay(10 * 60 * 1000L)  // 10 minues
    }
}

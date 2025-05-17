package org.example

import kotlinx.coroutines.*
import scraper.U24urScraper
import scraper.N1infoScraper
import nlp.KeywordExtractor

fun main() = runBlocking {
    val scrapers = listOf(
        U24urScraper(),
        //N1infoScraper()
    )
    while (true) {
        println("\n--- Scraping cycle started at ${java.time.LocalDateTime.now()} ---")
        scrapers.forEach { scraper ->
            launch {
                println("\nRunning scraper: ${scraper.sourceName}")
                val news = scraper.scrape()
                news.forEachIndexed { idx, item ->
                    val keywords = KeywordExtractor.extractTopWords(item.content)
                    println("\nNews #${idx + 1}")
                    println("Heading: ${item.heading}")
                    println("Content: ${item.content}")
                    println("Source: ${item.source}")
                    println("URL: ${item.url}")
                    println("Published At: ${item.publishedAt}")
                    println("Category: ${item.category}")
                    println("Image URL: ${item.imageUrl}")
                    println("Tags: ${item.tags}")
                    println("Author: ${item.author}")
                    println("Top Keywords: $keywords")
                }
            }
        }
        delay(10 * 60 * 1000L) // 10 minutes
    }
}

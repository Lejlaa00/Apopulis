package org.example

import kotlinx.coroutines.runBlocking
import org.example.scraper.N1infoScraper

fun main() = runBlocking {
    val scraper = N1infoScraper()
    println("Scraping news from n1info.si...")
    val news = scraper.scrape()
    news.forEachIndexed { idx, item ->
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
    }
}
package scraper

import model.NewsItem
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import it.skrape.core.document
import it.skrape.fetcher.*
import it.skrape.selects.*
import it.skrape.selects.html5.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class N1infoScraper : BaseScraper(
    baseUrl = "https://n1info.si/novice/slovenija/",
    sourceName = "n1info"
) {
    override suspend fun scrape(): List<NewsItem> = withContext(Dispatchers.IO) {
        val links = try {
            skrape(BrowserFetcher) {
                request {
                    url = baseUrl
                    timeout = 30_000
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                }
                response {
                    document.findAll("h3[data-testid='article-title'] a").mapNotNull { it.attribute("href") }
                        .distinct()
                        .filter { it.isNotBlank() }
                        .map { href ->
                            // Resolve relative URL to absolute URL
                            URL(URL(baseUrl), href).toString()
                        }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to fetch or parse main page: $baseUrl, Error: ${e.message}")
            emptyList()
        }

        println("Found ${links.size} news links from $baseUrl:")
        links.forEach { println(it) }

        val newsItems = mutableListOf<NewsItem>()

        for (linkUrl in links) {
            println("Scraping: $linkUrl")
            try {
                val news = scrapeDetailedNews(linkUrl)
                if (news != null) {
                    newsItems.add(news)
                } else {
                    println("Failed to scrape or parse detailed news (returned null): $linkUrl")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error scraping detailed news: $linkUrl, Error: ${e.message}")
            }
        }
        newsItems
    }

    private suspend fun scrapeDetailedNews(url: String): NewsItem? = withContext(Dispatchers.IO) {
        try {
            skrape(BrowserFetcher) {
                request {
                    this.url = url
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                    timeout = 30_000
                }
                response {
                    document.run {
                        try {
                            val headingText = try {
                                findFirst("h1.title[data-testid='article-main-title']").text
                            } catch (e: ElementNotFoundException) {
                                println("Heading not found for $url with selector 'h1.title[data-testid='article-main-title']'")
                                throw e
                            }

                            var contentText = try {
                                findAll("div.rich-text-block p").map { it.text }.joinToString("\n\n")
                            } catch (e: ElementNotFoundException) {
                                println("Content not found for $url with selector 'div.rich-text-block p'")
                                throw e
                            }
                            if (contentText.isBlank()) {
                                contentText = try {
                                    findFirst("article p[data-testid='article-lead-text']").text
                                } catch (e: ElementNotFoundException) {
                                    println("Lead text not found for $url with selector 'article p[data-testid='article-lead-text']'")
                                    throw e
                                }
                            }

                            val authorText = try {
                                findFirst("span.author-name").text.takeIf { it.isNotBlank() }
                            } catch (e: ElementNotFoundException) {
                                println("Author not found for $url with selector 'span.author-name'")
                                null
                            }

                            val publishedAtTextRaw = try {
                                findFirst("div[data-testid='article-published-time']").text
                                    .substringBefore("|").trim().split(",").getOrNull(1)?.trim() ?: ""
                            } catch (e: ElementNotFoundException) {
                                println("Published time not found for $url with selector 'div[data-testid='article-published-time']'")
                                throw e
                            }

                            val publishedAtTime = parseDate(publishedAtTextRaw)

                            val imageUrl = try {
                                findFirst("div[data-testid='featured-zone'] figure div img").let { element ->
                                    val src = element.attribute("data-src").takeIf { it.isNotBlank() } ?: element.attribute("src")
                                    if (src != null && !src.startsWith("http")) {
                                        "https://n1info.si/$src"
                                    } else {
                                        src
                                    }
                                }.takeIf { it.isNotBlank() }
                            } catch (e: ElementNotFoundException) {
                                println("Image not found for $url with selector 'div[data-testid='featured-zone'] figure div img'")
                                null
                            }

                            val categoryText = try {
                                findFirst("a.category").text.takeIf { it.isNotBlank() }
                            } catch (e: ElementNotFoundException) {
                                println("Category not found for $url with selector 'a.category'")
                                null
                            }

                            NewsItem(
                                title = cleanText(headingText),
                                content = cleanText(contentText),
                                author = authorText?.let { cleanText(it) },
                                source = sourceName,
                                url = url,
                                publishedAt = publishedAtTime,
                                imageUrl = imageUrl,
                                category = categoryText?.let { cleanText(it) },
                                tags = emptyList()
                            )

                        } catch (e: ElementNotFoundException) {
                            println("Required elements not found for $url, skipping item.")
                            null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Exception during detailed news scraping for $url: ${e.message}")
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
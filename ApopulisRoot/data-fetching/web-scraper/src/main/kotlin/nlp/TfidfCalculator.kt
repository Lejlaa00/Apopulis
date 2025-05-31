package org.example.nlp

import org.example.model.NewsItem
import kotlin.math.ln

object TfidfCalculator {

    fun computeTopKeywords(
        documents: List<NewsItem>,
        topN: Int = 5
    ): Map<NewsItem, List<String>> {

        val stopWords = KeywordExtractor.stopWords
        val docWordCounts = mutableMapOf<NewsItem, Map<String, Int>>()
        val documentFrequencies = mutableMapOf<String, Int>()

        // Count word frequencies per document + document frequencies
        for (doc in documents) {
            val words = doc.content.lowercase()
                .replace(Regex("[^a-zčšžđć ]"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() && it !in stopWords }

            val wordCounts = words.groupingBy { it }.eachCount()
            docWordCounts[doc] = wordCounts

            // Update DF count (only once per word per doc)
            for (word in wordCounts.keys.distinct()) {
                documentFrequencies[word] = documentFrequencies.getOrDefault(word, 0) + 1
            }
        }

        val totalDocs = documents.size
        val result = mutableMapOf<NewsItem, List<String>>()

        // Compute TF-IDF for each word in each document
        for ((doc, wordCounts) in docWordCounts) {
            val tfidfScores = mutableMapOf<String, Double>()

            for ((word, tf) in wordCounts) {
                val df = documentFrequencies[word] ?: continue
                val idf = ln(totalDocs.toDouble() / df)
                val tfidf = tf * idf
                tfidfScores[word] = tfidf
            }

            val topKeywords = tfidfScores.entries
                .sortedByDescending { it.value }
                .take(topN)
                .map { it.key }

            result[doc] = topKeywords
        }

        return result
    }
}

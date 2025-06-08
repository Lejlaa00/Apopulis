package org.example.nlp

import model.NewsItem
import kotlin.math.ln

object TfidfCalculator {

    fun computeTopKeywords(
        documents: List<NewsItem>,
        topN: Int = 100,
        tfidfThreshold: Double = 0.1
    ): Map<NewsItem, List<String>> {

        val stopWords = KeywordExtractor.stopWords
        val docWordCounts = mutableMapOf<NewsItem, Map<String, Int>>()
        val documentFrequencies = mutableMapOf<String, Int>()

        for (doc in documents) {
            val words = doc.content.lowercase()
                .replace(Regex("[^a-zčšžđć ]"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() && it !in stopWords }

            val wordCounts = words.groupingBy { it }.eachCount()
            docWordCounts[doc] = wordCounts

            for (word in wordCounts.keys.distinct()) {
                documentFrequencies[word] = documentFrequencies.getOrDefault(word, 0) + 1
            }
        }

        val totalDocs = documents.size
        val result = mutableMapOf<NewsItem, List<String>>()

        for ((doc, wordCounts) in docWordCounts) {
            val tfidfScores = mutableMapOf<String, Double>()
            val totalWordsInDoc = wordCounts.values.sum()

            for ((word, count) in wordCounts) {
                val tf = count.toDouble() / totalWordsInDoc
                val df = documentFrequencies[word] ?: continue
                val idf = ln(1 + totalDocs.toDouble() / (1 + df))
                val tfidf = tf * idf
                tfidfScores[word] = tfidf
            }

            val topKeywords = tfidfScores.entries
                .filter { it.value > tfidfThreshold }
                .sortedByDescending { it.value }
                .take(topN)
                .map { it.key }

            result[doc] = topKeywords
        }

        return result
    }
}


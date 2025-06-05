package org.example.nlp

import model.NewsItem

object Categorizer {

    fun stem(word: String): String {
        return word.lowercase()
            .removeSuffix("a")
            .removeSuffix("i")
            .removeSuffix("e")
            .removeSuffix("u")
            .removeSuffix("ji")
            .removeSuffix("je")
            .removeSuffix("ov")
            .removeSuffix("ski")
            .removeSuffix("ni")
            .removeSuffix("ni")
    }

    fun categorizeByText(item: NewsItem): String? {
        val text = "${item.heading} ${item.content}".lowercase()
            .replace(Regex("[^a-zčšžđć ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { stem(it) }

        val scores = mutableMapOf<String, Int>()

        for ((category, keywords) in CategoryRules.categoryKeywords) {
            val stemmedKeywords = keywords.map { stem(it) }
            val score = text.groupingBy { it }.eachCount()
                .filterKeys { it in stemmedKeywords }
                .values.sum()
            if (score > 0) scores[category] = score
        }

        val sorted = scores.entries.sortedByDescending { it.value }
        return sorted.firstOrNull()?.key ?: "splošno"
    }

    fun extractLocationByText(item: NewsItem): String {
        val text = "${item.heading} ${item.content}".lowercase()
            .replace(Regex("[^a-zčšžđć ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { stem(it) }

        val scores = mutableMapOf<String, Int>()

        for ((location, keywords) in LocationRules.locationKeywords) {
            val stemmedKeywords = keywords.map { stem(it) }
            val score = text.groupingBy { it }.eachCount()
                .filterKeys { it in stemmedKeywords }
                .values.sum()
            if (score > 0) scores[location] = score
        }

        val bestMatch = scores.entries
            .filter { it.key != "splošno" }
            .maxByOrNull { it.value }

        return bestMatch?.key
            ?: LocationRules.locationKeywords.keys
                .filter { it != "splošno" }
                .random()
    }

    fun categorizeSmart(tags: List<String>): String {
        val stemmedTags = tags.map { stem(it) }

        val scores = mutableMapOf<String, Int>()

        for ((category, keywords) in CategoryRules.categoryKeywords) {
            val stemmedKeywords = keywords.map { stem(it) }
            val matchCount = stemmedTags.count { it in stemmedKeywords }
            if (matchCount > 0) scores[category] = matchCount
        }

        return scores.entries.maxByOrNull { it.value }?.key ?: "splošno"
    }

}

package org.example.nlp

object KeywordExtractor {

    val stopWords = setOf(
        "in", "na", "je", "da", "se", "za", "z", "ki", "so", "bo", "smo", "po", "ob", "kot", "od",
        "ga", "bodo", "bi", "pa", "tudi", "če", "ali", "le", "pri", "že", "ko", "ni", "iz", "o", "v",
        "še", "s", "do", "ne", "da", "ja", "sem", "mi", "to", "tja", "tu", "tam", "kaj", "kdo", "kjer",
        "kateri", "katere", "katero", "ki", "kar", "nihče", "nič", "nekaj", "vsak", "vsaka", "vsako",
        "vse", "vsi", "bil", "bila", "bili", "bile", "bilo", "biti", "imam", "ima", "imajo", "imaš",
        "bomo", "boste", "bodi", "bila", "bil", "lahko", "mora", "morajo", "moraš", "more", "naj",
        "najbolj", "bilo", "bile", "kjer", "tisti", "tista", "tisto", "tako", "tak", "taka", "potem"
    )

    fun extractTopWords(text: String, topN: Int = 5): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-zčšžđć ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in stopWords }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(topN)
            .map { it.key }
    }
}
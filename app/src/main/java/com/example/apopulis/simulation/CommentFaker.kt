package com.example.apopulis.simulation

import kotlin.random.Random

object CommentTextGenerator {

    private val words = listOf(
        "lorem","ipsum","dolor","sit","amet","consectetur","adipiscing","elit",
        "sed","do","eiusmod","tempor","incididunt","ut","labore","et","dolore",
        "magna","aliqua","ut","enim","ad","minim","veniam","quis","nostrud",
        "exercitation","ullamco","laboris","nisi","ut","aliquip","ex","ea",
        "commodo","consequat","duis","aute","irure","dolor","in","reprehenderit"
    )

    fun generate(minWords: Int = 6, maxWords: Int = 18): String {
        val count = Random.nextInt(minWords.coerceAtLeast(1), maxWords.coerceAtLeast(minWords) + 1)
        val text = (1..count).joinToString(" ") { words.random() }
        return text.replaceFirstChar { it.uppercase() } + "."
    }
}

package ui.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.example.model.NewsItem
import java.io.File

object Storage {
    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JavaTimeModule())

    private val file = File("saved_news.json")

    fun save(news: List<NewsItem>) {
        mapper.writeValue(file, news)
    }

    fun load(): List<NewsItem> {
        return if (file.exists()) {
            mapper.readValue(file)
        } else emptyList()
    }
}

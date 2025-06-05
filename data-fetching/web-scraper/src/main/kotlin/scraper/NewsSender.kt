package scraper

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import model.NewsItem
import java.io.IOException

object NewsSender {
    private val gson = Gson()
    private val client = OkHttpClient()
    private const val API_URL = "http://localhost:3001/api/news"

    fun send(newsItem: NewsItem) {
        val json = Gson().toJson(mapOf(
            "title" to newsItem.title,
            "summary" to newsItem.content.take(200),
            "content" to newsItem.content,
            "author" to newsItem.author,
            "source" to newsItem.source,
            "url" to newsItem.url,
            "publishedAt" to newsItem.publishedAt.toString(),
            "imageUrl" to newsItem.imageUrl,
            "category" to newsItem.category,
            "location" to newsItem.location,
            "tags" to newsItem.tags
        ))

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://localhost:3000/api/news")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                printServerResponse(response, newsItem.title)
            }
        } catch (e: IOException) {
            println("Error while sending '${newsItem.title}': ${e.message}")
        }
    }

    private fun printServerResponse(response: Response, title: String) {
        val body = response.body?.string()
        if (!response.isSuccessful) {
            println("Error [${response.code}]: $body")
        } else {
            println("Successfully sent: $title")
        }
    }

}

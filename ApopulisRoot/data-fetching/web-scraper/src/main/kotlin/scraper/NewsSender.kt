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
    private const val API_URL = "http://localhost:5001/api/news"

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
            .url("http://localhost:5001/api/news")
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

    fun update(newsItem: NewsItem) {
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

        val id = newsItem.id ?: return println("Cannot update: missing MongoDB ObjectId")

        val request = Request.Builder()
            .url("http://localhost:5001/api/news/$id") // PUT zahteva ID u URL-u
            .put(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                printServerResponse(response, newsItem.title)
            }
        } catch (e: IOException) {
            println("Error while updating '${newsItem.title}': ${e.message}")
        }
    }

    fun delete(newsItem: NewsItem) {
        if (newsItem.id == null) {
            println("❌ Cannot delete from backend: missing ID")
            return
        }
        val id = newsItem.id


        val request = Request.Builder()
            .url("http://localhost:5001/api/news/$id")
            .delete()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    println("Delete error [${response.code}]: $body")
                } else {
                    println("✅ Deleted: ${newsItem.title}")
                }
            }
        } catch (e: IOException) {
            println("Error deleting '${newsItem.title}': ${e.message}")
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

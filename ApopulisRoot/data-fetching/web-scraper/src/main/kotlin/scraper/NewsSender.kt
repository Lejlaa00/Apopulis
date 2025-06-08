package scraper

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import model.NewsItem
import java.io.IOException
import org.json.JSONObject
import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.OffsetDateTime


object NewsSender {
    private val client = OkHttpClient()
    private const val API_URL = "http://localhost:5001/api/news"
    private val sentItems = mutableListOf<NewsItem>()

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, object : JsonDeserializer<LocalDateTime> {
            override fun deserialize(
                json: JsonElement, typeOfT: Type, context: JsonDeserializationContext
            ): LocalDateTime {
                return OffsetDateTime.parse(json.asString).toLocalDateTime()
            }
        })
        .registerTypeAdapter(LocalDateTime::class.java, object : JsonSerializer<LocalDateTime> {
            override fun serialize(
                src: LocalDateTime, typeOfSrc: Type, context: JsonSerializationContext
            ): JsonElement {
                return JsonPrimitive(src.toString())
            }
        })
        .create()


    // Fetches all news items from the backend API and updates the local `sentItems` list
    fun fetchAllNews(): List<NewsItem> {
        val request = Request.Builder()
            .url("http://localhost:5001/api/news")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    println("Failed to fetch news: ${response.code}")
                    emptyList()
                } else {
                    val jsonObject = JSONObject(body)
                    val newsArray = jsonObject.getJSONArray("news").toString()
                    val listType = object : com.google.gson.reflect.TypeToken<List<NewsItem>>() {}.type

                    val newsList = gson.fromJson<List<NewsItem>>(newsArray, listType)
                    // Reload items
                    sentItems.clear()
                    sentItems.addAll(newsList)

                    newsList
                }
            }
        } catch (e: IOException) {
            println("Error while fetching news: ${e.message}")
            emptyList()
        }
    }


    //Sends a new news item to the backend and adds it to the local `sentItems` list if successful
    fun send(newsItem: NewsItem) {
        val json = Gson().toJson(
            mapOf(
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
            )
        )

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://localhost:5001/api/news")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val jsonResponse = JSONObject(body)
                    val newId = jsonResponse.getString("_id")
                    newsItem.id = newId
                    sentItems.add(newsItem)
                    println("Sent and saved with ID = $newId")
                } else {
                    println("Failed to send: $body")
                }
            }
        } catch (e: IOException) {
            println("Error while sending '${newsItem.title}': ${e.message}")
            throw e
        }
    }


    suspend fun sendAsync(newsItem: NewsItem): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val json = Gson().toJson(
            mapOf(
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
            )
        )

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val jsonResponse = JSONObject(body)
                    val newId = jsonResponse.getString("_id")
                    newsItem.id = newId
                    sentItems.add(newsItem)
                    println("Sent and saved with ID = $newId")
                    true
                } else {
                    println("Failed to send: $body")
                    false
                }
            }
        } catch (e: IOException) {
            println("Error while sending '${newsItem.title}': ${e.message}")
            false
        }
    }



    // Finds a previously sent item by its ID from the `sentItems` list
    fun findItemById(id: String): NewsItem? {
        return sentItems.find { it.id == id }
    }


    //Updates an existing news item on the backend by its ID with new data
    fun update(newsItem: NewsItem) {
        val existingItem = if (newsItem.id != null) findItemById(newsItem.id!!) else null

        if (existingItem == null) {
            println("Item not found in sentItems. Current ID: ${newsItem.id}")
            println("Checking if item exists in sentItems by title: '${newsItem.title}'")
            sentItems.forEach { println(" - ${it.id}: ${it.title}") }
            return
        }

        val id = existingItem.id ?: run {
            println("Critical error: Item in sentItems has null ID!")
            sentItems.remove(existingItem)
            return
        }

        /* println("Attempting to update item with ID: $id")
         println("Item details before update:")
         println(" - Title: ${existingItem.title}")
         println(" - Content: ${existingItem.content.take(30)}...")*/

        val json = gson.toJson(
            mapOf(
                "title" to newsItem.title,
                "summary" to newsItem.content.take(200),
                "content" to newsItem.content,
                "author" to newsItem.author,
                "source" to (newsItem.source?.takeIf { it.isNotBlank() } ?: "24ur"),
                "url" to newsItem.url,
                "publishedAt" to newsItem.publishedAt.toString(),
                "imageUrl" to (newsItem.imageUrl ?: "http://localhost:5001/images/default-image.jpg"),
                "category" to (newsItem.category ?: "splošno"),
                "location" to newsItem.location,
                "tags" to newsItem.tags
            )
        )

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$API_URL/$id")
            .put(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Update failed [${response.code}]: ${response.body?.string()}")
                } else {
                    println("Updated item with ID: $id")
                    response.body?.string()?.let { body ->
                        JSONObject(body).optString("_id").takeIf { it.isNotEmpty() }?.let {
                            existingItem.id = it
                        }
                    }
                }
            }
        } catch (e: IOException) {
            println("Error while updating '${existingItem.title}': ${e.message}")
        }
    }


    // Deletes a news item from the backend using its ID
    fun delete(newsItem: NewsItem) {
        if (newsItem.id == null) {
            println("Cannot delete from backend: missing ID")
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
                    println("Deleted: ${newsItem.title}")
                }
            }
        } catch (e: IOException) {
            println("Error deleting '${newsItem.title}': ${e.message}")
        }
    }

}

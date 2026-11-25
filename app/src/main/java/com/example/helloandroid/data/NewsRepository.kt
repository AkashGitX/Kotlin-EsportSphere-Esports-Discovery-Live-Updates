package com.example.helloandroid.data

import com.example.helloandroid.SupabaseProjectUrl
import com.example.helloandroid.SupabasePublishableKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

data class HomeNewsItem(
    val id: String,
    val title: String,
    val summary: String? = null,
    val content: String? = null,
    val author: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null,
)

sealed interface HomeNewsResult {
    data class Success(val stories: List<HomeNewsItem>) : HomeNewsResult
    data class Failure(val message: String) : HomeNewsResult
}

private object HomeNewsMemoryCache {
    private const val MaxAgeMillis = 90_000L
    private data class Entry(val result: HomeNewsResult.Success, val savedAt: Long)
    private val entries = ConcurrentHashMap<String, Entry>()

    fun get(key: String): HomeNewsResult.Success? = entries[key]
        ?.takeIf { System.currentTimeMillis() - it.savedAt < MaxAgeMillis }
        ?.result

    fun put(key: String, result: HomeNewsResult.Success) { entries[key] = Entry(result, System.currentTimeMillis()) }
}

/** Read-only Home news queries using the same publishable-key Data API pattern as the directories. */
class NewsRepository {
    suspend fun trending(): HomeNewsResult = query("trending", "is_trending=eq.true&order=created_at.desc&limit=8")
    suspend fun featured(): HomeNewsResult = query("featured", "is_featured=eq.true&order=created_at.desc&limit=8")
    suspend fun latest(): HomeNewsResult {
        val cutoff = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
        return query("latest", "created_at=gte.$cutoff&order=created_at.desc&limit=8")
    }

    private suspend fun query(cacheKey: String, filters: String): HomeNewsResult = withContext(Dispatchers.IO) {
        HomeNewsMemoryCache.get(cacheKey)?.let { return@withContext it }
        runCatching {
            val connection = (URL("$SupabaseProjectUrl/rest/v1/news?select=*&$filters").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("apikey", SupabasePublishableKey)
                setRequestProperty("Authorization", "Bearer $SupabasePublishableKey")
            }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) HomeNewsResult.Failure("News query failed (HTTP ${connection.responseCode}): $response")
            else HomeNewsResult.Success(JSONArray(response).let { rows ->
                List(rows.length()) { index -> rows.getJSONObject(index).toHomeNewsItem() }
            }).also { HomeNewsMemoryCache.put(cacheKey, it) }
        }.getOrElse { error -> HomeNewsResult.Failure("News query failed: ${error.message ?: error.javaClass.simpleName}") }
    }
}

private fun JSONObject.toHomeNewsItem(): HomeNewsItem {
    fun firstText(vararg names: String): String? = names.firstNotNullOfOrNull { name ->
        optString(name).takeIf { it.isNotBlank() && it != "null" }
    }
    return HomeNewsItem(
        id = firstText("id", "slug") ?: hashCode().toString(),
        title = firstText("title", "headline", "name") ?: "Untitled update",
        summary = firstText("summary", "description", "excerpt"),
        content = firstText("content", "body", "article_content", "article"),
        author = firstText("author", "author_name", "writer", "published_by"),
        category = firstText("category", "game", "tag"),
        imageUrl = firstText("image_url", "thumbnail_url", "cover_url", "banner_url"),
        createdAt = firstText("created_at", "published_at"),
    )
}

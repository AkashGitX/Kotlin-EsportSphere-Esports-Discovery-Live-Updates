package com.example.helloandroid.data

import com.example.helloandroid.SupabaseProjectUrl
import com.example.helloandroid.SupabasePublishableKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** A tolerant read model for the existing public event_profiles view. */
data class EventConnectionRow(
    val id: String,
    val name: String,
    val shortName: String? = null,
    val status: String? = null,
    val currentRound: String? = null,
    val game: String? = null,
    val organizer: String? = null,
    val location: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
)

sealed interface EventsConnectionResult {
    data class Success(val events: List<EventConnectionRow>) : EventsConnectionResult
    data class Failure(val message: String) : EventsConnectionResult
}

private object EventDirectoryMemoryCache {
    private const val MaxAgeMillis = 90_000L
    private data class Entry(val events: List<EventConnectionRow>, val storedAt: Long)
    private val entries = ConcurrentHashMap<String, Entry>()

    fun get(key: String): List<EventConnectionRow>? = entries[key]
        ?.takeIf { System.currentTimeMillis() - it.storedAt < MaxAgeMillis }
        ?.events

    fun put(key: String, events: List<EventConnectionRow>) {
        entries[key] = Entry(events, System.currentTimeMillis())
    }
}

/** Uses the app's existing publishable-key REST client pattern; no privileged credentials. */
class EventsRepository {
    suspend fun getEvents(): EventsConnectionResult = withContext(Dispatchers.IO) {
        val cacheKey = "directory"
        EventDirectoryMemoryCache.get(cacheKey)?.let { return@withContext EventsConnectionResult.Success(it) }

        runCatching {
            // The established view provides event data without Android-side joins. `select=*`
            // avoids assuming optional view columns while the app is being expanded.
            val response = request("event_profiles?select=*&limit=100")
            if (response.first !in 200..299) {
                error("Events query failed (HTTP ${response.first}): ${response.second}")
            }
            val events = JSONArray(response.second).let { rows ->
                List(rows.length()) { index -> rows.getJSONObject(index).toEventConnectionRow() }
            }
            EventDirectoryMemoryCache.put(cacheKey, events)
            EventsConnectionResult.Success(events)
        }.getOrElse { error ->
            EventsConnectionResult.Failure(error.message ?: "Unable to load tournaments.")
        }
    }

    private fun request(path: String): Pair<Int, String> {
        val connection = (URL("$SupabaseProjectUrl/rest/v1/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", SupabasePublishableKey)
            setRequestProperty("Authorization", "Bearer $SupabasePublishableKey")
        }
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        return code to body
    }
}

private fun JSONObject.toEventConnectionRow(): EventConnectionRow {
    val name = firstString("name", "display_name", "event_name", "title") ?: "Tournament"
    return EventConnectionRow(
        id = firstString("id") ?: name,
        name = name,
        shortName = firstString("short_name", "event_short_name"),
        status = firstString("status", "event_status"),
        currentRound = firstString("current_round", "round", "stage_name", "current_stage"),
        game = firstString("game", "game_name"),
        organizer = firstString("organizer", "organizer_name"),
        location = firstString("location", "city", "country"),
        startDate = firstString("start_date", "starts_at", "start_at"),
        endDate = firstString("end_date", "ends_at", "end_at"),
        description = firstString("description", "overview", "bio"),
        logoUrl = firstString("logo_url", "image_url"),
        bannerUrl = firstString("banner_url", "cover_url", "image_url"),
    )
}

private fun JSONObject.firstString(vararg names: String): String? = names.firstNotNullOfOrNull { name ->
    optString(name).takeIf { it.isNotBlank() && it != "null" }
}

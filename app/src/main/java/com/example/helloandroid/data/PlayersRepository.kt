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

/**
 * A directory row from the existing `players` table.
 *
 * `id` is the verified required column. The descriptive fields are optional because
 * the currently RLS-visible dataset is empty; when present they are read directly
 * from the table response without requiring any schema changes.
 */
data class PlayerConnectionRow(
    val id: String,
    val ign: String? = null,
    val displayName: String? = null,
    val realName: String? = null,
    val country: String? = null,
    val region: String? = null,
    val role: String? = null,
    val teamName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
)

sealed interface PlayersConnectionResult {
    data class Success(val players: List<PlayerConnectionRow>, val totalCount: Int?) : PlayersConnectionResult
    data class Failure(val message: String) : PlayersConnectionResult
}

/**
 * A small process-lifetime, stale-while-revalidate cache for the player directory.
 * Pages are intentionally short-lived: a newly opened directory is immediate, while
 * a later visit rechecks Supabase and replaces the cached page if its data changed.
 */
private object PlayerDirectoryMemoryCache {
    private const val MaxAgeMillis = 90_000L
    private data class Entry(val result: PlayersConnectionResult.Success, val storedAt: Long)
    private val pages = ConcurrentHashMap<String, Entry>()

    fun get(key: String): PlayersConnectionResult.Success? = pages[key]
        ?.takeIf { System.currentTimeMillis() - it.storedAt < MaxAgeMillis }
        ?.result

    fun put(key: String, result: PlayersConnectionResult.Success) {
        pages[key] = Entry(result, System.currentTimeMillis())
    }
}

/**
 * A UI-independent Data API repository.
 *
 * It deliberately reuses the project's existing direct HTTP configuration and
 * publishable key. The request is anonymous, therefore Supabase RLS remains in effect.
 */
class PlayersRepository {
    suspend fun getPlayers(limit: Int = 25, offset: Int = 0): PlayersConnectionResult = withContext(Dispatchers.IO) {
        runCatching {
            val safeLimit = limit.coerceIn(1, 25)
            val safeOffset = offset.coerceAtLeast(0)
            val cacheKey = "$safeLimit:$safeOffset"
            PlayerDirectoryMemoryCache.get(cacheKey)?.let { return@withContext it }
            val connection = (URL("$SupabaseProjectUrl/rest/v1/players?select=*,teams(name,short_name,display_name)&order=id.asc&limit=$safeLimit&offset=$safeOffset").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("apikey", SupabasePublishableKey)
                setRequestProperty("Authorization", "Bearer $SupabasePublishableKey")
                setRequestProperty("Prefer", "count=exact")
            }
            val responseText = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (connection.responseCode !in 200..299) {
                PlayersConnectionResult.Failure("Players query failed (HTTP ${connection.responseCode}): $responseText")
            } else {
                val rows = JSONArray(responseText).let { response ->
                    List(response.length()) { index ->
                        response.getJSONObject(index).toPlayerConnectionRow()
                    }
                }
                PlayersConnectionResult.Success(rows, connection.getHeaderField("Content-Range").totalCountOrNull()).also {
                    PlayerDirectoryMemoryCache.put(cacheKey, it)
                }
            }
        }.getOrElse { error ->
            PlayersConnectionResult.Failure("Players query failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    /** Warm the next page while the current page is being read. Never blocks the UI. */
    suspend fun prefetchPlayers(limit: Int = 25, offset: Int) {
        getPlayers(limit, offset)
    }
}

private fun JSONObject.toPlayerConnectionRow(): PlayerConnectionRow {
    val relatedTeam = optJSONObject("teams")
    return PlayerConnectionRow(
        id = getString("id"),
        ign = optionalString("ign"),
        displayName = optionalString("display_name"),
        realName = optionalString("real_name"),
        country = optionalString("country"),
        region = optionalString("region"),
        role = optionalString("player_role") ?: optionalString("role"),
        avatarUrl = optionalString("avatar_url"),
        bio = optionalString("bio"),
        teamName = relatedTeam?.optionalString("display_name")
            ?: relatedTeam?.optionalString("name")
            ?: relatedTeam?.optionalString("short_name")
            ?: optionalString("current_team_name")
            ?: optionalString("team_name")
            ?: optionalString("team"),
    )
}

private fun JSONObject.optionalString(name: String): String? =
    optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun String?.totalCountOrNull(): Int? =
    this?.substringAfter('/', missingDelimiterValue = "")?.toIntOrNull()

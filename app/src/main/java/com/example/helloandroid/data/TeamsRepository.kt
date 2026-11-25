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

/** A compact directory representation of a row from the existing public `teams` table. */
data class TeamConnectionRow(
    val id: String,
    val name: String? = null,
    val shortName: String? = null,
    val displayName: String? = null,
    val country: String? = null,
    val region: String? = null,
    val logoUrl: String? = null,
    val status: String? = null,
    val organizationId: String? = null,
    val isVerified: Boolean = false,
    val profileSlug: String? = null,
    val bannerUrl: String? = null,
)

sealed interface TeamsConnectionResult {
    data class Success(val teams: List<TeamConnectionRow>, val totalCount: Int?) : TeamsConnectionResult
    data class Failure(val message: String) : TeamsConnectionResult
}

private object TeamDirectoryMemoryCache {
    private const val MaxAgeMillis = 90_000L
    private data class Entry(val result: TeamsConnectionResult.Success, val storedAt: Long)
    private val pages = ConcurrentHashMap<String, Entry>()

    fun get(key: String): TeamsConnectionResult.Success? = pages[key]
        ?.takeIf { System.currentTimeMillis() - it.storedAt < MaxAgeMillis }
        ?.result

    fun put(key: String, result: TeamsConnectionResult.Success) {
        pages[key] = Entry(result, System.currentTimeMillis())
    }
}

/** Reuses the app's existing direct Data API configuration and publishable key. */
class TeamsRepository {
    suspend fun getTeams(limit: Int = 25, offset: Int = 0): TeamsConnectionResult = withContext(Dispatchers.IO) {
        runCatching {
            val safeLimit = limit.coerceIn(1, 25)
            val safeOffset = offset.coerceAtLeast(0)
            val cacheKey = "$safeLimit:$safeOffset"
            TeamDirectoryMemoryCache.get(cacheKey)?.let { return@withContext it }
            val select = "id,name,short_name,display_name,country,region,logo_url,status,organization_id,is_verified,profile_slug,banner_url"
            val connection = (URL("$SupabaseProjectUrl/rest/v1/teams?select=$select&order=name.asc&limit=$safeLimit&offset=$safeOffset").openConnection() as HttpURLConnection).apply {
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
                TeamsConnectionResult.Failure("Teams query failed (HTTP ${connection.responseCode}): $responseText")
            } else {
                val teams = JSONArray(responseText).let { payload ->
                    List(payload.length()) { index -> payload.getJSONObject(index).toTeamConnectionRow() }
                }
                TeamsConnectionResult.Success(teams, connection.getHeaderField("Content-Range").totalCountOrNull()).also {
                    TeamDirectoryMemoryCache.put(cacheKey, it)
                }
            }
        }.getOrElse { error ->
            TeamsConnectionResult.Failure("Teams query failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    suspend fun prefetchTeams(limit: Int = 25, offset: Int) {
        getTeams(limit, offset)
    }
}

private fun JSONObject.toTeamConnectionRow() = TeamConnectionRow(
    id = getString("id"),
    name = optionalString("name"),
    shortName = optionalString("short_name"),
    displayName = optionalString("display_name"),
    country = optionalString("country"),
    region = optionalString("region"),
    logoUrl = optionalString("logo_url"),
    status = optionalString("status"),
    organizationId = optionalString("organization_id"),
    isVerified = optBoolean("is_verified", false),
    profileSlug = optionalString("profile_slug"),
    bannerUrl = optionalString("banner_url"),
)

private fun JSONObject.optionalString(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }
private fun String?.totalCountOrNull(): Int? = this?.substringAfter('/', missingDelimiterValue = "")?.toIntOrNull()

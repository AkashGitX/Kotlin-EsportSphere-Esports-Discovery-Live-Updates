package com.example.helloandroid.data

import android.content.Context
import com.example.helloandroid.SupabaseProjectUrl
import com.example.helloandroid.SupabasePublishableKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class HomeCarouselItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val label: String? = null,
    val buttonText: String? = null,
    val buttonUrl: String? = null,
    val displayOrder: Int? = null,
    val startAt: String? = null,
    val endAt: String? = null,
    val localImagePath: String? = null,
)

sealed interface HomeCarouselResult {
    data class Success(val items: List<HomeCarouselItem>) : HomeCarouselResult
    data class Failure(val message: String) : HomeCarouselResult
}

/**
 * Read-only home-carousel query using the app's existing publishable-key REST setup.
 * Images live in app files storage rather than cache storage, so Android will not evict
 * them as temporary cache. Stale files are removed only after a replacement set has been
 * fetched and cached successfully.
 */
class HomeCarouselRepository {
    suspend fun cachedActive(context: Context): List<HomeCarouselItem> = withContext(Dispatchers.IO) {
        val manifest = File(context.filesDir, "home_carousel_manifest.json")
        runCatching {
            val items = JSONArray(manifest.readText()).let { rows ->
                List(rows.length()) { index -> rows.getJSONObject(index).toHomeCarouselItem() }
            }.filter(HomeCarouselItem::isActiveNow)
            items.takeIf { cached -> cached.all { item -> item.imageUrl.isNullOrBlank() || item.localImagePath?.let(::File)?.isFile == true } }.orEmpty()
        }.getOrDefault(emptyList())
    }

    suspend fun active(context: Context): HomeCarouselResult = withContext(Dispatchers.IO) {
        runCatching {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
            val query = buildString {
                append("select=id,title,subtitle,description,image_url,label,button_text,button_url,display_order,is_active,start_at,end_at")
                append("&is_active=eq.true")
                append("&or=(start_at.is.null,start_at.lte.$now)")
                append("&or=(end_at.is.null,end_at.gte.$now)")
                append("&order=display_order.asc.nullslast")
            }
            val connection = (URL("$SupabaseProjectUrl/rest/v1/home_carousel?$query").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("apikey", SupabasePublishableKey)
                setRequestProperty("Authorization", "Bearer $SupabasePublishableKey")
            }
            val responseCode = connection.responseCode
            val response = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            check(responseCode in 200..299) { "Home carousel query failed (HTTP $responseCode)" }
            val items = JSONArray(response).let { rows ->
                List(rows.length()) { index -> rows.getJSONObject(index).toHomeCarouselItem() }
            }
            val imageCache = cacheActiveImages(context, items)
            if (imageCache.complete) {
                writeManifest(context, imageCache.items)
                HomeCarouselResult.Success(imageCache.items)
            } else {
                // Keep the last fully cached set on-screen until replacement assets are ready.
                val previous = cachedActive(context)
                HomeCarouselResult.Success(previous.ifEmpty { imageCache.items })
            }
        }.getOrElse { error ->
            HomeCarouselResult.Failure(error.message ?: "Unable to load the home carousel.")
        }
    }

    private data class ImageCache(val items: List<HomeCarouselItem>, val complete: Boolean)

    private fun cacheActiveImages(context: Context, items: List<HomeCarouselItem>): ImageCache {
        val directory = File(context.filesDir, "home_carousel_images").apply { mkdirs() }
        var everyNewImageReady = true
        val localFiles = mutableMapOf<String, String>()

        items.forEach { item ->
            val url = item.imageUrl?.takeIf(String::isNotBlank) ?: return@forEach
            val file = File(directory, "${cacheKey(item.id, url)}.img")
            val ready = file.isFile && file.length() > 0L || downloadImage(url, file)
            if (ready) localFiles[item.id] = file.absolutePath else everyNewImageReady = false
        }

        // Never discard an existing carousel image if a replacement has not downloaded.
        if (everyNewImageReady) {
            val activeFiles = localFiles.values.map(::File).map { it.name }.toSet()
            directory.listFiles()?.filter { it.isFile && it.name !in activeFiles }?.forEach(File::delete)
        }
        return ImageCache(items.map { it.copy(localImagePath = localFiles[it.id]) }, everyNewImageReady)
    }

    private fun writeManifest(context: Context, items: List<HomeCarouselItem>) {
        val manifest = JSONArray().apply { items.forEach { put(it.toManifestJson()) } }
        val destination = File(context.filesDir, "home_carousel_manifest.json")
        val temporary = File(context.filesDir, "home_carousel_manifest.json.write")
        temporary.writeText(manifest.toString())
        if (destination.exists()) destination.delete()
        check(temporary.renameTo(destination))
    }

    private fun downloadImage(url: String, destination: File): Boolean = runCatching {
        val temporary = File(destination.parentFile, "${destination.name}.download")
        temporary.delete()
        val connection = URL(url).openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        connection.getInputStream().use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
        check(temporary.length() > 0L)
        if (destination.exists()) destination.delete()
        check(temporary.renameTo(destination))
    }.isSuccess

    private fun cacheKey(id: String, imageUrl: String): String = MessageDigest.getInstance("SHA-256")
        .digest("$id|$imageUrl".toByteArray())
        .joinToString("") { "%02x".format(it) }
}

private fun JSONObject.toHomeCarouselItem(): HomeCarouselItem {
    fun text(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }
    return HomeCarouselItem(
        id = text("id") ?: hashCode().toString(),
        title = text("title") ?: "Featured update",
        subtitle = text("subtitle"),
        description = text("description"),
        imageUrl = text("image_url"),
        label = text("label"),
        buttonText = text("button_text"),
        buttonUrl = text("button_url"),
        displayOrder = if (has("display_order") && !isNull("display_order")) optInt("display_order") else null,
        startAt = text("start_at"),
        endAt = text("end_at"),
        localImagePath = text("local_image_path"),
    )
}

private fun HomeCarouselItem.toManifestJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("subtitle", subtitle)
    .put("description", description)
    .put("image_url", imageUrl)
    .put("label", label)
    .put("button_text", buttonText)
    .put("button_url", buttonUrl)
    .put("display_order", displayOrder)
    .put("start_at", startAt)
    .put("end_at", endAt)
    .put("local_image_path", localImagePath)

private fun HomeCarouselItem.isActiveNow(): Boolean {
    val now = System.currentTimeMillis()
    return (startAt.toSupabaseTimeMillis() ?: Long.MIN_VALUE) <= now && (endAt.toSupabaseTimeMillis() ?: Long.MAX_VALUE) >= now
}

private fun String?.toSupabaseTimeMillis(): Long? = this?.let { timestamp ->
    listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX").firstNotNullOfOrNull { pattern ->
        runCatching { SimpleDateFormat(pattern, Locale.US).parse(timestamp)?.time }.getOrNull()
    }
}

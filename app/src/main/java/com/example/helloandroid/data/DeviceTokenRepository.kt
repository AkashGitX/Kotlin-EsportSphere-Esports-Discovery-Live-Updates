package com.example.helloandroid.data

import com.example.helloandroid.SupabaseProjectUrl
import com.example.helloandroid.SupabasePublishableKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Authenticated, RLS-scoped access to public.user_devices. */
internal class DeviceTokenRepository {
    suspend fun upsert(
        accessToken: String,
        userId: String,
        fcmToken: String,
        deviceName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL("$SupabaseProjectUrl/rest/v1/user_devices?on_conflict=fcm_token").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("apikey", SupabasePublishableKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
            }
            val payload = JSONObject()
                .put("user_id", userId)
                .put("fcm_token", fcmToken)
                .put("device_type", "android")
                .put("device_name", deviceName)
                .put("is_active", true)
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            connection.disconnect()
            check(responseCode in 200..299) { "user_devices upsert failed (HTTP $responseCode)" }
        }
    }

    suspend fun removeCurrentToken(accessToken: String, userId: String, fcmToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedToken = java.net.URLEncoder.encode(fcmToken, Charsets.UTF_8.name())
            val connection = (URL("$SupabaseProjectUrl/rest/v1/user_devices?user_id=eq.$userId&fcm_token=eq.$encodedToken").openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("apikey", SupabasePublishableKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Prefer", "return=minimal")
            }
            val responseCode = connection.responseCode
            connection.disconnect()
            check(responseCode in 200..299) { "user_devices delete failed (HTTP $responseCode)" }
        }
    }
}

package com.example.helloandroid

import android.content.Context
import android.util.Log
import android.os.Build
import com.example.helloandroid.data.DeviceTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object FcmDeviceSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun synchronize(context: Context, token: String) {
        context.getSharedPreferences(AppPreferencesName, Context.MODE_PRIVATE).edit().putString(PrefFcmToken, token).apply()
        val preferences = context.getSharedPreferences(AppPreferencesName, Context.MODE_PRIVATE)
        val userId = preferences.getString(PrefAccountId, null)
        val accessToken = preferences.getString(PrefAccessToken, null)
        if (userId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            Log.d("EsportSphereFCM", "FCM token obtained; no authenticated Supabase user to sync")
            return
        }
        Log.d("EsportSphereFCM", "Authenticated Supabase user ID found; attempting FCM token sync")
        val deviceName = listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" ").take(120)
        scope.launch {
            DeviceTokenRepository().upsert(accessToken, userId, token, deviceName)
                .onSuccess { Log.i("EsportSphereFCM", "FCM token synchronization succeeded") }
                .onFailure { Log.w("EsportSphereFCM", "FCM token synchronization failed", it) }
        }
    }

    fun removeForCurrentUser(context: Context, accessToken: String?, userId: String?) {
        val token = context.getSharedPreferences(AppPreferencesName, Context.MODE_PRIVATE).getString(PrefFcmToken, null)
        if (accessToken.isNullOrBlank() || userId.isNullOrBlank() || token.isNullOrBlank()) return
        scope.launch {
            DeviceTokenRepository().removeCurrentToken(accessToken, userId, token)
                .onSuccess { Log.i("EsportSphereFCM", "Current user device token removed on logout") }
                .onFailure { Log.w("EsportSphereFCM", "Unable to remove current user device token on logout", it) }
        }
    }
}

class EsportSphereMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("EsportSphereFCM", "FCM token refreshed")
        FcmDeviceSync.synchronize(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val topic = NotificationTopic.fromWireValue(data["topic"])
        val title = data["title"] ?: message.notification?.title ?: "EsportSphere"
        val body = data["body"] ?: data["message"] ?: message.notification?.body ?: return
        EsportSphereNotifications.post(this, topic, title, body)
    }
}

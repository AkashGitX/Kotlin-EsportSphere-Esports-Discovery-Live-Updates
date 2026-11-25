    package com.example.helloandroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** The notification categories exposed in Settings and used by future live-data events. */
internal enum class NotificationTopic(
    internal val preferenceKey: String,
    private val channelId: String,
    private val channelName: String,
    private val defaultEnabled: Boolean,
) {
    MatchReminders(PrefMatchReminders, "match_reminders", "Match reminders", true),
    TournamentUpdates(PrefTournamentUpdates, "tournament_updates", "Tournament updates", true),
    TeamPlayerUpdates(PrefTeamPlayerUpdates, "team_player_updates", "Team and player updates", true),
    News(PrefNewsNotifications, "esports_news", "Esports news", false);

    internal fun channelId() = channelId
    internal fun channelName() = channelName
    internal fun defaultEnabled() = defaultEnabled

    internal companion object {
        fun fromWireValue(value: String?): NotificationTopic = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true) || it.channelId() == value
        } ?: News
    }
}

internal enum class NotificationPostResult {
    Posted,
    PreferenceDisabled,
    PermissionDenied,
    AppNotificationsDisabled,
    ChannelDisabled,
}

/**
 * Central notification gate. Feature code can call [post] without bypassing the
 * user's Settings choice or Android's notification permission.
 */
internal object EsportSphereNotifications {
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        NotificationTopic.entries.forEach { topic ->
            manager.createNotificationChannel(
                NotificationChannel(topic.channelId(), topic.channelName(), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "EsportSphere ${topic.channelName().lowercase()}"
                },
            )
        }
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun post(context: Context, topic: NotificationTopic, title: String, message: String): NotificationPostResult {
        val preferences = context.getSharedPreferences(AppPreferencesName, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(
                topic.preferenceKey,
                topic.defaultEnabled()
            )
        ) return NotificationPostResult.PreferenceDisabled
        if (!hasPermission(context)) return NotificationPostResult.PermissionDenied
        createChannels(context)
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return NotificationPostResult.AppNotificationsDisabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(topic.channelId())
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return NotificationPostResult.ChannelDisabled
        }
        notificationManager.notify(
            "${topic.name}:$title".hashCode(),
            NotificationCompat.Builder(context, topic.channelId())
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build(),
        )
        return NotificationPostResult.Posted
    }
}

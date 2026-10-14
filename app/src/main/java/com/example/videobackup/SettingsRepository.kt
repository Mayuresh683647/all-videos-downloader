package com.example.videobackup

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("video_backup_prefs", Context.MODE_PRIVATE)

    var botToken: String?
        get() = prefs.getString("bot_token", null)
        set(value) = prefs.edit().putString("bot_token", value).apply()

    var chatId: String?
        get() = prefs.getString("chat_id", null)
        set(value) = prefs.edit().putString("chat_id", value).apply()

    var apiUrl: String?
        get() = prefs.getString("api_url", null)
        set(value) = prefs.edit().putString("api_url", value).apply()
}

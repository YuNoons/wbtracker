package com.wbtracker.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wbtracker_prefs", Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK_THEME, true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _syncInterval = MutableStateFlow(prefs.getString(KEY_SYNC_INTERVAL, "3 часа") ?: "3 часа")
    val syncInterval: StateFlow<String> = _syncInterval.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _isDarkTheme.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setSyncInterval(interval: String) {
        prefs.edit().putString(KEY_SYNC_INTERVAL, interval).apply()
        _syncInterval.value = interval
    }

    companion object {
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_SYNC_INTERVAL = "sync_interval"
    }
}

package com.wbtracker.app.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wbtracker.app.data.repository.UserPreferencesRepository
import com.wbtracker.app.domain.repository.SyncScheduler
import com.wbtracker.app.domain.usecase.CalculateMonthlySavingsUseCase
import com.wbtracker.app.domain.usecase.GetTrackedProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class UserProfileStats(
    val trackedCount: Int = 0,
    val totalSavingsRub: Long = 0L,
    val activeAlertsCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    getTrackedProducts: GetTrackedProductsUseCase,
    calculateMonthlySavings: CalculateMonthlySavingsUseCase,
    private val preferencesRepository: UserPreferencesRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    val profileTitle: String = "Профиль WB Tracker"
    val userStatus: String = "Умный мониторинг скидок"

    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabled
    val darkThemeEnabled: StateFlow<Boolean> = preferencesRepository.isDarkTheme
    val syncInterval: StateFlow<String> = preferencesRepository.syncInterval

    val profileStats: StateFlow<UserProfileStats> = getTrackedProducts().map { list ->
        val savings = calculateMonthlySavings(list)
        UserProfileStats(
            trackedCount = list.size,
            totalSavingsRub = savings,
            activeAlertsCount = list.count { it.basicPrice > it.currentPrice && it.basicPrice > 0 }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileStats())

    fun toggleNotifications(enabled: Boolean) {
        preferencesRepository.setNotificationsEnabled(enabled)
    }

    fun toggleDarkTheme(enabled: Boolean) {
        preferencesRepository.setDarkTheme(enabled)
    }

    fun setSyncInterval(interval: String) {
        preferencesRepository.setSyncInterval(interval)
        val hours = interval.filter { it.isDigit() }.toLongOrNull() ?: 6L
        syncScheduler.schedulePeriodicUpdate(hours)
    }
}

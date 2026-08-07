package com.wbtracker.app.ui.theme

import androidx.lifecycle.ViewModel
import com.wbtracker.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = preferencesRepository.isDarkTheme

    fun toggleTheme(isDark: Boolean) {
        preferencesRepository.setDarkTheme(isDark)
    }
}

package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.model.UserSessionRole
import com.example.data.repository.AuthRepository
import com.example.utils.AppLanguage
import com.example.utils.AppPreferences
import com.example.utils.ConnectivityObserver
import com.example.utils.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppPanelMode {
    STUDENT, ADMIN, SUPER_ADMIN
}

class AppViewModel(
    private val authRepository: AuthRepository,
    private val preferences: AppPreferences,
    connectivityObserver: ConnectivityObserver,
    private val currentUserId: String
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val language: StateFlow<AppLanguage> = preferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.BENGALI)

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), connectivityObserver.isCurrentlyOnline())

    val userProfile: StateFlow<UserProfile?> = authRepository.observeUserProfile(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sessionRole: StateFlow<UserSessionRole> = authRepository.observeUserSessionRole(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(UserSessionRole(uid = currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSessionRole(uid = currentUserId))

    private val _panelMode = MutableStateFlow(AppPanelMode.STUDENT)
    val panelMode: StateFlow<AppPanelMode> = _panelMode.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.ensureUserProfile()
        }
    }

    fun switchPanel(target: AppPanelMode) {
        val role = sessionRole.value
        when (target) {
            AppPanelMode.STUDENT -> _panelMode.value = AppPanelMode.STUDENT
            AppPanelMode.ADMIN -> if (role.isAdmin) _panelMode.value = AppPanelMode.ADMIN
            AppPanelMode.SUPER_ADMIN -> if (role.isSuperAdmin) _panelMode.value = AppPanelMode.SUPER_ADMIN
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { preferences.setLanguage(lang) }
    }

    fun updateProfile(name: String, phone: String, langCode: String) {
        viewModelScope.launch {
            val res = authRepository.updateProfile(currentUserId, name, phone, langCode)
            _statusMessage.value = if (res.isSuccess) {
                "প্রোফাইল সফলভাবে হালনাগাদ হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "প্রোফাইল আপডেট ব্যর্থ হয়েছে।"
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            val res = authRepository.uploadProfilePhoto(currentUserId, uri)
            _statusMessage.value = if (res.isSuccess) {
                "প্রোফাইল ছবি আপলোড হয়েছে।"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "ছবি আপলোড ব্যর্থ হয়েছে।"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}

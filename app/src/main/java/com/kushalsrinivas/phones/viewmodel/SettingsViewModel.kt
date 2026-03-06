package com.kushalsrinivas.phones.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kushalsrinivas.phones.security.SecurityConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val securityConfig = SecurityConfig(application)

    private val _botToken = MutableStateFlow("")
    val botToken: StateFlow<String> = _botToken.asStateFlow()

    private val _anthropicApiKey = MutableStateFlow("")
    val anthropicApiKey: StateFlow<String> = _anthropicApiKey.asStateFlow()

    fun setBotToken(token: String) {
        _botToken.value = token
    }

    fun setAnthropicApiKey(key: String) {
        _anthropicApiKey.value = key
    }

    fun setAllowedUserIds(ids: String) {
        viewModelScope.launch {
            securityConfig.setAllowedUserIds(
                ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            )
        }
    }

    fun setAllowedDirectories(dirs: String) {
        viewModelScope.launch {
            securityConfig.setAllowedDirectories(
                dirs.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            )
        }
    }

    fun setRateLimit(limit: Int) {
        viewModelScope.launch {
            securityConfig.setRateLimit(limit)
        }
    }
}

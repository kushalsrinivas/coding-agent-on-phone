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

    private val _agentCommand = MutableStateFlow("node agent/index.js")
    val agentCommand: StateFlow<String> = _agentCommand.asStateFlow()

    private val _botCommand = MutableStateFlow("python bot/main.py")
    val botCommand: StateFlow<String> = _botCommand.asStateFlow()

    fun setBotToken(token: String) {
        _botToken.value = token
    }

    fun setAgentCommand(cmd: String) {
        _agentCommand.value = cmd
    }

    fun setBotCommand(cmd: String) {
        _botCommand.value = cmd
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

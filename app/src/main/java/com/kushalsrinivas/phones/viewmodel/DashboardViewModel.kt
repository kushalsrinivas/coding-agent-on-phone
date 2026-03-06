package com.kushalsrinivas.phones.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kushalsrinivas.phones.bootstrap.BootstrapManager
import com.kushalsrinivas.phones.bootstrap.BootstrapState
import com.kushalsrinivas.phones.ipc.SocketBridge
import com.kushalsrinivas.phones.process.ProcessManager
import com.kushalsrinivas.phones.service.ProcessForegroundService
import com.kushalsrinivas.phones.terminal.TerminalSessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "DashboardVM"
    }

    val bootstrapManager = BootstrapManager(application)
    val sessionManager = TerminalSessionManager(bootstrapManager)
    val processManager = ProcessManager(bootstrapManager, sessionManager)
    val socketBridge = SocketBridge(bootstrapManager.runDir)

    private val _logLines = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _logLines.asStateFlow()

    private val _uptimes = MutableStateFlow<Map<String, String>>(emptyMap())
    val uptimes: StateFlow<Map<String, String>> = _uptimes.asStateFlow()

    private var service: ProcessForegroundService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as ProcessForegroundService.LocalBinder).getService()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    init {
        viewModelScope.launch {
            bootstrapManager.bootstrap()
        }

        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                updateUptimes()
                updateServiceNotification()
            }
        }
    }

    fun addLog(message: String) {
        _logLines.value = (_logLines.value + message).takeLast(500)
    }

    private fun updateUptimes() {
        val now = System.currentTimeMillis()
        _uptimes.value = processManager.processes.value.mapValues { (_, info) ->
            if (info.isRunning && info.startTimeMs > 0) {
                val elapsed = (now - info.startTimeMs) / 1000
                val hours = elapsed / 3600
                val minutes = (elapsed % 3600) / 60
                val seconds = elapsed % 60
                "%02d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "00:00:00"
            }
        }
    }

    private fun updateServiceNotification() {
        val running = processManager.processes.value.count { it.value.isRunning }
        if (running > 0) {
            service?.updateNotification("$running process${if (running > 1) "es" else ""} running")
        }
    }

    fun startForegroundService() {
        val context = getApplication<Application>()
        val intent = Intent(context, ProcessForegroundService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.i(TAG, "Foreground service started and bound")
    }

    fun stopForegroundService() {
        val context = getApplication<Application>()
        try {
            context.unbindService(serviceConnection)
        } catch (_: Exception) {}
        context.stopService(Intent(context, ProcessForegroundService::class.java))
        service = null
        Log.i(TAG, "Foreground service stopped")
    }

    fun startAgent(command: String) {
        socketBridge.ensureRunDir()
        socketBridge.cleanup()

        processManager.startAgent(
            agentCommand = command,
            socketPath = socketBridge.socketPath,
            onTextChanged = { addLog("[agent] output updated") }
        )
        addLog("[system] Agent started")

        val running = processManager.processes.value.count { it.value.isRunning }
        if (running == 1) startForegroundService()
    }

    fun startBot(botToken: String) {
        processManager.startBot(
            botToken = botToken,
            socketPath = socketBridge.socketPath,
            onTextChanged = { addLog("[bot] output updated") }
        )
        addLog("[system] Telegram bot started (pi-tele)")

        val running = processManager.processes.value.count { it.value.isRunning }
        if (running == 1) startForegroundService()
    }

    fun stopAgent() {
        processManager.stopProcess(ProcessManager.AGENT_ID)
        socketBridge.cleanup()
        addLog("[system] Agent stopped")
        stopServiceIfIdle()
    }

    fun stopBot() {
        processManager.stopProcess(ProcessManager.BOT_ID)
        addLog("[system] Telegram bot stopped")
        stopServiceIfIdle()
    }

    fun stopAll() {
        processManager.stopAll()
        socketBridge.cleanup()
        addLog("[system] All processes stopped")
        stopForegroundService()
    }

    private fun stopServiceIfIdle() {
        val running = processManager.processes.value.count { it.value.isRunning }
        if (running == 0) stopForegroundService()
    }

    override fun onCleared() {
        processManager.stopAll()
        socketBridge.cleanup()
        stopForegroundService()
        super.onCleared()
    }
}

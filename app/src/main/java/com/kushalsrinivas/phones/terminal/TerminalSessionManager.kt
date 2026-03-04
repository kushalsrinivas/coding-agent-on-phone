package com.kushalsrinivas.phones.terminal

import android.util.Log
import com.kushalsrinivas.phones.bootstrap.BootstrapManager
import com.kushalsrinivas.phones.process.ProcessConfig
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TerminalSessionManager(private val bootstrapManager: BootstrapManager) {

    companion object {
        private const val TAG = "SessionManager"
        private const val TRANSCRIPT_ROWS = 500
    }

    private val _sessions = MutableStateFlow<Map<String, TerminalSession>>(emptyMap())
    val sessions: StateFlow<Map<String, TerminalSession>> = _sessions.asStateFlow()

    fun createSession(
        id: String,
        config: ProcessConfig,
        onTextChanged: () -> Unit = {},
        onFinished: (TerminalSession) -> Unit = {},
    ): TerminalSession {
        val callback = TerminalSessionCallback(
            onTextChanged = onTextChanged,
            onSessionFinished = { session ->
                Log.i(TAG, "Session '$id' finished")
                onFinished(session)
            }
        )

        val shell = config.shellPath ?: bootstrapManager.let {
            it.binDir.resolve("bash").absolutePath
        }
        val cwd = config.workingDir ?: bootstrapManager.homeDir.absolutePath
        val args = if (config.command != null) {
            arrayOf("-c", config.command)
        } else {
            emptyArray()
        }
        val env = bootstrapManager.getEnvironment() + config.extraEnv

        val session = TerminalSession(
            shell,
            cwd,
            args,
            env,
            TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
            callback
        )

        _sessions.value = _sessions.value + (id to session)
        Log.i(TAG, "Created session '$id': shell=$shell, cmd=${config.command}")
        return session
    }

    fun getSession(id: String): TerminalSession? = _sessions.value[id]

    fun destroySession(id: String) {
        val session = _sessions.value[id] ?: return
        session.finishIfRunning()
        _sessions.value = _sessions.value - id
        Log.i(TAG, "Destroyed session '$id'")
    }

    fun destroyAll() {
        _sessions.value.forEach { (id, session) ->
            session.finishIfRunning()
            Log.i(TAG, "Destroyed session '$id'")
        }
        _sessions.value = emptyMap()
    }

    fun isRunning(id: String): Boolean {
        return _sessions.value[id]?.isRunning == true
    }
}

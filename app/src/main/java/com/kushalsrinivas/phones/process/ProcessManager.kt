package com.kushalsrinivas.phones.process

import android.util.Log
import com.kushalsrinivas.phones.agent.PiAgentManager
import com.kushalsrinivas.phones.bootstrap.BootstrapManager
import com.kushalsrinivas.phones.bot.PiTeleManager
import com.kushalsrinivas.phones.terminal.TerminalSessionManager
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProcessInfo(
    val id: String,
    val label: String,
    val config: ProcessConfig,
    val isRunning: Boolean = false,
    val startTimeMs: Long = 0,
)

class ProcessManager(
    private val bootstrapManager: BootstrapManager,
    private val sessionManager: TerminalSessionManager,
) {
    companion object {
        private const val TAG = "ProcessManager"
        const val AGENT_ID = "agent"
        const val BOT_ID = "bot"
    }

    private val _processes = MutableStateFlow<Map<String, ProcessInfo>>(emptyMap())
    val processes: StateFlow<Map<String, ProcessInfo>> = _processes.asStateFlow()

    fun startProcess(
        id: String,
        label: String,
        config: ProcessConfig,
        onTextChanged: () -> Unit = {},
    ): TerminalSession {
        stopProcess(id)

        val session = sessionManager.createSession(
            id = id,
            config = config,
            onTextChanged = onTextChanged,
            onFinished = {
                _processes.value = _processes.value.toMutableMap().apply {
                    this[id] = this[id]?.copy(isRunning = false) ?: return@apply
                }
            }
        )

        _processes.value = _processes.value + (id to ProcessInfo(
            id = id,
            label = label,
            config = config,
            isRunning = true,
            startTimeMs = System.currentTimeMillis(),
        ))

        Log.i(TAG, "Started process '$id' ($label)")
        return session
    }

    fun stopProcess(id: String) {
        if (sessionManager.isRunning(id)) {
            sessionManager.destroySession(id)
            _processes.value = _processes.value.toMutableMap().apply {
                this[id] = this[id]?.copy(isRunning = false) ?: return@apply
            }
            Log.i(TAG, "Stopped process '$id'")
        }
    }

    fun restartProcess(
        id: String,
        onTextChanged: () -> Unit = {},
    ): TerminalSession? {
        val info = _processes.value[id] ?: return null
        return startProcess(id, info.label, info.config, onTextChanged)
    }

    fun stopAll() {
        _processes.value.keys.toList().forEach { stopProcess(it) }
    }

    fun isRunning(id: String): Boolean = sessionManager.isRunning(id)

    fun getSession(id: String): TerminalSession? = sessionManager.getSession(id)

    /**
     * Starts the coding agent using the `@mariozechner/pi-coding-agent` npm CLI package.
     *
     * The install → run workflow is handled by [PiAgentManager]. The Anthropic API key
     * is passed via the ANTHROPIC_API_KEY environment variable so it never appears in
     * process arguments or logs.
     */
    fun startAgent(
        anthropicApiKey: String,
        socketPath: String,
        onTextChanged: () -> Unit = {},
    ): TerminalSession {
        val config = ProcessConfig(
            command = PiAgentManager.buildAgentCommand(),
            extraEnv = arrayOf(
                "ANTHROPIC_API_KEY=$anthropicApiKey",
                "SOCKET_PATH=$socketPath",
            ),
        )
        return startProcess(AGENT_ID, "Coding Agent", config, onTextChanged)
    }

    /**
     * Starts the Telegram bot using the `pi-tele` npm CLI package.
     *
     * The three-step workflow (install → setup → start) is handled by
     * [PiTeleManager]. The bot token is passed via the TELEGRAM_BOT_TOKEN
     * environment variable so it never appears in process arguments or logs.
     */
    fun startBot(
        botToken: String,
        socketPath: String,
        onTextChanged: () -> Unit = {},
    ): TerminalSession {
        val config = ProcessConfig(
            command = PiTeleManager.buildBotCommand(),
            extraEnv = arrayOf(
                "TELEGRAM_BOT_TOKEN=$botToken",
                "SOCKET_PATH=$socketPath",
            ),
        )
        return startProcess(BOT_ID, "Telegram Bot", config, onTextChanged)
    }
}

package com.kushalsrinivas.phones.bot

/**
 * Manages the lifecycle of the Telegram bot via the `pi-tele` npm CLI package.
 *
 * The three-step workflow executed inside the bot's terminal session:
 *   1. Install pi-tele globally if the binary is not already on PATH.
 *   2. Configure the bot token with `pi-tele setup --token <TOKEN>`.
 *   3. Start the bot server with `pi-tele start`.
 *
 * The entire sequence is expressed as a single bash command so that it runs
 * inside the existing TerminalSession infrastructure without any changes to
 * ProcessManager or TerminalSessionManager.
 */
object PiTeleManager {

    private const val PACKAGE = "pi-tele"
    private const val VERSION = "0.1.0"

    /**
     * Returns a bash command string that:
     *  - Installs [PACKAGE]@[VERSION] globally when `pi-tele` is not found on PATH.
     *  - Runs `pi-tele setup --token TOKEN` to persist the bot token.
     *  - Runs `pi-tele start` to bring the bot server up.
     *
     * The token value is interpolated from the [TELEGRAM_BOT_TOKEN] environment
     * variable that ProcessManager already injects, so the token is never
     * embedded as a literal string in process arguments or logs.
     */
    fun buildBotCommand(): String = """
        set -e
        if ! command -v pi-tele >/dev/null 2>&1; then
            echo "[pi-tele] Installing $PACKAGE@$VERSION globally..."
            npm install -g $PACKAGE@$VERSION
        fi
        echo "[pi-tele] Configuring bot token..."
        pi-tele setup --token "${'$'}TELEGRAM_BOT_TOKEN"
        echo "[pi-tele] Starting bot server..."
        exec pi-tele start
    """.trimIndent()
}

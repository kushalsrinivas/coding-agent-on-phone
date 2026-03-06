package com.kushalsrinivas.phones.agent

/**
 * Manages the lifecycle of the coding agent via the `@mariozechner/pi-coding-agent` npm CLI package.
 *
 * The three-step workflow executed inside the agent's terminal session:
 *   1. Install @mariozechner/pi-coding-agent globally if the `pi` binary is not already on PATH.
 *   2. Export the Anthropic API key so pi can authenticate.
 *   3. Start the agent with `pi`.
 *
 * The entire sequence is expressed as a single bash command so that it runs
 * inside the existing TerminalSession infrastructure without any changes to
 * ProcessManager or TerminalSessionManager.
 */
object PiAgentManager {

    private const val PACKAGE = "@mariozechner/pi-coding-agent"

    /**
     * Returns a bash command string that:
     *  - Installs [PACKAGE] globally when the `pi` binary is not found on PATH.
     *  - Sets ANTHROPIC_API_KEY from the environment variable already injected by ProcessManager.
     *  - Runs `pi` to start the interactive coding agent session.
     *
     * The API key is passed via the ANTHROPIC_API_KEY environment variable so it
     * never appears as a literal in process arguments or logs.
     */
    fun buildAgentCommand(): String = """
        set -e
        if ! command -v pi >/dev/null 2>&1; then
            echo "[pi-agent] Installing $PACKAGE globally..."
            npm install -g $PACKAGE
        fi
        echo "[pi-agent] Starting coding agent..."
        exec pi
    """.trimIndent()
}

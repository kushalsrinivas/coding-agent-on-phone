package com.kushalsrinivas.phones.ipc

import android.util.Log
import java.io.File

/**
 * Manages the Unix domain socket path used for IPC between the coding agent and the Telegram bot.
 * The actual socket server runs inside the Node.js agent process; the Python bot connects to it.
 * This class just manages the filesystem path and ensures the directory exists.
 */
class SocketBridge(private val runDir: File) {

    companion object {
        private const val TAG = "SocketBridge"
        private const val SOCKET_NAME = "agent.sock"
    }

    val socketPath: String get() = File(runDir, SOCKET_NAME).absolutePath

    fun ensureRunDir() {
        if (!runDir.exists()) {
            runDir.mkdirs()
            Log.i(TAG, "Created run directory: ${runDir.absolutePath}")
        }
    }

    fun cleanup() {
        val sockFile = File(runDir, SOCKET_NAME)
        if (sockFile.exists()) {
            sockFile.delete()
            Log.i(TAG, "Cleaned up socket file")
        }
    }
}

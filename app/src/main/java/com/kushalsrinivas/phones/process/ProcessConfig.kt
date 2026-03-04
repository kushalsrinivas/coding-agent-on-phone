package com.kushalsrinivas.phones.process

data class ProcessConfig(
    val shellPath: String? = null,
    val command: String? = null,
    val workingDir: String? = null,
    val extraEnv: Array<String> = emptyArray(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessConfig) return false
        return shellPath == other.shellPath &&
                command == other.command &&
                workingDir == other.workingDir &&
                extraEnv.contentEquals(other.extraEnv)
    }

    override fun hashCode(): Int {
        var result = shellPath?.hashCode() ?: 0
        result = 31 * result + (command?.hashCode() ?: 0)
        result = 31 * result + (workingDir?.hashCode() ?: 0)
        result = 31 * result + extraEnv.contentHashCode()
        return result
    }
}

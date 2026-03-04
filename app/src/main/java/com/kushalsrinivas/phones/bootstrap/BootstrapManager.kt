package com.kushalsrinivas.phones.bootstrap

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URL
import java.util.zip.ZipInputStream

class BootstrapManager(private val context: Context) {

    companion object {
        private const val TAG = "BootstrapManager"
        private const val BOOTSTRAP_FILE = "bootstrap-aarch64.zip"

        // Primary: latest GitHub release. If this 404s on older devices use the SourceForge mirror.
        private val BOOTSTRAP_URLS = listOf(
            "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.03.01-r1%2Bapt.android-7/bootstrap-aarch64.zip",
            "https://sourceforge.net/projects/termux-packages.mirror/files/bootstrap-2026.02.12-r1%2Bapt.android-7/bootstrap-aarch64.zip/download",
            "https://sourceforge.net/projects/termux-packages.mirror/files/bootstrap-2025.07.27-r1%2Bapt.android-7/bootstrap-aarch64.zip/download",
        )
    }

    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.NotStarted)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    val filesDir: File get() = context.filesDir
    val prefixDir: File get() = File(filesDir, "usr")
    val homeDir: File get() = File(filesDir, "home")
    val binDir: File get() = File(prefixDir, "bin")
    val runDir: File get() = File(filesDir, "run")

    val isBootstrapped: Boolean get() = File(binDir, "bash").exists()

    fun getEnvironment(): Array<String> {
        val prefix = prefixDir.absolutePath
        val home = homeDir.absolutePath
        val bin = binDir.absolutePath
        val lib = File(prefixDir, "lib").absolutePath

        return arrayOf(
            "HOME=$home",
            "PREFIX=$prefix",
            "TMPDIR=${File(prefixDir, "tmp").absolutePath}",
            "PATH=$bin:$prefix/bin/applets",
            "LD_LIBRARY_PATH=$lib",
            "LANG=en_US.UTF-8",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "SHELL=$bin/bash",
        )
    }

    suspend fun bootstrap() {
        if (isBootstrapped) {
            _state.value = BootstrapState.Ready
            return
        }

        try {
            download()
            extract()
            setupSymlinks()
            _state.value = BootstrapState.Ready
            Log.i(TAG, "Bootstrap complete")
        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap failed", e)
            _state.value = BootstrapState.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun download() = withContext(Dispatchers.IO) {
        _state.value = BootstrapState.Downloading(0f)
        val zipFile = File(filesDir, BOOTSTRAP_FILE)

        if (zipFile.exists() && zipFile.length() > 1024) {
            Log.i(TAG, "Bootstrap zip already downloaded (${zipFile.length() / 1024}KB)")
            _state.value = BootstrapState.Downloading(1f)
            return@withContext
        }

        var lastError: Exception? = null
        for (url in BOOTSTRAP_URLS) {
            try {
                Log.i(TAG, "Trying bootstrap URL: $url")
                val connection = URL(url).openConnection()
                connection.connectTimeout = 30_000
                connection.readTimeout = 120_000
                connection.setRequestProperty("User-Agent", "PhoneAgent/1.0")

                val responseCode = (connection as? java.net.HttpURLConnection)?.responseCode ?: 200
                if (responseCode == 404) {
                    Log.w(TAG, "404 for $url, trying next")
                    continue
                }

                val totalBytes = connection.contentLengthLong.coerceAtLeast(1)
                var downloadedBytes = 0L

                connection.getInputStream().buffered().use { input ->
                    FileOutputStream(zipFile).buffered().use { output ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            _state.value = BootstrapState.Downloading(
                                (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 0.99f)
                            )
                        }
                    }
                }

                if (zipFile.length() < 1024) {
                    zipFile.delete()
                    throw IOException("Downloaded file too small (${zipFile.length()} bytes), likely an error page")
                }

                Log.i(TAG, "Downloaded ${downloadedBytes / 1024}KB from $url")
                _state.value = BootstrapState.Downloading(1f)
                return@withContext
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download from $url: ${e.message}")
                zipFile.delete()
                lastError = e
            }
        }

        throw IOException("All bootstrap URLs failed. Last error: ${lastError?.message}")
    }

    private suspend fun extract() = withContext(Dispatchers.IO) {
        _state.value = BootstrapState.Extracting
        val zipFile = File(filesDir, BOOTSTRAP_FILE)

        prefixDir.mkdirs()
        homeDir.mkdirs()
        runDir.mkdirs()
        File(prefixDir, "tmp").mkdirs()

        val symlinksTxt = mutableListOf<String>()

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "SYMLINKS.txt") {
                    symlinksTxt.addAll(
                        zis.bufferedReader().readText().lines().filter { it.isNotBlank() }
                    )
                } else {
                    val outFile = File(prefixDir, name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        if (name.startsWith("bin/") || name.startsWith("libexec/")) {
                            outFile.setExecutable(true, false)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Process symlinks from SYMLINKS.txt
        // Format: each line is "target←link" where ← is the actual arrow character,
        // or sometimes "link→target". The Termux bootstrap uses a custom format.
        for (line in symlinksTxt) {
            val parts = line.split("←")
            if (parts.size == 2) {
                val target = parts[0].trim()
                val linkPath = parts[1].trim()
                val linkFile = File(prefixDir, linkPath)
                linkFile.parentFile?.mkdirs()
                try {
                    Runtime.getRuntime().exec(
                        arrayOf("ln", "-sf", target, linkFile.absolutePath)
                    ).waitFor()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create symlink: $linkPath -> $target", e)
                }
            }
        }

        // Clean up the zip
        zipFile.delete()
        Log.i(TAG, "Extraction complete")
    }

    private suspend fun setupSymlinks() = withContext(Dispatchers.IO) {
        // Ensure critical executables are executable
        binDir.listFiles()?.forEach { file ->
            if (file.isFile) file.setExecutable(true, false)
        }

        // Create a basic profile
        val profileFile = File(homeDir, ".profile")
        if (!profileFile.exists()) {
            profileFile.writeText(
                """
                export PREFIX="${prefixDir.absolutePath}"
                export HOME="${homeDir.absolutePath}"
                export PATH="${binDir.absolutePath}:${'$'}PATH"
                export TERM=xterm-256color
                """.trimIndent() + "\n"
            )
        }
    }

    suspend fun installPackages(vararg packages: String) = withContext(Dispatchers.IO) {
        _state.value = BootstrapState.InstallingPackages
        val shell = File(binDir, "bash").absolutePath
        val env = getEnvironment()
        val pkgList = packages.joinToString(" ")

        val process = Runtime.getRuntime().exec(
            arrayOf(shell, "-c", "pkg update -y && pkg install -y $pkgList"),
            env,
            homeDir
        )

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val stderr = process.errorStream.bufferedReader().readText()
            throw IOException("Package installation failed (exit $exitCode): $stderr")
        }

        _state.value = BootstrapState.Ready
        Log.i(TAG, "Installed packages: $pkgList")
    }
}

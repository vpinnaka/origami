package com.origami.assistant.terminal

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sandboxed terminal session for executing skill scripts and code.
 *
 * Uses the device's shell process directly (app's private data dir).
 * For production, replace with a proot-based Linux pod (like Termux
 * architecture) to provide a proper isolated Linux environment with
 * Python/Node/bash. This implementation provides a working foundation
 * using the app process's available executables.
 */
@Singleton
class TerminalSession @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workDir: File = File(context.filesDir, "terminal").also { it.mkdirs() }
    private val scriptsDir: File = File(context.filesDir, "scripts").also { it.mkdirs() }

    private var _available: Boolean? = null

    fun isAvailable(): Boolean {
        if (_available != null) return _available!!
        _available = try {
            val p = ProcessBuilder("sh", "-c", "echo ok").start()
            val result = p.inputStream.bufferedReader().readLine()
            p.waitFor(2, TimeUnit.SECONDS)
            result == "ok"
        } catch (e: Exception) {
            false
        }
        return _available!!
    }

    /**
     * Execute [command] in the sandboxed working directory.
     * Returns stdout+stderr combined. Times out after [timeoutSeconds].
     */
    suspend fun execute(command: String, timeoutSeconds: Int = 30): String =
        withContext(Dispatchers.IO) {
            val result = withTimeoutOrNull(timeoutSeconds * 1000L) {
                try {
                    val process = ProcessBuilder("sh", "-c", command)
                        .directory(workDir)
                        .redirectErrorStream(true)
                        .apply {
                            environment().apply {
                                put("HOME", workDir.absolutePath)
                                put("TMPDIR", context.cacheDir.absolutePath)
                                put("PATH", "/system/bin:/system/xbin")
                            }
                        }
                        .start()

                    val output = process.inputStream.bufferedReader().readText()
                    val exited = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)

                    if (!exited) {
                        process.destroyForcibly()
                        "Process timed out after ${timeoutSeconds}s\n$output"
                    } else {
                        val exitCode = process.exitValue()
                        if (exitCode != 0) "Exit code $exitCode:\n$output" else output
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Terminal execute failed")
                    "Execution error: ${e.message}"
                }
            }
            result ?: "Command timed out after ${timeoutSeconds}s"
        }

    /** Write a script file to the scripts directory */
    fun writeScript(name: String, content: String): File =
        File(scriptsDir, name).also { it.writeText(content) }

    fun getWorkDir() = workDir
    fun getScriptsDir() = scriptsDir
}

package com.origami.assistant.agent.tools

import com.origami.assistant.agent.model.Tool
import com.origami.assistant.agent.model.ToolParameter
import com.origami.assistant.terminal.TerminalSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CodeExecutionTool @Inject constructor(
    private val terminal: TerminalSession
) : BaseTool() {

    override val definition = Tool(
        name = "execute_code",
        description = "Execute Python or shell script code in a sandboxed terminal environment. Use for data processing, file manipulation, or running scripts.",
        parameters = listOf(
            ToolParameter("language", "string", "Programming language: 'python' or 'bash'", required = true),
            ToolParameter("code", "string", "The code to execute", required = true),
            ToolParameter("timeout_seconds", "number", "Execution timeout (max 60)", required = false)
        )
    )

    override suspend fun execute(params: Map<String, Any>): String {
        val language = param(params, "language").lowercase()
        val code = param(params, "code")
        val timeout = paramOrNull(params, "timeout_seconds")?.toIntOrNull()?.coerceAtMost(60) ?: 30

        if (!terminal.isAvailable()) {
            return "Code execution unavailable: terminal environment not initialized."
        }

        val command = when (language) {
            "python", "python3" -> {
                val scriptFile = writeTemp(code, ".py")
                "python3 $scriptFile"
            }
            "bash", "sh" -> {
                val scriptFile = writeTemp(code, ".sh")
                "bash $scriptFile"
            }
            else -> return "Unsupported language: $language. Use 'python' or 'bash'."
        }

        return terminal.execute(command, timeoutSeconds = timeout)
    }

    private fun writeTemp(content: String, ext: String): String {
        val file = java.io.File.createTempFile("origami_code_", ext)
        file.writeText(content)
        file.deleteOnExit()
        return file.absolutePath
    }
}

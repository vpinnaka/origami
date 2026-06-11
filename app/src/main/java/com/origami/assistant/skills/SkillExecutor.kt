package com.origami.assistant.skills

import com.origami.assistant.data.db.entity.SkillEntity
import com.origami.assistant.terminal.TerminalSession
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class SkillRunResult(
    val output: String,
    val exitCode: Int = 0,
    val isError: Boolean = false
)

@Singleton
class SkillExecutor @Inject constructor(
    private val terminal: TerminalSession
) {
    /**
     * Execute a skill's entry point script.
     * [args] are passed as environment variables and command-line arguments.
     */
    suspend fun execute(
        skill: SkillEntity,
        entryPoint: String = "run.sh",
        args: Map<String, String> = emptyMap(),
        timeoutSeconds: Int = 60
    ): SkillRunResult {
        val scriptPath = "${skill.folderPath}/$entryPoint"
        val argString = args.entries.joinToString(" ") { (k, v) ->
            "$k=\"${v.replace("\"", "\\\"")}\""
        }
        val command = if (argString.isNotBlank()) {
            "$argString sh $scriptPath"
        } else {
            "sh $scriptPath"
        }

        Timber.d("Executing skill ${skill.name}: $command")
        val output = terminal.execute(command, timeoutSeconds)
        val isError = output.startsWith("Exit code") || output.startsWith("Execution error")

        return SkillRunResult(
            output = output,
            isError = isError
        )
    }
}

package com.origami.assistant.agent

import com.origami.assistant.agent.model.Tool
import com.origami.assistant.agent.model.ToolCall
import com.origami.assistant.agent.model.ToolResult
import com.origami.assistant.agent.tools.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    private val webSearchTool: WebSearchTool,
    private val composioTool: ComposioTool,
    private val codeExecutionTool: CodeExecutionTool,
    private val calculatorTool: CalculatorTool,
    private val calendarTool: CalendarTool
) {
    private val tools: Map<String, BaseTool> = mapOf(
        "web_search" to webSearchTool,
        "composio_action" to composioTool,
        "execute_code" to codeExecutionTool,
        "calculate" to calculatorTool,
        "calendar" to calendarTool
    )

    fun getDefinitions(enabled: Set<String> = tools.keys): List<Tool> =
        enabled.mapNotNull { tools[it]?.definition }

    suspend fun execute(call: ToolCall): ToolResult {
        val tool = tools[call.name]
            ?: return ToolResult(
                call.id, call.name,
                "Unknown tool: ${call.name}", isError = true
            )
        return try {
            Timber.d("Executing tool: ${call.name} params=${call.parameters}")
            val result = tool.execute(call.parameters)
            ToolResult(call.id, call.name, result)
        } catch (e: Exception) {
            Timber.e(e, "Tool execution failed: ${call.name}")
            ToolResult(call.id, call.name, "Tool error: ${e.message}", isError = true)
        }
    }
}

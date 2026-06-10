package com.origami.assistant.agent.model

data class Tool(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList()
)

data class ToolParameter(
    val name: String,
    val type: String,       // "string" | "number" | "boolean" | "array"
    val description: String,
    val required: Boolean = true
)

data class ToolCall(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val parameters: Map<String, Any>
)

data class ToolResult(
    val toolCallId: String,
    val toolName: String,
    val result: String,
    val isError: Boolean = false
)

sealed class AgentResponse {
    data class TextResponse(val text: String) : AgentResponse()
    data class ToolCallResponse(val toolCall: ToolCall, val preamble: String = "") : AgentResponse()
    data class ErrorResponse(val error: String) : AgentResponse()
}

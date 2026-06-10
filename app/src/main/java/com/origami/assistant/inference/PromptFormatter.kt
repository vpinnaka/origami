package com.origami.assistant.inference

import com.origami.assistant.agent.model.Tool
import com.origami.assistant.data.db.entity.MessageEntity

/**
 * Formats the Gemma 4 chat template.
 *
 * Gemma 4 uses the <start_of_turn> / <end_of_turn> format:
 *   <start_of_turn>user\n...<end_of_turn>\n<start_of_turn>model\n
 *
 * Tool definitions are injected in the system turn as a JSON schema block.
 */
object PromptFormatter {

    private const val BOS = "<bos>"
    private const val USER_START = "<start_of_turn>user"
    private const val MODEL_START = "<start_of_turn>model"
    private const val TURN_END = "<end_of_turn>"

    fun buildChatPrompt(
        systemPrompt: String,
        messages: List<MessageEntity>,
        tools: List<Tool> = emptyList(),
        memories: String = "",
        contextSummary: String = ""
    ): String = buildString {
        append(BOS)

        // System turn (Gemma 4 uses user role for system instructions)
        append("\n$USER_START\n")
        if (systemPrompt.isNotBlank()) append("$systemPrompt\n\n")
        if (memories.isNotBlank()) append("Relevant memories:\n$memories\n\n")
        if (contextSummary.isNotBlank()) append("Earlier conversation summary:\n$contextSummary\n\n")
        if (tools.isNotEmpty()) {
            append("You have access to the following tools. To call a tool, respond with a JSON block:\n")
            append("```json\n{\"tool_call\": {\"name\": \"<tool_name>\", \"parameters\": {<args>}}}\n```\n")
            append("Available tools:\n")
            tools.forEach { tool ->
                append("- **${tool.name}**: ${tool.description}\n")
                tool.parameters.forEach { param ->
                    append("  - ${param.name} (${param.type}${if (param.required) ", required" else ""}): ${param.description}\n")
                }
            }
            append("\n")
        }
        append("Respond naturally. Use tools when they'll help answer the question.\n")
        append(TURN_END)

        // Conversation history
        messages.forEach { msg ->
            when (msg.role) {
                "user" -> {
                    append("\n$USER_START\n${msg.content}$TURN_END")
                }
                "assistant" -> {
                    append("\n$MODEL_START\n${msg.content}$TURN_END")
                }
                "tool" -> {
                    append("\n$USER_START\nTool result (${msg.toolName}):\n${msg.content}$TURN_END")
                }
            }
        }

        // Open the model turn for generation
        append("\n$MODEL_START\n")
    }

    /** Extract JSON tool call from a model response, if present */
    fun extractToolCall(response: String): String? {
        val pattern = Regex("""```json\s*(\{.*?"tool_call".*?\})\s*```""", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(response)?.groupValues?.get(1)?.trim()
    }

    /** Strip the tool_call JSON block from a response to get the text portion */
    fun stripToolCall(response: String): String {
        val pattern = Regex("""```json\s*\{.*?"tool_call".*?\}\s*```""", RegexOption.DOT_MATCHES_ALL)
        return pattern.replace(response, "").trim()
    }
}

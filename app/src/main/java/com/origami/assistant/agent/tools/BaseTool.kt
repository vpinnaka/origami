package com.origami.assistant.agent.tools

import com.origami.assistant.agent.model.Tool

/** Base class for all agent tools */
abstract class BaseTool {
    abstract val definition: Tool
    abstract suspend fun execute(params: Map<String, Any>): String

    protected fun param(params: Map<String, Any>, key: String): String =
        params[key]?.toString() ?: error("Missing required parameter: $key")

    protected fun paramOrNull(params: Map<String, Any>, key: String): String? =
        params[key]?.toString()
}

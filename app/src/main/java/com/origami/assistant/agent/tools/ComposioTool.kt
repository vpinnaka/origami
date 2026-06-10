package com.origami.assistant.agent.tools

import com.origami.assistant.agent.model.Tool
import com.origami.assistant.agent.model.ToolParameter
import com.origami.assistant.data.prefs.AppPreferences
import com.origami.assistant.network.ComposioApi
import com.origami.assistant.network.model.ComposioActionRequest
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComposioTool @Inject constructor(
    private val composioApi: ComposioApi,
    private val prefs: AppPreferences
) : BaseTool() {

    override val definition = Tool(
        name = "composio_action",
        description = "Execute an action on a connected app (Gmail, Calendar, Slack, GitHub, Notion, etc.) via Composio's 250+ app integrations.",
        parameters = listOf(
            ToolParameter("app", "string", "App name, e.g. 'gmail', 'slack', 'github', 'notion'", required = true),
            ToolParameter("action", "string", "Action name, e.g. 'send_email', 'create_issue'", required = true),
            ToolParameter("parameters", "string", "JSON string of action parameters", required = false)
        )
    )

    override suspend fun execute(params: Map<String, Any>): String {
        val apiKey = prefs.composioApiKey.first()
        if (apiKey.isBlank()) {
            return "Composio unavailable: no API key configured. Go to Settings to add one."
        }

        val app = param(params, "app")
        val action = param(params, "action")
        val actionParams = paramOrNull(params, "parameters") ?: "{}"

        return try {
            val response = composioApi.executeAction(
                authorization = "Bearer $apiKey",
                request = ComposioActionRequest(
                    appName = app,
                    actionName = action,
                    parameters = actionParams
                )
            )
            if (response.success) {
                "Success: ${response.data ?: "Action completed"}"
            } else {
                "Action failed: ${response.error ?: "Unknown error"}"
            }
        } catch (e: Exception) {
            Timber.e(e, "Composio action failed")
            "Composio error: ${e.message}"
        }
    }
}

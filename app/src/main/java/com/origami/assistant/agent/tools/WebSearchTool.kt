package com.origami.assistant.agent.tools

import com.origami.assistant.agent.model.Tool
import com.origami.assistant.agent.model.ToolParameter
import com.origami.assistant.data.prefs.AppPreferences
import com.origami.assistant.network.TavilyApi
import com.origami.assistant.network.model.TavilySearchRequest
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSearchTool @Inject constructor(
    private val tavilyApi: TavilyApi,
    private val prefs: AppPreferences
) : BaseTool() {

    override val definition = Tool(
        name = "web_search",
        description = "Search the web for current information. Use for news, facts, prices, weather, or anything that changes over time.",
        parameters = listOf(
            ToolParameter("query", "string", "The search query", required = true),
            ToolParameter("max_results", "number", "Number of results to return (default 5)", required = false)
        )
    )

    override suspend fun execute(params: Map<String, Any>): String {
        val query = param(params, "query")
        val maxResults = paramOrNull(params, "max_results")?.toIntOrNull() ?: 5
        val apiKey = prefs.tavilyApiKey.first()

        if (apiKey.isBlank()) {
            return "Web search unavailable: no Tavily API key configured. Go to Settings to add one."
        }

        return try {
            val response = tavilyApi.search(
                TavilySearchRequest(
                    apiKey = apiKey,
                    query = query,
                    maxResults = maxResults,
                    searchDepth = "basic",
                    includeAnswer = true
                )
            )

            buildString {
                if (response.answer != null) {
                    appendLine("**Quick answer:** ${response.answer}")
                    appendLine()
                }
                appendLine("**Search results for:** $query")
                response.results.take(maxResults).forEachIndexed { i, result ->
                    appendLine("${i + 1}. **${result.title}**")
                    appendLine("   ${result.url}")
                    appendLine("   ${result.content.take(300)}…")
                    appendLine()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Web search failed")
            "Search failed: ${e.message}"
        }
    }
}

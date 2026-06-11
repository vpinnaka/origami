package com.origami.assistant.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TavilySearchRequest(
    @Json(name = "api_key") val apiKey: String,
    val query: String,
    @Json(name = "max_results") val maxResults: Int = 5,
    @Json(name = "search_depth") val searchDepth: String = "basic",
    @Json(name = "include_answer") val includeAnswer: Boolean = true,
    @Json(name = "include_raw_content") val includeRawContent: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TavilySearchResponse(
    val query: String = "",
    val answer: String? = null,
    val results: List<TavilyResult> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TavilyResult(
    val title: String = "",
    val url: String = "",
    val content: String = "",
    val score: Float = 0f
)

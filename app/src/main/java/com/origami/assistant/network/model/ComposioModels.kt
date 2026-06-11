package com.origami.assistant.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ComposioActionRequest(
    @Json(name = "appName") val appName: String,
    @Json(name = "actionName") val actionName: String,
    @Json(name = "parameters") val parameters: String = "{}"
)

@JsonClass(generateAdapter = true)
data class ComposioActionResponse(
    val success: Boolean = false,
    val data: String? = null,
    val error: String? = null
)

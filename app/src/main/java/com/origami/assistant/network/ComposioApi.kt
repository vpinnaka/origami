package com.origami.assistant.network

import com.origami.assistant.network.model.ComposioActionRequest
import com.origami.assistant.network.model.ComposioActionResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ComposioApi {
    @POST("actions/execute")
    suspend fun executeAction(
        @Header("Authorization") authorization: String,
        @Body request: ComposioActionRequest
    ): ComposioActionResponse
}

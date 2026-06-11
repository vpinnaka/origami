package com.origami.assistant.network

import com.origami.assistant.network.model.TavilySearchRequest
import com.origami.assistant.network.model.TavilySearchResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TavilyApi {
    @POST("search")
    suspend fun search(@Body request: TavilySearchRequest): TavilySearchResponse
}

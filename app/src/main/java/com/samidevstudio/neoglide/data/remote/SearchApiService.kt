package com.samidevstudio.neoglide.data.remote

import kotlinx.serialization.json.JsonArray
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApiService {
    @GET("complete/search?client=firefox")
    suspend fun getSearchSuggestions(
        @Query("q") query: String
    ): JsonArray // Returns ["query", ["suggestion1", "suggestion2", ...]]
}

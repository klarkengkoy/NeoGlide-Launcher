package com.samidevstudio.pxllauncherneo.data.repository

import com.samidevstudio.pxllauncherneo.data.remote.SearchApiService
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val searchApiService: SearchApiService
) {
    suspend fun getWebSuggestions(query: String): List<String> {
        return try {
            val response = searchApiService.getSearchSuggestions(query)
            if (response.size > 1) {
                val suggestionsArray = response[1].jsonArray
                suggestionsArray.map { it.jsonPrimitive.content }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

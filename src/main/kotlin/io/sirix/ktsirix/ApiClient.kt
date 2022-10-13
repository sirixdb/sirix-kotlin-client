package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference
import io.sirix.ktsirix.util.DefaultObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val TOKEN_ENDPOINT = "token"

private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

internal class ApiClient(
    private val basePath: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) {

    fun <T> authenticate(username: String, password: String, tClass: TypeReference<T>): T {
        return executeAuthentication(
            mapOf(
                "username" to username, "password" to password
            ),
            tClass
        )
    }

    fun <T> refreshToken(refreshToken: String, tClass: TypeReference<T>): T {
        return executeAuthentication(
            mapOf(
                "refresh_token" to refreshToken
            ),
            tClass
        )
    }

    private fun <T> executeAuthentication(requestBodyMap: Map<String, String>, tClass: TypeReference<T>): T {
        val requestBody = DefaultObjectMapper.writeValueAsString(requestBodyMap)
            .toRequestBody(JSON_MEDIA_TYPE.toMediaType())
        val request = Request.Builder()
            .url("$basePath/$TOKEN_ENDPOINT")
            .post(requestBody)
            .build()
        return executeRequestWithResponse(request, tClass, "authentication")
    }

    private fun <T> executeRequestWithResponse(request: Request, tClass: TypeReference<T>, operationName: String): T {
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SirixHttpClientException("The Sirix $operationName failed with status code: ${response.code}")
            }

            response.body?.let {
                DefaultObjectMapper.readValue(it.string(), tClass)
            } ?: throw SirixHttpClientException("The Sirix $operationName failed because the response body was not valid")
        }
    }
}

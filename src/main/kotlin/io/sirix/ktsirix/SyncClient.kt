package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference
import io.sirix.ktsirix.util.DefaultObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val TOKEN_ENDPOINT = "token"

private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

internal class SyncClient(
    private val host: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) : ApiClient {

    override fun <T> authenticate(username: String, password: String, tClass: TypeReference<T>): T {
        return executeAuthentication(
            mapOf(
                "username" to username,
                "password" to password,
                "grant_type" to "password"
            ),
            tClass
        )
    }

    override fun <T> refreshToken(refreshToken: String, tClass: TypeReference<T>): T {
        return executeAuthentication(
            mapOf(
                "refresh_token" to refreshToken
            ),
            tClass
        )
    }

    override fun createDatabase(name: String, type: DbType, accessToken: String) {
        val request = Request.Builder()
            .url("$host/$name")
            .method("PUT", null)
            .header("Content-Type", type.value)
            .header("Authorization", "Bearer $accessToken")
            .build()
        executeRequest(request, "database creation")
    }

    override fun <T> getDatabaseInfo(name: String, accessToken: String, tClass: TypeReference<T>): T {
        val request = Request.Builder()
            .url("$host/$name")
            .header("Accept", JSON_MEDIA_TYPE)
            .header("Authorization", "Bearer $accessToken")
            .build()
        return executeRequestWithResponse(request, tClass, "read database info")
    }

    private fun <T> executeAuthentication(requestBodyMap: Map<String, String>, tClass: TypeReference<T>): T {
        val requestBody = DefaultObjectMapper.writeValueAsString(requestBodyMap)
            .toRequestBody(JSON_MEDIA_TYPE.toMediaType())
        val request = Request.Builder()
            .url("$host/$TOKEN_ENDPOINT")
            .post(requestBody)
            .build()
        //TODO: change authentication exception for unauthorized
        return executeRequestWithResponse(request, tClass, "authentication")
    }

    private fun <T> executeRequestWithResponse(request: Request, tClass: TypeReference<T>, operationName: String): T {
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SirixHttpClientException("The Sirix $operationName failed with status code: ${response.code} and body: ${response.body?.string()}")
            }

            response.body?.let {
                DefaultObjectMapper.readValue(it.string(), tClass)
            } ?: throw SirixHttpClientException("The Sirix $operationName failed because the response body was not valid")
        }
    }

    private fun executeRequest(request: Request, operationName: String) {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SirixHttpClientException("The Sirix $operationName failed with status code: ${response.code} and body: ${response.body?.string()}")
            }
        }
    }
}

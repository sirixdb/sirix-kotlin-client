package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference
import io.sirix.ktsirix.util.DefaultObjectMapper
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody

private const val TOKEN_ENDPOINT = "token"

private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

class SyncClient(
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
            .withAuthorization(accessToken)
            .build()
        executeRequest(request, "database creation")
    }

    override fun <T> getDatabaseInfo(name: String, accessToken: String, tClass: TypeReference<T>): T {
        val request = Request.Builder()
            .url("$host/$name")
            .header("Accept", JSON_MEDIA_TYPE)
            .withAuthorization(accessToken)
            .build()
        return executeRequestWithResponse(request, tClass, "read database info")
    }

    override fun deleteDatabase(name: String, accessToken: String) {
        val request = Request.Builder()
            .url("$host/$name")
            .method("DELETE", null)
            .withAuthorization(accessToken)
            .build()
        executeRequest(request, "database deletion")
    }

    override fun executeQuery(query: String, accessToken: String): String? = executeQuery(mapOf("query" to query), accessToken)

    override fun executeQuery(query: Map<String, String>, accessToken: String): String? {
        val request = Request.Builder()
            .url("$host/")
            .post(query.toRequestBody(JSON_MEDIA_TYPE))
            .withAuthorization(accessToken)
            .build()
        return executeRequest(request, "query")
    }

    override fun resourceExists(dbName: String, dbType: DbType, storeName: String, accessToken: String): Boolean {
        val request = Request.Builder()
            .url("$host/$dbName/$storeName")
            .head()
            .withAuthorization(accessToken)
            .header("Accept", dbType.value)
            .build()
        return httpClient.newCall(request).execute().use(Response::isSuccessful)
    }

    override fun <T> readResource(dbName: String, dbType: DbType, storeName: String, params: Map<String, String>, accessToken: String, tClass: TypeReference<T>): T {
        val urlBuilder = "$host/$storeName".toHttpUrl()
            .newBuilder()
        params.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", dbType.value)
            .withAuthorization(accessToken)
            .build()
        return executeRequestWithResponse(request, tClass, "read resource")
    }

    override fun createResource(dbName: String, dbType: DbType, storeName: String, data: String, accessToken: String, hashType: String): String? {
        val request = Request.Builder()
            .url("$host/$dbName/$storeName?hashType=$hashType")
            .put(data.toRequestBody(dbType.value.toMediaType()))
            .withAuthorization(accessToken)
            .build()
        return executeRequest(request, "create resource")
    }

    // TODO: add xml deserializer
    override fun <T> history(dbName: String, dbType: DbType, storeName: String, accessToken: String, tClass: TypeReference<T>): T {
        val request = Request.Builder()
            .url("$host/$dbName/$storeName/history")
            .header("Accept", dbType.value)
            .withAuthorization(accessToken)
            .build()
        return executeRequestWithResponse(request, tClass, "fetch history")
    }

    private fun <T> executeAuthentication(requestBodyMap: Map<String, String>, tClass: TypeReference<T>): T {
        val request = Request.Builder()
            .url("$host/$TOKEN_ENDPOINT")
            .post(requestBodyMap.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        //TODO: change authentication exception for unauthorized
        return executeRequestWithResponse(request, tClass, "authentication")
    }

    private fun <T> executeRequestWithResponse(request: Request, tClass: TypeReference<T>, operationName: String): T {
        return executeRequest(request, operationName)?.let {
            DefaultObjectMapper.readValue(it, tClass)
        } ?: throw SirixHttpClientException("The Sirix $operationName failed because the response body was not valid")
    }

    private fun executeRequest(request: Request, operationName: String): String? {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SirixHttpClientException("The Sirix $operationName failed with status code: ${response.code} and body: ${response.body?.string()}")
            }
            return response.body?.use(ResponseBody::string)
        }
    }

    private fun Map<String, String>.toRequestBody(mediaType: String) = DefaultObjectMapper.writeValueAsString(this).toRequestBody(mediaType.toMediaType())

    private fun Request.Builder.withAuthorization(accessToken: String): Request.Builder = this.header("Authorization", "Bearer $accessToken")
}

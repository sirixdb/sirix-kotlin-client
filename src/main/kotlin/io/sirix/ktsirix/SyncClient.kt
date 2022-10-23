package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference
import io.sirix.ktsirix.util.DefaultObjectMapper
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

private const val TOKEN_ENDPOINT = "token"

private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

private const val AUTHORIZATION = "Authorization"

private const val BEARER = "Bearer"

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

    override fun <T> getGlobalInfo(resources: Boolean, accessToken: String, tClass: TypeReference<T>): T {
        val urlBuilder = "$host/".toHttpUrl()
            .newBuilder()
        if (resources) {
            urlBuilder.addQueryParameter("withResources", "true")
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .withAuthorization(accessToken)
            .build()
        return executeRequestWithResponse(request, tClass, "read global info")
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
            .delete()
            .withAuthorization(accessToken)
            .build()
        executeRequest(request, "database deletion")
    }

    override fun deleteAll(accessToken: String) {
        val request = Request.Builder()
            .url("$host/")
            .delete()
            .withAuthorization(accessToken)
            .build()
        executeRequest(request, "complete deletion")
    }

    override fun executeTextQuery(query: String, accessToken: String): String? = executeQuery(mapOf("query" to query), accessToken)

    override fun <T> executeQuery(query: T, accessToken: String): String? {
        val request = Request.Builder()
            .url("$host/")
            .post(query.toRequestBody(JSON_MEDIA_TYPE))
            .withAuthorization(accessToken)
            .build()
        return executeRequest(request, "query")
    }

    override fun resourceExists(dbName: String, dbType: DbType, resourceName: String, accessToken: String): Boolean {
        val request = Request.Builder()
            .url("$host/$dbName/$resourceName")
            .head()
            .withAuthorization(accessToken)
            .header("Accept", dbType.value)
            .build()
        return httpClient.newCall(request).execute().use(Response::isSuccessful)
    }

    override fun <T> readResource(dbName: String, dbType: DbType, resourceName: String, params: Map<String, String>, accessToken: String, tClass: TypeReference<T>): T =
        executeRequestWithResponse(readResourceRequest(dbName, dbType, resourceName, params, accessToken), tClass, "read resource")

    override fun readResourceAsString(dbName: String, dbType: DbType, resourceName: String, params: Map<String, String>, accessToken: String): String? =
        executeRequest(readResourceRequest(dbName, dbType, resourceName, params, accessToken), "read resource")

    private fun readResourceRequest(dbName: String, dbType: DbType, resourceName: String, params: Map<String, String>, accessToken: String): Request {
        val urlBuilder = "$host/$dbName/$resourceName".toHttpUrl()
            .newBuilder()
        params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

        return Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", dbType.value)
            .withAuthorization(accessToken)
            .build()
    }

    override fun createResource(dbName: String, dbType: DbType, resourceName: String, data: String, accessToken: String, hashType: String): String? {
        val request = Request.Builder()
            .url("$host/$dbName/$resourceName?hashType=$hashType")
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

    override fun <T> diff(dbName: String, resourceName: String, params: Map<String, String>, accessToken: String, tClass: TypeReference<T>): T {
        val urlBuilder = "$host/$dbName/$resourceName/diff".toHttpUrl()
            .newBuilder()
        params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
        val request = Request.Builder()
            .url(urlBuilder.build())
            .withAuthorization(accessToken)
            .build()
        return executeRequestWithResponse(request, tClass, "fetch diff")
    }

    override fun deleteResource(dbName: String, dbType: DbType, resourceName: String, nodeId: Int?, etag: String?, accessToken: String) {
        val urlBuilder = "$host/$resourceName".toHttpUrl()
            .newBuilder()
        nodeId?.let { urlBuilder.addQueryParameter("nodeId", it.toString()) }

        val headersBuilder: Headers.Builder = Headers.Builder()
            .withAuthorization(accessToken)
            .add("Content-Type", dbType.value)

        val nodeEtag = etag ?: nodeId?.let {
            getEtag(dbName, dbType, resourceName, it, accessToken)
        }
        nodeEtag?.let {
            headersBuilder.add("ETag", it)
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .delete()
            .headers(headersBuilder.build())
            .build()
        executeRequest(request, "delete resource")
    }

    override fun getEtag(dbName: String, dbType: DbType, resourceName: String, nodeId: Int, accessToken: String): String? {
        val request = Request.Builder()
            .url("$host/$resourceName?nodeId=$nodeId")
            .head()
            .withAuthorization(accessToken)
            .header("Accept", dbType.value)
            .build()
        return httpClient.newCall(request).execute().use {
            it.header("etag")
        }
    }

    override fun update(dbName: String, dbType: DbType, resourceName: String, nodeId: Int, data: String, insert: Insert, etag: String?, accessToken: String): String? {
        val headersBuilder: Headers.Builder = Headers.Builder()
            .withAuthorization(accessToken)
            .add("Content-Type", dbType.value)
        (etag ?: getEtag(dbName, dbType, resourceName, nodeId, accessToken))?.let {
            headersBuilder.add("ETag", it)
        }

        val request = Request.Builder()
            .url("$host/$resourceName?nodeId=$nodeId&insert=${insert.value}")
            .post(data.toRequestBody(dbType.value))
            .headers(headersBuilder.build())
            .build()
        return executeRequest(request, "update resource")
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

    private fun <T> T.toRequestBody(mediaType: String) = DefaultObjectMapper.writeValueAsString(this).toRequestBody(mediaType.toMediaType())

    private fun Request.Builder.withAuthorization(accessToken: String): Request.Builder = this.header(AUTHORIZATION, "$BEARER $accessToken")

    private fun Headers.Builder.withAuthorization(accessToken: String): Headers.Builder = this.add(AUTHORIZATION, "$BEARER $accessToken")
}

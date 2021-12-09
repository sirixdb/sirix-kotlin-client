package wrappers

import com.fasterxml.jackson.annotation.JsonProperty
import io.vertx.core.http.*
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.*
import kotlinx.coroutines.*

data class KeycloakAuthToken(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("refresh_expires_in") val refreshExpiresIn: Int,
    @JsonProperty("refresh_token") val refreshToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("not-before-policy") val notBeforePolicy: Int,
    @JsonProperty("session_state") val sessionState: String,
    @JsonProperty("scope") val scope: String,
    @JsonProperty("expires_at") val expiresAt: Long,
) : AbstractAuthToken() {
    override val token: String
        get() = "$tokenType $accessToken"
}

@Suppress("MemberVisibilityCanBePrivate")
class VertxClientWrapper(
    private val httpClient: HttpClient,
    val host: String = "localhost",
    val port: Int = 9443,
    val username: String?,
    val password: String?,
) : AbstractClientWrapper() {
    private var _authTokenObject: KeycloakAuthToken? = null
    val authTokenObject get() = _authTokenObject
    private var job: Job? = null

    override val token: String?
        get() = authTokenObject?.token

    private suspend fun makeResponse(resp: HttpClientResponse): Response {
        val body = resp.body().await().toString("utf-8")
        return Response(resp.statusCode(), resp.headers().toSet(), body)
    }

    private fun makeRequest(
        method: HttpMethod, path: String, params: Map<String, String>, headers: Map<String, String>, token: String?
    ): RequestOptions? {
        val uri = if (params.isNotEmpty()) {
            val strings = mutableListOf<String>()
            for (entry in params.entries) {
                strings.add("${entry.key}=${entry.value}")
            }
            path + strings.joinToString("&")
        } else {
            path
        }
        val options = RequestOptions().setHost(host).setPort(port).setMethod(method).setURI(uri)
        if (token != null) options.putHeader("authorization", token)
        for (pair in headers.entries) {
            options.putHeader(pair.key, pair.value)
        }
        return options
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun authenticate() {
        val resp = makeResponse(
            httpClient.request(
                makeRequest(
                    HttpMethod.POST, "/token", mapOf(), mapOf("content-type" to "application/json"), null
                )
            ).await().send(
                Json.encodeToBuffer(
                    mapOf(
                        "username" to username,
                        "password" to password,
                        "grant_type" to "password",
                    )
                )
            ).await()
        )
        // TODO("check status code for success")
        _authTokenObject = Json.decodeValue(resp.body, KeycloakAuthToken::class.java)
        job?.cancel()
        job = GlobalScope.launch {
            while (isActive) {
                val tokenObject = authTokenObject
                if (tokenObject == null) {
                    authenticate()
                } else {
                    delay((tokenObject.expiresIn - 5) * 1000L)
                    @Suppress("NAME_SHADOWING")
                    val resp = makeResponse(
                        httpClient.request(
                            makeRequest(
                                HttpMethod.POST,
                                "/token",
                                mapOf(),
                                mapOf("content-type" to "application/json"),
                                null,
                            )
                        ).await().send(Json.encodeToBuffer(mapOf("refresh_token" to tokenObject.refreshToken))).await()
                    )
                    _authTokenObject = Json.decodeValue(resp.body, KeycloakAuthToken::class.java)
                }
            }
        }
    }

    fun stopRefreshLoop() {
        job?.cancel()
    }

    override suspend fun head(path: String, params: Map<String, String>, headers: Map<String, String>): Response {
        return makeResponse(
            httpClient.request(makeRequest(HttpMethod.HEAD, path, params, headers, token)).await().send().await()
        )
    }

    override suspend fun get(path: String, params: Map<String, String>, headers: Map<String, String>): Response {
        return makeResponse(
            httpClient.request(makeRequest(HttpMethod.GET, path, params, headers, token)).await().send().await()
        )
    }

    override suspend fun delete(path: String, params: Map<String, String>, headers: Map<String, String>): Response {
        return makeResponse(
            httpClient.request(makeRequest(HttpMethod.DELETE, path, params, headers, token)).await().send().await()
        )
    }

    override suspend fun post(
        path: String, params: Map<String, String>, headers: Map<String, String>, body: String
    ): Response {
        return makeResponse(
            httpClient.request(makeRequest(HttpMethod.POST, path, params, headers, token)).await().send(body).await()
        )
    }

    override suspend fun put(
        path: String, params: Map<String, String>, headers: Map<String, String>, body: String
    ): Response {
        return makeResponse(
            httpClient.request(makeRequest(HttpMethod.PUT, path, params, headers, token)).await().send(body).await()
        )
    }
}
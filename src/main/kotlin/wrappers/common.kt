package wrappers

class Response(
    val statusCode: Int,
    val headers: Set<Map.Entry<String, String>>,
    val body: String,
)

abstract class AbstractAuthToken {
    abstract val token: String;
}

abstract class AbstractClientWrapper {
    abstract val token: String?
    abstract suspend fun authenticate()
    abstract suspend fun head(path: String, params: Map<String, String>, headers: Map<String, String>): Response
    abstract suspend fun get(path: String, params: Map<String, String>, headers: Map<String, String>): Response
    abstract suspend fun delete(path: String, params: Map<String, String>, headers: Map<String, String>): Response
    abstract suspend fun post(
        path: String,
        params: Map<String, String>,
        headers: Map<String, String>,
        body: String
    ): Response

    abstract suspend fun put(
        path: String,
        params: Map<String, String>,
        headers: Map<String, String>,
        body: String
    ): Response
}

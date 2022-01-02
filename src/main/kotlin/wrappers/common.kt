package wrappers

open class SirixException : Exception()

class UnauthorizedException(val serverMessage: String) : SirixException()

class InternalSirixServerException(val serverMessage: String) : SirixException()

class SirixClientUsageException(val errorMessage: String): SirixException()

class MissingSirixResourceException(val serverMessage: String) : SirixException()

class ConnectionException : SirixException()

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
    abstract fun stopRefreshLoop()
    abstract suspend fun head(
        path: String,
        params: Map<String, java.io.Serializable?>,
        headers: Map<String, String>
    ): Response

    abstract suspend fun get(
        path: String,
        params: Map<String, java.io.Serializable?>,
        headers: Map<String, String>
    ): Response

    abstract suspend fun delete(
        path: String,
        params: Map<String, java.io.Serializable?>,
        headers: Map<String, String>
    ): Response

    abstract suspend fun post(
        path: String,
        params: Map<String, java.io.Serializable?>,
        headers: Map<String, String>,
        body: String
    ): Response

    abstract suspend fun put(
        path: String,
        params: Map<String, java.io.Serializable?>,
        headers: Map<String, String>,
        body: String
    ): Response
}

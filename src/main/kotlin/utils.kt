import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import wrappers.Response

open class SirixException : Exception()

class UnauthorizedException(val serverMessage: String) : SirixException()

class InternalSirixServerException(val serverMessage: String) : SirixException()

class MissingSirixResourceException(val serverMessage: String) : SirixException()

suspend fun wrapRequest(check404: Boolean = true, requestFunction: suspend () -> Response): Response {
    val response = requestFunction()
    if (response.statusCode >= 500) {
        throw InternalSirixServerException(response.body)
    }
    if (response.statusCode == 401) {
        throw UnauthorizedException(response.body)
    }
    if (check404 && response.statusCode == 404) {
        throw MissingSirixResourceException(response.body)
    }
    return response
}

inline fun <reified T> parseToJson(response: Response): T {
    return Json.decodeFromString(response.body)
}
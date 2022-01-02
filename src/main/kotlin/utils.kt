import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import wrappers.InternalSirixServerException
import wrappers.MissingSirixResourceException
import wrappers.Response
import wrappers.UnauthorizedException

suspend fun checkResponse(response: Response, check404: Boolean = true): Response {
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
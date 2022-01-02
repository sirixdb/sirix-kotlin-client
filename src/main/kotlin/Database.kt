import wrappers.AbstractClientWrapper
import wrappers.Response

class Database(
    val databaseName: String, val databaseType: DatabaseType, private val httpClient: AbstractClientWrapper
) {
    suspend fun create(): Response {
        return checkResponse(
            httpClient.put(databaseName, mapOf(), mapOf("content-type" to databaseType.toHttpType()), "")
        )
    }

    suspend fun getDatabaseInfo(): DatabaseInfo {
        return parseToJson(
            checkResponse(
                httpClient.get(databaseName, mapOf(), mapOf("accept" to "application/json"))
            )
        )
    }

    suspend fun delete(): Response {
        return checkResponse(
            httpClient.delete(databaseName, mapOf(), mapOf())
        )
    }

    fun resource() {

    }

    fun jsonStore() {

    }
}
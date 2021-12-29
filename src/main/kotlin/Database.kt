import wrappers.AbstractClientWrapper
import wrappers.Response

class Database(
    val databaseName: String, val databaseType: DatabaseType, private val httpClient: AbstractClientWrapper
) {
    suspend fun create()/*: Response*/ {

    }

    suspend fun getDatabaseInfo(): DatabaseInfo {
        return parseToJson(wrapRequest {
            httpClient.get(databaseName, mapOf(), mapOf("accept" to "application/json"))
        })
    }

    suspend fun delete(): Response {
        return wrapRequest {
            httpClient.delete(databaseName, mapOf(), mapOf())
        }
    }

    fun resource() {

    }
}
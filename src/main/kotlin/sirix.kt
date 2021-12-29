import io.vertx.core.Vertx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wrappers.AbstractClientWrapper
import wrappers.Response
import wrappers.VertxClientWrapper

@Serializable
private data class QueryObject(
    val query: String, val startResultSeqIndex: Int? = null, val endResultSeqIndex: Int? = null
)

class Sirix(
    private val httpClient: AbstractClientWrapper
) {
    fun dispose() {
        httpClient.stopRefreshLoop()
    }

    fun database(databaseName: String, databaseType: DatabaseType): Database {
        return Database(databaseName, databaseType, httpClient)
    }

    suspend fun getInfo(): ServerInfo {
        return parseToJson(wrapRequest {
            httpClient.get("/", mapOf("withResources" to false), mapOf())
        })
    }

    suspend fun getInfoWithResources(): ServerInfo {
        return parseToJson(wrapRequest {
            httpClient.get("/", mapOf("withResources" to true), mapOf())
        })
    }

    suspend fun query(query: String, startResultSeqIndex: Int? = null, endResultSeqIndex: Int? = null): Response {
        return wrapRequest {
            httpClient.post(
                "/", mapOf(), mapOf(), Json.encodeToString(QueryObject(query, startResultSeqIndex, endResultSeqIndex))
            )
        }
    }

    suspend fun deleteAll(): Response {
        return wrapRequest {
            httpClient.delete("/", mapOf(), mapOf())
        }
    }
}

fun main() {
    val vertx = Vertx.vertx()
    val httpClient = vertx.createHttpClient()
    val clientWrapper = VertxClientWrapper(httpClient, username = "admin", password = "admin")
    runBlocking {
        launch {
            clientWrapper.authenticate()
            val sirix = Sirix(clientWrapper)
            try {
                println(sirix.getInfo())
                println(sirix.getInfoWithResources())
            } catch (e: InternalSirixServerException) {
                println(e.serverMessage)
            }
            sirix.dispose()
            vertx.close()
        }
    }
}
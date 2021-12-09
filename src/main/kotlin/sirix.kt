import io.vertx.core.Vertx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import wrappers.VertxClientWrapper


fun main() {
    val vertx = Vertx.vertx()
    val httpClient = vertx.createHttpClient()
    val clientWrapper = VertxClientWrapper(httpClient, username = "admin", password = "admin")
    runBlocking {
        launch {
            clientWrapper.authenticate()
            println(clientWrapper.token)
            clientWrapper.stopRefreshLoop()
            vertx.close()
        }
    }
}
import kotlinx.serialization.Serializable

enum class DatabaseType {
    JSON,
    XML,
}

@Serializable
data class ServerInfo(
    val databases: List<DatabaseInfo>,
)

@Serializable
data class DatabaseInfo(
    val name: String,
    val type: String,
    val resources: List<String> = listOf(),
)
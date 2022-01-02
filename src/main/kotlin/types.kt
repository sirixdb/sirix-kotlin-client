import kotlinx.serialization.Serializable

enum class DatabaseType {
    JSON,
    XML;

    fun toHttpType() = when (this) {
        JSON -> "application/json"
        XML -> "application/xml"
    }
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

@Serializable
data class History(
    val history: List<Commit>
)

@Serializable
data class Commit(
    val revisionTimestamp: String,
    val revision: ULong,
    val author: String,
    val commitMessage: String,
)
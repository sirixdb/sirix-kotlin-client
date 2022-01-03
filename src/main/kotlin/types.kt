import kotlinx.serialization.Serializable
import java.time.LocalDate

enum class DatabaseType {
    JSON, XML;

    fun toHttpType() = when (this) {
        JSON -> "application/json"
        XML -> "application/xml"
    }
}

enum class HashType {
    ROLLING, NONE;

    override fun toString() = when (this) {
        ROLLING -> "ROLLING"
        NONE -> "NONE"
    }
}

enum class MetadataType {
    ALL, KEY, NODE_KEY_AND_CHILD_COUNT;

    override fun toString(): String = when (this) {
        ALL -> "true"
        KEY -> "nodeKey"
        NODE_KEY_AND_CHILD_COUNT -> "nodeKeyAndChildCount"
    }
}

enum class Insert {
    CHILD, LEFT, RIGHT, REPLACE;

    override fun toString(): String = when (this) {
        CHILD -> "asFirstChild"
        LEFT -> "asLeftSibling"
        RIGHT -> "asRightSibling"
        REPLACE -> "replace"
    }
}

open class Revision private constructor() {
    class RevisionNumber(val number: Int?) : Revision()
    class RevisionTimestamp(val timestamp: LocalDate) : Revision()
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

@Serializable
data class DiffResponse(
    val diffs: List<Diff>
)

@Serializable
data class Diff(
    /// TODO fix this
    val dummyValue: String
)
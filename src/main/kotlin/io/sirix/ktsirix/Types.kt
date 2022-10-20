package io.sirix.ktsirix

import java.time.Instant

data class QueryResult(
    val revisionNumber: Int,
    val revisionTimestamp: Instant,
    // TODO: Check the revision type Union[Dict, List]
    val revision: Any
)

data class HistoryCommit(
    val history: List<Commit>
)

data class Commit(
    val revisionTimestamp: Instant,
    val revision: Int,
    val author: String,
    val commitMessage: String
)

data class HistorySubtreeRevision(
    val rest: List<SubtreeRevision>
)

data class SubtreeRevision(
    val revisionTimestamp: Instant,
    val revisionNumber: Int
)

data class HistoryRevision(
    val rest: List<Revision>
)

data class Revision(
    val revisionNumber: Int? = null,
    val revisionTimestamp: Instant? = null,
) {
    fun toParamsMap(): Map<String, String> {
        val map = HashMap<String, String>()
        revisionNumber?.let { map.put("revision", it.toString()) }
        revisionTimestamp?.let { map.put("revision-timestamp", it.toString()) }
        return map
    }
}

data class InsertDiff(
    val nodeKey: Int,
    val insertPositionNodeKey: Int,
    val insertPosition: InsertPosition,
    val deweyId: String,
    val depth: Int,
    val type: String,
    val data: DataType
)

data class ReplaceDiff(
    val nodeKey: Int,
    val type: DataType,
    val data: String
)

data class UpdateDiff(
    val nodeKey: Int,
    val type: DataType,
    // TODO: Check the revision type Union[str, int, float, bool, None]
    val value: Any
)

data class DeleteDiff(
    val nodeKey: Int,
    val deweyId: String,
    val depth: Int
)

data class Metadata(
    val nodeKey: Int,
    val hash: Int,
    val type: NodeType,
    val descendantCount: Int,
    val childCount: Int
)

data class MetaNode(
    val metadata: Metadata,
    val key: String,
    // TODO: Check the revision type Union[ List[Iterable["MetaNode"]], Iterable["MetaNode"], str, int, float, bool, None, ]
    val value: Any
)

data class DatabaseInfo(
    val name: String,
    val type: DbType,
    val resources: List<String>
)

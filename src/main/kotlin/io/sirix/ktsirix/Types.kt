package io.sirix.ktsirix

import java.time.Instant

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

data class DiffQueryResult(
    val diffs: List<Map<String, Any>>
)

data class GlobalInfo(
    val databases: List<DatabaseInfo>
)

data class DatabaseInfo(
    val name: String,
    val type: DbType,
    val resources: List<String>
)

data class SirixIndexedQuery(
    val query: String,
    val startResultSeqIndex: Int?,
    val endResultSeqIndex: Int?
)

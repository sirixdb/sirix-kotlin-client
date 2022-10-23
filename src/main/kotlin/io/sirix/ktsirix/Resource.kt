package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference

class Resource(
    private val dbName: String,
    private val dbType: DbType,
    private val resourceName: String,
    private val client: ApiClient,
    private val authManager: AuthenticationManager
) {

    fun create(data: String, hashType: String = "ROLLING"): String? = client.createResource(dbName, dbType, resourceName, data, authManager.getAccessToken(), hashType)

    fun read(
        nodeId: Int? = null,
        revision: Revision? = null,
        revisionRange: Pair<Revision, Revision>? = null,
        maxLevel: Int? = null,
        topLevelLimit: Int? = null,
        topLevelSkipLastNode: Int? = null,
        metadataType: MetadataType? = null
    ): String? {
        val params: Map<String, String> = mapOf(
            "nodeId" to nodeId?.toString(),
            "maxLevel" to maxLevel?.toString(),
            "nextTopLevelNodes" to topLevelLimit?.toString(),
            "lastTopLevelNodeKey" to topLevelSkipLastNode?.toString(),
            "revision" to revision?.revisionNumber?.toString(),
            "revision-timestamp" to revision?.revisionTimestamp?.toString(),
            "start-revision" to revisionRange?.first?.revisionNumber?.toString(),
            "start-revision-timestamp" to revisionRange?.first?.revisionTimestamp?.toString(),
            "end-revision" to revisionRange?.second?.revisionNumber?.toString(),
            "end-revision-timestamp" to revisionRange?.second?.revisionTimestamp?.toString(),
            "withMetadata" to metadataType?.value
        ).toMapNotNull()

        return client.readResourceAsString(dbName, dbType, resourceName, params, authManager.getAccessToken())
    }

    fun exists(): Boolean = client.resourceExists(dbName, dbType, resourceName, authManager.getAccessToken())

    fun history(): List<Commit> = client.history(dbName, dbType, resourceName, authManager.getAccessToken(), object : TypeReference<HistoryCommit>() {}).history

    fun diff(revisionPair: Pair<Revision, Revision>, nodeId: Int?, maxDepth: Int?): List<Map<String, Any>> {
        val params: Map<String, String> = mapOf(
            "first-revision" to revisionPair.first.revisionNumber?.toString(),
            "first-revision-timestamp" to revisionPair.first.revisionTimestamp?.toString(),
            "end-revision" to revisionPair.second.revisionNumber?.toString(),
            "end-revision-timestamp" to revisionPair.second.revisionTimestamp?.toString(),
            "startNodeKey" to nodeId?.toString(),
            "maxDepth" to maxDepth?.toString()
        ).toMapNotNull()
        return client.diff(dbName, resourceName, params, authManager.getAccessToken(), object : TypeReference<DiffQueryResult>() {}).diffs
    }

    fun getEtag(nodeId: Int): String? = client.getEtag(dbName, dbType, resourceName, nodeId, authManager.getAccessToken())

    fun update(nodeId: Int, data: String, insert: Insert = Insert.CHILD, etag: String? = null): String? =
        client.update(dbName, dbType, resourceName, nodeId, data, insert, etag, authManager.getAccessToken())

    fun query(query: String, startResultSeqIndex: Int?, endResultSeqIndex: Int?): String? {
        val params = mapOf(
            "query" to query,
            "startResultSeqIndex" to startResultSeqIndex?.toString(),
            "endResultSeqIndex" to endResultSeqIndex?.toString()
        ).toMapNotNull()
        return client.readResourceAsString(dbName, dbType, resourceName, params, authManager.getAccessToken())
    }

    private fun Map<String, String?>.toMapNotNull(): Map<String, String> = mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    fun delete(nodeId: Int? = null, etag: String? = null) {
        client.deleteResource(dbName, dbType, resourceName, nodeId, etag, authManager.getAccessToken())
    }
}

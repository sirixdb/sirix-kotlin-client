import wrappers.AbstractClientWrapper
import wrappers.Response
import wrappers.SirixClientUsageException
import java.io.Serializable

class Resource(
    val databaseName: String,
    val databaseType: DatabaseType,
    val resourceName: String,
    val httpClient: AbstractClientWrapper
) {
    private val httpType = databaseType.toHttpType()
    private val path = "$databaseName/$resourceName"

    suspend fun exists(): Boolean {
        return checkResponse(
            httpClient.head(path, mapOf(), mapOf("accept" to httpType)), check404 = false
        ).statusCode != 404
    }

    suspend fun create(data: String, hashType: HashType): Response {
        return checkResponse(
            httpClient.put(
                path, mapOf("hashType" to hashType.toString()), mapOf("content-type" to httpType), data
            )
        )
    }

    suspend fun history(): List<Commit> {
        return parseToJson<History>(
            checkResponse(
                httpClient.get(
                    "$databaseName/$resourceName/history", mapOf(), mapOf("accept" to httpType)
                )
            )
        ).history
    }

    suspend fun read(
        nodeId: Long? = null,
        maxLevel: UInt? = null,
        topLevelLimit: UInt? = null,
        topLevelSkipLastNode: UInt? = null,
        revision: Revision? = null,
        firstRevision: Revision? = null,
        lastRevision: Revision? = null
    ): Response {
        val params = buildReadParams(
            nodeId, maxLevel, topLevelLimit, topLevelSkipLastNode, revision, firstRevision, lastRevision, null
        )
        return checkResponse(httpClient.get(path, params, mapOf("accept" to httpType)))
    }

    suspend inline fun <reified T> readAndParse(
        nodeId: Long? = null,
        maxLevel: UInt? = null,
        topLevelLimit: UInt? = null,
        topLevelSkipLastNode: UInt? = null,
        revision: Revision? = null,
        firstRevision: Revision? = null,
        lastRevision: Revision? = null
    ): T {
        return parseToJson(
            read(
                nodeId, maxLevel, topLevelLimit, topLevelSkipLastNode, revision, firstRevision, lastRevision
            )
        )
    }

    suspend fun readWithMetadata(
        nodeId: Long? = null,
        maxLevel: UInt? = null,
        topLevelLimit: UInt? = null,
        topLevelSkipLastNode: UInt? = null,
        revision: Revision? = null,
        firstRevision: Revision? = null,
        lastRevision: Revision? = null,
        metadataType: MetadataType = MetadataType.ALL
    ): Response {
        /// TODO: change return type to metadata response type, parse json response
        val params = buildReadParams(
            nodeId, maxLevel, topLevelLimit, topLevelSkipLastNode, revision, firstRevision, lastRevision, metadataType
        )
        return checkResponse(httpClient.get(path, params, mapOf("accept" to httpType)))
    }

    suspend fun diff(firstRevision: Revision, secondRevision: Revision, nodeId: Long?, maxDepth: UInt?): DiffResponse {
        val params = mutableMapOf<String, Serializable>()
        params["first-revision"] = firstRevision.toString()
        params["second-revision"] = secondRevision.toString()
        if (nodeId != null) {
            params["startNodeKey"] = nodeId.toString()
        }
        if (maxDepth != null) {
            params["maxDepth"] = maxDepth.toString()
        }
        return parseToJson(checkResponse(httpClient.get("$databaseName/$resourceName/diff", params, mapOf())))
    }

    suspend fun etag(nodeId: Long): String {
        return checkResponse(
            httpClient.head(
                path, mapOf("nodeId" to nodeId.toString()), mapOf("accept" to httpType)
            )
        ).headers.first { it.key == "etag" }.value
    }

    suspend fun query(query: String, startResultSeqIndex: UInt?, endResultSeqIndex: UInt?): Response {
        val params = mutableMapOf(
            "query" to query,
        )
        if (startResultSeqIndex != null) {
            params["startResultSeqIndex"] = startResultSeqIndex.toString()
        }
        if (endResultSeqIndex != null) {
            params["endResultSeqIndex"] = endResultSeqIndex.toString()
        }
        return checkResponse(httpClient.get(path, params, mapOf("accept" to httpType)))
    }

    suspend fun update(nodeId: Long, data: String, insert: Insert = Insert.CHILD, etag: String?): Response {
        val etagToUse = etag ?: etag(nodeId)
        return checkResponse(
            httpClient.post(
                path,
                mapOf("nodeId" to nodeId.toString(), "insert" to insert),
                mapOf("etag" to etagToUse, "content-type" to httpType),
                data
            )
        )
    }

    suspend fun delete(nodeId: Long?, etag: String?): Response {
        var etagAssignable = etag
        if ((nodeId == null) && (etag != null)) {
            throw SirixClientUsageException("Provided an ETag but no nodeId. ETag: $etag")
        }
        val params = if (nodeId == null) {
            mapOf()
        } else {
            etagAssignable = etag(nodeId)
            mapOf("nodeId" to nodeId.toString())
        }
        val headers = if (etagAssignable != null) {
            mapOf("etag" to etagAssignable, "content-type" to httpType)
        } else {
            mapOf("content-type" to databaseType.toHttpType())
        }
        return checkResponse(httpClient.delete(path, params, headers))
    }
}

private fun buildReadParams(
    nodeId: Long?,
    maxLevel: UInt?,
    topLevelLimit: UInt?,
    topLevelSkipLastNode: UInt?,
    revision: Revision?,
    firstRevision: Revision?,
    lastRevision: Revision?,
    metadataType: MetadataType?,
): Map<String, Serializable> {
    val params = mutableMapOf<String, Serializable>()
    if (nodeId != null) {
        params["nodeId"] = nodeId.toString()
    }
    if (maxLevel != null) {
        params["maxLevel"] = maxLevel.toString()
    }
    if (topLevelLimit != null) {
        params["nextTopLevelNodes"] = topLevelLimit.toString()
    }
    if (topLevelSkipLastNode != null) {
        params["lastTopLevelNodeKey"] = topLevelSkipLastNode.toString()
    }
    if (revision != null) {
        when (revision) {
            is Revision.RevisionNumber -> params["revision"] = revision.number.toString()
            is Revision.RevisionTimestamp -> params["revision-timestamp"] = revision.timestamp
        }
    } else if (firstRevision != null && lastRevision != null) {
        when (firstRevision) {
            is Revision.RevisionNumber -> params["start-revision"] = firstRevision.number.toString()
            is Revision.RevisionTimestamp -> params["start-revision-timestamp"] = firstRevision.timestamp
        }
        when (lastRevision) {
            is Revision.RevisionNumber -> params["start-revision"] = lastRevision.number.toString()
            is Revision.RevisionTimestamp -> params["start-revision-timestamp"] = lastRevision.timestamp
        }
    }
    if (metadataType != null) {
        params["withMetadata"] = metadataType
    }
    return params
}

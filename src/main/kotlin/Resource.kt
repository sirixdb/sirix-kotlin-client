import wrappers.AbstractClientWrapper
import wrappers.Response
import wrappers.SirixClientUsageException

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

    suspend fun history(): List<Commit> {
        return parseToJson<History>(
            checkResponse(
                httpClient.get(
                    "$databaseName/$resourceName/history",
                    mapOf(),
                    mapOf("accept" to httpType)
                )
            )
        ).history
    }

    suspend fun etag(nodeId: ULong): String {
        return checkResponse(
            httpClient.head(
                path, mapOf("nodeId" to nodeId.toString()), mapOf("accept" to httpType)
            )
        ).headers.first { it.key == "etag" }.value
    }

    suspend fun query(query: String, startResultSeqIndex: ULong?, endResultSeqIndex: ULong?): Response {
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

    suspend fun delete(nodeId: ULong?, etag: String?): Response {
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
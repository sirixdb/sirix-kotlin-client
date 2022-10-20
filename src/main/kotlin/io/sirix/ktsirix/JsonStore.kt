package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference
import io.sirix.ktsirix.util.DefaultObjectMapper

private val queryFunctionInclude = """
    declare function local:q(${'$'}i, ${'$'}q) {
    let ${'$'}m := for ${'$'}k in jn:keys(${'$'}q) return if (not(empty(${'$'}i.${'$'}k))) then deep-equal(${'$'}i.${'$'}k, ${'$'}q.${'$'}k) else false()
     return empty(index-of(${'$'}m, false()))
    };
""".trimIndent()

private val upsertFunctionInclude = """
    declare %updating function local:upsert-fields(${'$'}r, ${'$'}u) {
    for ${'$'}key in bit:fields(${'$'}u) return if (empty(${'$'}r.${'$'}key)) then insert json ${'$'}u into ${'$'}r
     else replace json value of ${'$'}r.${'$'}key with ${'$'}u.${'$'}key
    };
""".trimIndent()

private val updateFunctionInclude = """
    declare %updating function local:update-fields(${'$'}r, ${'$'}u) {
    for ${'$'}key in bit:fields(${'$'}u) return replace json value of ${'$'}r.${'$'}key with ${'$'}u.${'$'}key
    };
""".trimIndent()

class JsonStore(
    private val dbName: String, private val dbType: DbType, private val storeName: String, private val root: String, private val client: ApiClient, private val authManager: AuthenticationManager
) {

    fun insertOne(record: String): String? {
        val query = "append json jn:parse('$record') into jn:doc('$dbName','$storeName')$root"
        return client.executeTextQuery(query, authManager.getAccessToken())
    }

    fun insertMany(records: List<String>): String? {
        val recordsString = records.joinToString(prefix = "[", separator = ", ", postfix = "]")
        val query = """
            let ${'$'}doc := jn:doc('$dbName','$storeName')$root
            for ${'$'}i in jn:parse('$recordsString') return append json ${'$'}i into ${'$'}doc
        """.trimIndent()
        return client.executeTextQuery(query, authManager.getAccessToken())
    }

    fun exists(): Boolean = client.resourceExists(dbName, dbType, storeName, authManager.getAccessToken())

    fun create(data: String = "[]"): String? = client.createResource(dbName, dbType, storeName, data, authManager.getAccessToken())

    fun resourceHistory(): List<Commit> = client.history(dbName, dbType, storeName, authManager.getAccessToken(), object : TypeReference<HistoryCommit>() {}).history

    fun historyEmbed(nodeKey: Int, revision: Revision? = null): List<Revision> {
        val query = """
            let ${'$'}node := sdb:select-item(${'$'}${'$'}, $nodeKey) let ${'$'}result := for ${'$'}rev in jn:all-times(${'$'}node)
             return if (not(exists(jn:previous(${'$'}rev)))) then ${'$'}rev
             else if (sdb:hash(${'$'}rev) ne sdb:hash(jn:previous(${'$'}rev))) then ${'$'}rev
             else () return ${'$'}result
        """.trimIndent()
        return readResource(query, revision, object : TypeReference<HistoryRevision>() {}).rest
    }

    fun historyRevision(nodeKey: Int, revision: Revision? = null): List<Revision> {
        val query = "sdb:item-history(sdb:select-item(\$\$, $nodeKey))"
        return readResource(query, revision, object : TypeReference<HistoryRevision>() {}).rest
    }

    fun historySubtreeRevision(nodeKey: Int, revision: Revision? = null): List<SubtreeRevision> {
        val revisionData = """{"revisionNumber": sdb:revision(${'$'}rev), "revisionTimestamp": xs:string(sdb:timestamp(${'$'}rev))}"""
        val query = """
            let ${'$'}node := sdb:select-item(${'$'}${'$'}, $nodeKey) let ${'$'}result := for ${'$'}rev in jn:all-times(${'$'}node)"
             return if (not(exists(jn:previous(${'$'}rev)))) then {revision_data}
             else if (sdb:hash(${'$'}rev) ne sdb:hash(jn:previous(${'$'}rev))) then $revisionData
             else () return ${'$'}result
        """.trimIndent()
        return readResource(query, revision, object : TypeReference<HistorySubtreeRevision>() {}).rest
    }

    // TODO: Confirm the response schema
    fun findByKey(nodeKey: Int, revision: Revision?): Map<String, Any> {
        val params = mutableMapOf("nodeId" to nodeKey.toString())
        revision?.toParamsMap()?.let(params::putAll)
        return client.readResource(dbName, dbType, storeName, params, authManager.getAccessToken(), object : TypeReference<Map<String, Any>>() {})
    }

    private fun <T> readResource(query: String, revision: Revision? = null, tClass: TypeReference<T>): T {
        val params = mutableMapOf("query" to query)
        revision?.toParamsMap()?.let(params::putAll)
        return client.readResource(dbName, dbType, storeName, params, authManager.getAccessToken(), tClass)
    }

    // TODO: add prepare find all and find all methods

    fun updateByKey(nodeKey: Int, update: String, upsert: Boolean = true): String? {
        val query = """
            ${upsertOrUpdateQuery(upsert)}
            let ${'$'}rec := sdb:select-item(jn:doc('$dbName','$storeName'),$nodeKey) 
            return local:${if (upsert) "upsert" else "update"}-fields(${'$'}rec, ${update.toJsonParse()})
        """.trimIndent()
        return client.executeTextQuery(query, authManager.getAccessToken())
    }

    fun updateMany(queryMatch: String, update: String, upsert: Boolean = true): String? {
        val query = """
            $queryFunctionInclude${upsertOrUpdateQuery(upsert)}
            for ${'$'}i in jn:doc('$dbName','$storeName')$root where local:q(${'$'}i, ${queryMatch.toJsonParse()})
            return local:${if (upsert) "upsert" else "update"}-fields(${'$'}i, ${update.toJsonParse()})
        """.trimIndent()
        return client.executeTextQuery(query, authManager.getAccessToken())
    }

    fun deleteFieldsByKey(nodeKey: Int, fields: List<String>): String? {
        val query = """
            let ${'$'}obj := sdb:select-item(jn:doc('$dbName','$storeName'),$nodeKey)
             let ${'$'}update := ${fields.toJsonParse()}
             for ${'$'}i in ${'$'}fields return delete json ${'$'}obj.${'$'}i
        """.trimIndent()
        return client.executeTextQuery(query, authManager.getAccessToken())
    }

    fun deleteField(queryMatch: String, fields: List<String>): String? {
        val query = """
            $queryFunctionInclude
            let ${'$'}records := for ${'$'}i in jn:doc('$dbName','$storeName')$root
             where local:q(${'$'}i, ${queryMatch.toJsonParse()}) return ${'$'}i
             let ${'$'}fields := ${fields.toJsonParse()}
             for ${'$'}i in ${'$'}fields return delete json ${'$'}records.${'$'}i
        """.trimIndent()
        return client.executeTextQuery(query, authManager.getAccessToken())
    }

    fun deleteRecords(queryMatch: String): String? {
        val query = """
            $queryFunctionInclude
            let ${'$'}doc := jn:doc('$dbName','$storeName')$root
             let ${'$'}m := for ${'$'}i at ${'$'}pos in ${'$'}doc where local:q(${'$'}i, ${queryMatch.toJsonParse()}) return ${'$'}pos - 1
             for ${'$'}i in ${'$'}m order by ${'$'}i descending return delete json ${'$'}doc[[${'$'}i]]
        """.trimIndent()
        return client.executeTextQuery(query, authManager.getAccessToken())
    }

    private fun upsertOrUpdateQuery(upsert: Boolean): String = if (upsert) upsertFunctionInclude else updateFunctionInclude

    private fun String.toJsonParse(): String = "jn:parse('$this')"

    private fun List<String>.toJsonParse(): String = "jn:parse('${DefaultObjectMapper.writeValueAsString(this)}')"
}

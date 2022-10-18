package io.sirix.ktsirix

class Sirix {
    private val client: SyncClient
    private val authManager: AuthenticationManager

    constructor(basePath: String, username: String, password: String) {
        client = SyncClient(basePath)
        authManager = AuthenticationManager(username, password, client)
    }

    internal constructor(client: SyncClient, authManager: AuthenticationManager) {
        this.client = client
        this.authManager = authManager
    }

    fun database(name: String, type: DbType): Database = Database(
        name,
        type,
        client,
        authManager
    )

    fun getInfo(withResources: Boolean = true): List<DatabaseInfo> {
        // TODO: implement
        return listOf()
    }

    fun query(query: String, startResultSeqIndex: Int? = null, endResultSeqIndex: Int? = null): String? {
        val queryMap = mutableMapOf("query" to query)
//        queryMap.put()
        // TODO: add the query params
        return client.executeQuery(query, authManager.getAccessToken())
    }

    fun deleteAll() {

    }


}

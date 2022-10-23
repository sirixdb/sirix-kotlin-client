package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference

class Sirix(
    private val client: ApiClient,
    private val authManager: AuthenticationManager
) {

    fun database(name: String, type: DbType): Database = Database(name, type, client, authManager)

    fun getInfo(withResources: Boolean = true): List<DatabaseInfo> = client.getGlobalInfo(withResources, authManager.getAccessToken(), object : TypeReference<GlobalInfo>() {}).databases

    fun query(query: String, startResultSeqIndex: Int? = null, endResultSeqIndex: Int? = null): String? = client.executeQuery(
        SirixIndexedQuery(query, startResultSeqIndex, endResultSeqIndex),
        authManager.getAccessToken()
    )

    fun deleteAll() {
        client.deleteAll(authManager.getAccessToken())
    }
}

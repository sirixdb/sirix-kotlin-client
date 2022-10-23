package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference

class Database(
    private val name: String,
    private val type: DbType,
    private val client: ApiClient,
    private val authManager: AuthenticationManager
) {

    fun create() {
        client.createDatabase(name, type, authManager.getAccessToken())
    }

    // TODO: confirm the response type
    fun getInfo(): DatabaseInfo = client.getDatabaseInfo(name, authManager.getAccessToken(), object : TypeReference<DatabaseInfo>() {})

    fun getResource(resourceName: String): Resource = Resource(
        name,
        type,
        resourceName,
        client,
        authManager
    )

    fun getJsonStore(storeName: String, root: String = ""): JsonStore = JsonStore(
        name,
        type,
        storeName,
        root,
        client,
        authManager
    )

    fun delete() {
        client.deleteDatabase(name, authManager.getAccessToken())
    }
}

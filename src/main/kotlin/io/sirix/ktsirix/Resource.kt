package io.sirix.ktsirix

class Resource(
    private val dbName: String,
    private val dbType: DbType,
    private val resourceName: String,
    private val client: ApiClient,
    private val authManager: AuthenticationManager
) {


}

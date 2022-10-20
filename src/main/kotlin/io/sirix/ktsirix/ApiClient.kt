package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference

interface ApiClient {

    fun <T> authenticate(username: String, password: String, tClass: TypeReference<T>): T
    fun <T> refreshToken(refreshToken: String, tClass: TypeReference<T>): T
    fun <T> getGlobalInfo(resources: Boolean = true, accessToken: String, tClass: TypeReference<T>): T
    fun createDatabase(name: String, type: DbType, accessToken: String)
    fun <T> getDatabaseInfo(name: String, accessToken: String, tClass: TypeReference<T>): T
    fun deleteDatabase(name: String, accessToken: String)
    fun deleteAll(accessToken: String)
    fun executeTextQuery(query: String, accessToken: String): String?
    fun <T> executeQuery(query: T, accessToken: String): String?
    fun resourceExists(dbName: String, dbType: DbType, storeName: String, accessToken: String): Boolean
    fun createResource(dbName: String, dbType: DbType, storeName: String, data: String, accessToken: String, hashType: String = "ROLLING"): String?
    fun <T> history(dbName: String, dbType: DbType, storeName: String, accessToken: String, tClass: TypeReference<T>): T
    fun <T> readResource(dbName: String, dbType: DbType, storeName: String, params: Map<String, String>, accessToken: String, tClass: TypeReference<T>): T
}

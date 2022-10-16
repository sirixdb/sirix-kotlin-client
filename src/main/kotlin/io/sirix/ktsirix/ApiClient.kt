package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference

interface ApiClient {

    fun <T> authenticate(username: String, password: String, tClass: TypeReference<T>): T

    fun <T> refreshToken(refreshToken: String, tClass: TypeReference<T>): T

    fun createDatabase(name: String, type: DbType, accessToken: String)

    fun <T> getDatabaseInfo(name: String, accessToken: String, tClass: TypeReference<T>): T
}

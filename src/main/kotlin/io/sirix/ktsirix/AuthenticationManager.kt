package io.sirix.ktsirix

import com.fasterxml.jackson.core.type.TypeReference

internal class AuthenticationManager(
    private val username: String,
    private val password: String,
    private val client: ApiClient
) {

    private var accessToken: AccessToken? = null

    fun getAccessToken(): String {
        accessToken?.let { token ->
            if (token.isNotExpired()) {
                return token.value
            }
            refreshAccessToken(token.refreshToken).let { response ->
                this.accessToken = response.toAccessToken()
                return response.accessToken
            }
        }
        fetchAccessToken().let { response ->
            this.accessToken = response.toAccessToken()
            return response.accessToken
        }
    }

    private fun fetchAccessToken(): AccessTokenResponse = client.authenticate(username, password, object : TypeReference<AccessTokenResponse>() {})

    private fun refreshAccessToken(refreshToken: String): AccessTokenResponse = client.refreshToken(refreshToken, object : TypeReference<AccessTokenResponse>() {})
}

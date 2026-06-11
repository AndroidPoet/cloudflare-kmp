package io.github.androidpoet.cloudflare.core.auth

/**
 * Authentication for the Cloudflare REST API (`https://api.cloudflare.com/client/v4`).
 *
 * Cloudflare supports two authentication schemes:
 *
 * - [ApiToken] — the recommended scheme. Sends `Authorization: Bearer <token>`.
 * - [ApiKey] — the legacy scheme. Sends `X-Auth-Email` and `X-Auth-Key`.
 *
 * These credentials administer Cloudflare account resources and must never be embedded in an
 * untrusted client. For mobile/browser apps prefer the Worker gateway model
 * (`CloudflareConfig` + `createCloudflareClient`).
 */
public sealed interface CloudflareCredentials {
    /**
     * API token authentication (recommended). The token is scoped via the Cloudflare dashboard.
     */
    public data class ApiToken(public val token: String) : CloudflareCredentials {
        init {
            require(token.isNotBlank()) { "token cannot be blank." }
        }
    }

    /**
     * Legacy global / origin CA API key authentication.
     *
     * @param email the account email associated with the key.
     * @param key the API key value.
     */
    public data class ApiKey(
        public val email: String,
        public val key: String,
    ) : CloudflareCredentials {
        init {
            require(email.isNotBlank()) { "email cannot be blank." }
            require(key.isNotBlank()) { "key cannot be blank." }
        }
    }
}

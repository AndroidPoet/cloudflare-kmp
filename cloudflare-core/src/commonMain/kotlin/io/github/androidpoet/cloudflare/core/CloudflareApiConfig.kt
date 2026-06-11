package io.github.androidpoet.cloudflare.core

import io.github.androidpoet.cloudflare.core.auth.CloudflareCredentials

/**
 * Configuration for talking directly to the Cloudflare REST API
 * (`https://api.cloudflare.com/client/v4`).
 *
 * @param accountId the Cloudflare account identifier most resources are scoped to.
 * @param credentials the [CloudflareCredentials] used to authenticate every request.
 * @param baseUrl the API base URL. Defaults to the public Cloudflare API endpoint.
 */
public data class CloudflareApiConfig(
    public val accountId: String,
    public val credentials: CloudflareCredentials,
    public val baseUrl: String = DEFAULT_BASE_URL,
) {
    init {
        require(accountId.isNotBlank()) { "accountId cannot be blank." }
        require(baseUrl.isNotBlank()) { "baseUrl cannot be blank." }
    }

    public val normalizedBaseUrl: String = baseUrl.trimEnd('/')

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.cloudflare.com/client/v4"
    }
}

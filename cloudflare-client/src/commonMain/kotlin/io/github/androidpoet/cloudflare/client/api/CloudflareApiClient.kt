package io.github.androidpoet.cloudflare.client.api

import io.github.androidpoet.cloudflare.client.transport.platformEngine
import io.github.androidpoet.cloudflare.core.CloudflareApiConfig
import io.github.androidpoet.cloudflare.core.auth.CloudflareCredentials
import io.github.androidpoet.cloudflare.core.result.CloudflareResult

/**
 * Client for the Cloudflare REST API (`https://api.cloudflare.com/client/v4`).
 *
 * Unlike [io.github.androidpoet.cloudflare.client.CloudflareClient] (which targets a Worker
 * gateway), this client talks to Cloudflare directly and authenticates with account-scoped
 * [CloudflareCredentials]. Use it for server-side / admin tooling, never inside an untrusted app.
 *
 * Returned strings are raw response bodies; decode them with the envelope helpers in
 * `CloudflareApiClientExt` (e.g. [decodeEnvelope]).
 */
public class CloudflareApiClient(
    public val config: CloudflareApiConfig,
) {
    private val transport =
        CloudflareApiTransport(
            config = config,
            engineFactory = platformEngine(),
        )

    /** The account id every account-scoped path is built against. */
    public val accountId: String get() = config.accountId

    public suspend fun get(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): CloudflareResult<String> = transport.get(path, queryParams, headers)

    public suspend fun post(
        path: String,
        body: String? = null,
        contentType: String = JSON_CONTENT_TYPE,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): CloudflareResult<String> = transport.post(path, body, contentType, queryParams, headers)

    public suspend fun put(
        path: String,
        body: String? = null,
        contentType: String = JSON_CONTENT_TYPE,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): CloudflareResult<String> = transport.put(path, body, contentType, queryParams, headers)

    public suspend fun delete(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): CloudflareResult<String> = transport.delete(path, queryParams, headers)

    public fun close(): Unit = transport.close()

    public companion object {
        /** `application/json` media type for [post] / [put] bodies. */
        public const val JSON_CONTENT_TYPE: String = "application/json"

        /** `text/plain` media type for [put] bodies (e.g. KV value writes). */
        public const val TEXT_CONTENT_TYPE: String = "text/plain"

        /** Build a client with an API token (`Authorization: Bearer <token>`). */
        public fun withToken(
            accountId: String,
            token: String,
            baseUrl: String = CloudflareApiConfig.DEFAULT_BASE_URL,
        ): CloudflareApiClient =
            CloudflareApiClient(
                CloudflareApiConfig(
                    accountId = accountId,
                    credentials = CloudflareCredentials.ApiToken(token),
                    baseUrl = baseUrl,
                ),
            )

        /** Build a client with the legacy email + global API key scheme. */
        public fun withApiKey(
            accountId: String,
            email: String,
            apiKey: String,
            baseUrl: String = CloudflareApiConfig.DEFAULT_BASE_URL,
        ): CloudflareApiClient =
            CloudflareApiClient(
                CloudflareApiConfig(
                    accountId = accountId,
                    credentials = CloudflareCredentials.ApiKey(email = email, key = apiKey),
                    baseUrl = baseUrl,
                ),
            )
    }
}

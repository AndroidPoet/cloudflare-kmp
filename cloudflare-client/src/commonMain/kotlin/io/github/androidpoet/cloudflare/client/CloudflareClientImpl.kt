package io.github.androidpoet.cloudflare.client

import io.github.androidpoet.cloudflare.client.transport.HttpTransport
import io.github.androidpoet.cloudflare.client.transport.platformEngine
import io.github.androidpoet.cloudflare.core.CloudflareConfig
import io.github.androidpoet.cloudflare.core.result.CloudflareResult

public class CloudflareClientImpl(
    config: CloudflareConfig,
) : CloudflareClient {
    private val transport = HttpTransport(
        config = config,
        engineFactory = platformEngine(),
    )

    override suspend fun get(
        endpoint: String,
        queryParams: Map<String, String>,
        headers: Map<String, String>,
    ): CloudflareResult<String> = transport.get(endpoint, queryParams, headers)

    override suspend fun post(
        endpoint: String,
        body: String?,
        headers: Map<String, String>,
    ): CloudflareResult<String> = transport.post(endpoint, body, headers)

    override suspend fun patch(
        endpoint: String,
        body: String?,
        queryParams: Map<String, String>,
        headers: Map<String, String>,
    ): CloudflareResult<String> = transport.patch(endpoint, body, queryParams, headers)

    override suspend fun delete(
        endpoint: String,
        queryParams: Map<String, String>,
        headers: Map<String, String>,
    ): CloudflareResult<String> = transport.delete(endpoint, queryParams, headers)

    override fun close(): Unit = transport.close()
}

public fun createCloudflareClient(
    workerUrl: String,
    publishableKey: String,
    accessTokenProvider: suspend () -> String? = { null },
): CloudflareClient = CloudflareClientImpl(
    CloudflareConfig(
        workerUrl = workerUrl,
        publishableKey = publishableKey,
        accessTokenProvider = accessTokenProvider,
    ),
)

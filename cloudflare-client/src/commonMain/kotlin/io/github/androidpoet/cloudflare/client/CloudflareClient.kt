package io.github.androidpoet.cloudflare.client

import io.github.androidpoet.cloudflare.core.result.CloudflareResult

public interface CloudflareClient {
    public suspend fun get(
        endpoint: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): CloudflareResult<String>

    public suspend fun post(
        endpoint: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): CloudflareResult<String>

    public suspend fun patch(
        endpoint: String,
        body: String? = null,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): CloudflareResult<String>

    public suspend fun delete(
        endpoint: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): CloudflareResult<String>

    public fun close()
}

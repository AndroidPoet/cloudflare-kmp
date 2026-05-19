package io.github.androidpoet.cloudflare.kv

import io.github.androidpoet.cloudflare.client.CloudflareClient
import io.github.androidpoet.cloudflare.client.decodeJson
import io.github.androidpoet.cloudflare.client.defaultCloudflareJson
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import kotlinx.serialization.encodeToString

public class KvClient(
    @PublishedApi
    internal val client: CloudflareClient,
) {
    public suspend fun get(
        namespace: String,
        key: String,
    ): CloudflareResult<String> =
        client.get(endpoint = "/kv/$namespace/$key")

    public suspend inline fun <reified T> getJson(
        namespace: String,
        key: String,
    ): CloudflareResult<T> =
        client.get(endpoint = "/kv/$namespace/$key").decodeJson()

    public suspend inline fun <reified T> putJson(
        namespace: String,
        key: String,
        value: T,
    ): CloudflareResult<String> =
        putText(namespace, key, defaultCloudflareJson.encodeToString(value))

    public suspend fun putText(
        namespace: String,
        key: String,
        value: String,
    ): CloudflareResult<String> =
        client.post(endpoint = "/kv/$namespace/$key", body = value)

    public suspend fun delete(
        namespace: String,
        key: String,
    ): CloudflareResult<String> =
        client.delete(endpoint = "/kv/$namespace/$key")
}

public fun CloudflareClient.kv(): KvClient = KvClient(this)

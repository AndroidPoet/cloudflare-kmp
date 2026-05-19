package io.github.androidpoet.cloudflare.d1

import io.github.androidpoet.cloudflare.client.CloudflareClient
import io.github.androidpoet.cloudflare.client.decodeJson
import io.github.androidpoet.cloudflare.client.defaultCloudflareJson
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import kotlinx.serialization.encodeToString

public class D1Table internal constructor(
    @PublishedApi
    internal val client: CloudflareClient,
    @PublishedApi
    internal val table: String,
) {
    public suspend inline fun <reified T> select(
        noinline query: D1QueryBuilder.() -> Unit = {},
    ): CloudflareResult<List<T>> {
        val builder = D1QueryBuilder().apply(query)
        return client.get(endpoint = endpoint(), queryParams = builder.build()).decodeJson()
    }

    public suspend inline fun <reified T> insert(
        value: T,
    ): CloudflareResult<T> {
        val body = defaultCloudflareJson.encodeToString(value)
        return client.post(endpoint = endpoint(), body = body).decodeJson()
    }

    public suspend inline fun <reified T> update(
        value: T,
        noinline query: D1QueryBuilder.() -> Unit,
    ): CloudflareResult<List<T>> {
        val body = defaultCloudflareJson.encodeToString(value)
        val builder = D1QueryBuilder().apply(query)
        return client.patch(endpoint = endpoint(), body = body, queryParams = builder.build()).decodeJson()
    }

    public suspend fun delete(
        query: D1QueryBuilder.() -> Unit,
    ): CloudflareResult<String> {
        val builder = D1QueryBuilder().apply(query)
        return client.delete(endpoint = endpoint(), queryParams = builder.build())
    }

    @PublishedApi
    internal fun endpoint(): String = "/d1/$table"
}

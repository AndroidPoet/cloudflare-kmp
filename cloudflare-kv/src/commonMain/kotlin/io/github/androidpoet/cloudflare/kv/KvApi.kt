package io.github.androidpoet.cloudflare.kv

import io.github.androidpoet.cloudflare.client.api.CloudflareApiClient
import io.github.androidpoet.cloudflare.client.api.CloudflarePage
import io.github.androidpoet.cloudflare.client.api.decodeEnvelope
import io.github.androidpoet.cloudflare.client.api.decodeEnvelopeListPage
import io.github.androidpoet.cloudflare.client.api.decodeEnvelopeUnit
import io.github.androidpoet.cloudflare.client.api.encodeUrlPathPart
import io.github.androidpoet.cloudflare.client.defaultCloudflareJson
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString

/**
 * Cloudflare Workers KV management + value operations over the REST API
 * (`/accounts/{account_id}/storage/kv/namespaces`).
 *
 * Direct-API counterpart to the Worker-gateway [KvClient]. Use it for server-side tooling with
 * account credentials, never in an untrusted client.
 */
public class KvApi(
    private val client: CloudflareApiClient,
) {
    private val basePath: String get() = "/accounts/${client.accountId}/storage/kv/namespaces"

    // --- Namespaces ---

    /** List KV namespaces with `page`/`per_page` pagination. */
    public suspend fun listNamespaces(
        page: Int? = null,
        perPage: Int? = null,
    ): CloudflareResult<CloudflarePage<List<KvNamespace>>> {
        val query = buildMap {
            page?.let { put("page", it.toString()) }
            perPage?.let { put("per_page", it.toString()) }
        }
        return client.get(basePath, queryParams = query).decodeEnvelopeListPage()
    }

    /** Create a KV namespace. */
    public suspend fun createNamespace(title: String): CloudflareResult<KvNamespace> {
        val body = defaultCloudflareJson.encodeToString(KvNamespaceRequest(title))
        return client.post(basePath, body = body).decodeEnvelope()
    }

    /** Get a single KV namespace by id. */
    public suspend fun getNamespace(namespaceId: String): CloudflareResult<KvNamespace> =
        client.get("$basePath/$namespaceId").decodeEnvelope()

    /** Rename a KV namespace (PUT). */
    public suspend fun renameNamespace(
        namespaceId: String,
        title: String,
    ): CloudflareResult<Unit> {
        val body = defaultCloudflareJson.encodeToString(KvNamespaceRequest(title))
        return client.put("$basePath/$namespaceId", body = body).decodeEnvelopeUnit()
    }

    /** Delete a KV namespace. */
    public suspend fun deleteNamespace(namespaceId: String): CloudflareResult<Unit> =
        client.delete("$basePath/$namespaceId").decodeEnvelopeUnit()

    // --- Keys ---

    /**
     * List keys in a namespace. Cursor-based pagination — pass the returned
     * `resultInfo.cursor` back as [cursor] to fetch the next page.
     */
    public suspend fun listKeys(
        namespaceId: String,
        prefix: String? = null,
        limit: Int? = null,
        cursor: String? = null,
    ): CloudflareResult<CloudflarePage<List<KvKey>>> {
        val query = buildMap {
            prefix?.let { put("prefix", it) }
            limit?.let { put("limit", it.toString()) }
            cursor?.let { put("cursor", it) }
        }
        return client.get("$basePath/$namespaceId/keys", queryParams = query)
            .decodeEnvelopeListPage()
    }

    // --- Values ---

    /**
     * Read a value. Returns the raw stored bytes as a string (this endpoint does NOT wrap its
     * response in the standard envelope).
     */
    public suspend fun readValue(
        namespaceId: String,
        key: String,
    ): CloudflareResult<String> =
        client.get("$basePath/$namespaceId/values/${key.encodeKey()}")

    /**
     * Write a value (PUT). Optionally set an absolute [expiration] (epoch seconds) or relative
     * [expirationTtl] (seconds).
     *
     * Note: attaching metadata requires the multipart form variant of this endpoint, which is not
     * portable across all KMP HTTP engines. To write values with metadata use [bulkWrite].
     */
    public suspend fun writeValue(
        namespaceId: String,
        key: String,
        value: String,
        expiration: Long? = null,
        expirationTtl: Long? = null,
    ): CloudflareResult<Unit> {
        val query = buildMap {
            expiration?.let { put("expiration", it.toString()) }
            expirationTtl?.let { put("expiration_ttl", it.toString()) }
        }
        return client.put(
            "$basePath/$namespaceId/values/${key.encodeKey()}",
            body = value,
            contentType = CloudflareApiClient.TEXT_CONTENT_TYPE,
            queryParams = query,
        ).decodeEnvelopeUnit()
    }

    /** Delete a value. */
    public suspend fun deleteValue(
        namespaceId: String,
        key: String,
    ): CloudflareResult<Unit> =
        client.delete("$basePath/$namespaceId/values/${key.encodeKey()}").decodeEnvelopeUnit()

    /** Read the metadata stored alongside a key. */
    public suspend fun readMetadata(
        namespaceId: String,
        key: String,
    ): CloudflareResult<String> =
        client.get("$basePath/$namespaceId/metadata/${key.encodeKey()}")

    // --- Bulk ---

    /** Bulk-write up to 10,000 key/value pairs (PUT .../bulk). */
    public suspend fun bulkWrite(
        namespaceId: String,
        entries: List<KvBulkWriteEntry>,
    ): CloudflareResult<KvBulkResult> {
        val body = defaultCloudflareJson.encodeToString(
            ListSerializer(KvBulkWriteEntry.serializer()),
            entries,
        )
        return client.put("$basePath/$namespaceId/bulk", body = body).decodeEnvelope()
    }

    /** Bulk-delete keys (POST .../bulk/delete). */
    public suspend fun bulkDelete(
        namespaceId: String,
        keys: List<String>,
    ): CloudflareResult<KvBulkResult> {
        val body = defaultCloudflareJson.encodeToString(
            ListSerializer(String.serializer()),
            keys,
        )
        return client.post("$basePath/$namespaceId/bulk/delete", body = body).decodeEnvelope()
    }

    private fun String.encodeKey(): String = encodeUrlPathPart(this)
}

/** Open the KV REST API surface against the given [CloudflareApiClient]. */
public fun CloudflareApiClient.kv(): KvApi = KvApi(this)

package io.github.androidpoet.cloudflare.d1

import io.github.androidpoet.cloudflare.client.api.CloudflareApiClient
import io.github.androidpoet.cloudflare.client.api.CloudflarePage
import io.github.androidpoet.cloudflare.client.api.decodeEnvelope
import io.github.androidpoet.cloudflare.client.api.decodeEnvelopeListPage
import io.github.androidpoet.cloudflare.client.api.decodeEnvelopeUnit
import io.github.androidpoet.cloudflare.client.defaultCloudflareJson
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import kotlinx.serialization.encodeToString

/**
 * Cloudflare D1 management + query over the REST API
 * (`/accounts/{account_id}/d1/database`).
 *
 * This is the direct-API counterpart to the Worker-gateway [D1Client]/[D1Table]. Use it for
 * server-side tooling with account credentials, never in an untrusted client.
 */
public class D1Api(
    private val client: CloudflareApiClient,
) {
    private val basePath: String get() = "/accounts/${client.accountId}/d1/database"

    /**
     * List D1 databases. Supports `name` filtering and `page`/`per_page` pagination.
     */
    public suspend fun listDatabases(
        name: String? = null,
        page: Int? = null,
        perPage: Int? = null,
    ): CloudflareResult<CloudflarePage<List<D1Database>>> {
        val query = buildMap {
            name?.let { put("name", it) }
            page?.let { put("page", it.toString()) }
            perPage?.let { put("per_page", it.toString()) }
        }
        return client.get(basePath, queryParams = query).decodeEnvelopeListPage()
    }

    /** Create a D1 database. */
    public suspend fun createDatabase(
        name: String,
        primaryLocationHint: String? = null,
    ): CloudflareResult<D1Database> {
        val body = defaultCloudflareJson.encodeToString(
            D1CreateDatabaseRequest(name = name, primaryLocationHint = primaryLocationHint),
        )
        return client.post(basePath, body = body).decodeEnvelope()
    }

    /** Get a single D1 database by id. */
    public suspend fun getDatabase(databaseId: String): CloudflareResult<D1Database> =
        client.get("$basePath/$databaseId").decodeEnvelope()

    /** Delete a D1 database by id. */
    public suspend fun deleteDatabase(databaseId: String): CloudflareResult<Unit> =
        client.delete("$basePath/$databaseId").decodeEnvelopeUnit()

    /**
     * Run a SQL statement against a database (`POST .../query`). Row results are returned as
     * objects. [params] are bound to `?` placeholders in order.
     */
    public suspend fun query(
        databaseId: String,
        sql: String,
        params: List<String> = emptyList(),
    ): CloudflareResult<List<D1QueryResult>> =
        execute("$basePath/$databaseId/query", sql, params)

    /**
     * Run a SQL statement using the performance-optimized raw endpoint (`POST .../raw`).
     * Row results are returned as column arrays rather than objects.
     */
    public suspend fun raw(
        databaseId: String,
        sql: String,
        params: List<String> = emptyList(),
    ): CloudflareResult<List<D1QueryResult>> =
        execute("$basePath/$databaseId/raw", sql, params)

    private suspend fun execute(
        path: String,
        sql: String,
        params: List<String>,
    ): CloudflareResult<List<D1QueryResult>> {
        val body = defaultCloudflareJson.encodeToString(
            D1QueryRequest(sql = sql, params = params.ifEmpty { null }),
        )
        return client.post(path, body = body).decodeEnvelope()
    }
}

/** Open the D1 REST API surface against the given [CloudflareApiClient]. */
public fun CloudflareApiClient.d1(): D1Api = D1Api(this)

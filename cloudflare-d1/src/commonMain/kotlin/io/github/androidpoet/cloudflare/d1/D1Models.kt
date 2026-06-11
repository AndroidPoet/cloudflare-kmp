package io.github.androidpoet.cloudflare.d1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A D1 database as returned by the Cloudflare REST API.
 */
@Serializable
public data class D1Database(
    public val uuid: String? = null,
    public val name: String? = null,
    public val version: String? = null,
    @SerialName("created_at")
    public val createdAt: String? = null,
    @SerialName("num_tables")
    public val numTables: Int? = null,
    @SerialName("file_size")
    public val fileSize: Long? = null,
    public val jurisdiction: String? = null,
    @SerialName("read_replication")
    public val readReplication: D1ReadReplication? = null,
)

@Serializable
public data class D1ReadReplication(
    public val mode: String? = null,
)

/** Body for creating a D1 database. */
@Serializable
public data class D1CreateDatabaseRequest(
    public val name: String,
    @SerialName("primary_location_hint")
    public val primaryLocationHint: String? = null,
)

/**
 * Per-statement query result returned inside the `result` array of a D1 query/raw response.
 *
 * For `/query` the [results] element shape is row objects; for `/raw` it is column arrays.
 */
@Serializable
public data class D1QueryResult(
    public val success: Boolean = false,
    public val results: JsonElement? = null,
    public val meta: D1Meta? = null,
)

/** Execution metadata for a D1 statement. */
@Serializable
public data class D1Meta(
    public val duration: Double? = null,
    @SerialName("changed_db")
    public val changedDb: Boolean? = null,
    public val changes: Int? = null,
    @SerialName("rows_read")
    public val rowsRead: Long? = null,
    @SerialName("rows_written")
    public val rowsWritten: Long? = null,
    @SerialName("last_row_id")
    public val lastRowId: Long? = null,
    @SerialName("served_by_primary")
    public val servedByPrimary: Boolean? = null,
    @SerialName("served_by_region")
    public val servedByRegion: String? = null,
    @SerialName("size_after")
    public val sizeAfter: Long? = null,
)

/** Body for `/query` and `/raw`. Send either a single [sql] or, for batched calls, multiple. */
@Serializable
public data class D1QueryRequest(
    public val sql: String,
    public val params: List<String>? = null,
)

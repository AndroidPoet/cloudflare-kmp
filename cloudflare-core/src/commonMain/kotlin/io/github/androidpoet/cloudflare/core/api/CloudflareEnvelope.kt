package io.github.androidpoet.cloudflare.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The standard Cloudflare REST API response envelope.
 *
 * Every `https://api.cloudflare.com/client/v4` response is wrapped in this shape:
 * `{ "success": bool, "errors": [...], "messages": [...], "result": <T>, "result_info": {...} }`.
 */
@Serializable
public data class CloudflareEnvelope<out T>(
    public val success: Boolean = false,
    public val errors: List<CloudflareApiError> = emptyList(),
    public val messages: List<CloudflareApiMessage> = emptyList(),
    public val result: T? = null,
    @SerialName("result_info")
    public val resultInfo: CloudflareResultInfo? = null,
)

/** A single error entry inside a Cloudflare envelope. */
@Serializable
public data class CloudflareApiError(
    public val code: Int = 0,
    public val message: String = "",
)

/** A single informational message entry inside a Cloudflare envelope. */
@Serializable
public data class CloudflareApiMessage(
    public val code: Int = 0,
    public val message: String = "",
)

/**
 * Pagination metadata returned by list endpoints that page with `page` / `per_page`.
 */
@Serializable
public data class CloudflareResultInfo(
    public val page: Int? = null,
    @SerialName("per_page")
    public val perPage: Int? = null,
    public val count: Int? = null,
    @SerialName("total_count")
    public val totalCount: Int? = null,
    @SerialName("total_pages")
    public val totalPages: Int? = null,
    /** Opaque cursor used by KV key listing (cursor-based pagination). */
    public val cursor: String? = null,
)

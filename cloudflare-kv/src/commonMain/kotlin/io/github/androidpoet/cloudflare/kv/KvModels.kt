package io.github.androidpoet.cloudflare.kv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** A Workers KV namespace. */
@Serializable
public data class KvNamespace(
    public val id: String,
    public val title: String,
    @SerialName("supports_url_encoding")
    public val supportsUrlEncoding: Boolean? = null,
)

/** Body for creating / renaming a namespace. */
@Serializable
public data class KvNamespaceRequest(
    public val title: String,
)

/** A key returned by the list-keys endpoint. */
@Serializable
public data class KvKey(
    public val name: String,
    public val expiration: Long? = null,
    public val metadata: JsonElement? = null,
)

/** An entry for a bulk write request. */
@Serializable
public data class KvBulkWriteEntry(
    public val key: String,
    public val value: String,
    public val expiration: Long? = null,
    @SerialName("expiration_ttl")
    public val expirationTtl: Long? = null,
    public val metadata: JsonElement? = null,
    public val base64: Boolean? = null,
)

/** Result of a bulk write / bulk delete operation. */
@Serializable
public data class KvBulkResult(
    @SerialName("successful_key_count")
    public val successfulKeyCount: Int? = null,
    @SerialName("unsuccessful_keys")
    public val unsuccessfulKeys: List<String> = emptyList(),
)

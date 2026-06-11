package io.github.androidpoet.cloudflare.r2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An R2 bucket as returned by the Cloudflare REST API. */
@Serializable
public data class R2Bucket(
    public val name: String,
    @SerialName("creation_date")
    public val creationDate: String? = null,
    public val location: String? = null,
    @SerialName("storage_class")
    public val storageClass: String? = null,
    public val jurisdiction: String? = null,
)

/** The list-buckets result object. */
@Serializable
public data class R2BucketList(
    public val buckets: List<R2Bucket> = emptyList(),
)

/** Body for creating an R2 bucket. */
@Serializable
public data class R2CreateBucketRequest(
    public val name: String,
    @SerialName("locationHint")
    public val locationHint: String? = null,
    @SerialName("storageClass")
    public val storageClass: String? = null,
)

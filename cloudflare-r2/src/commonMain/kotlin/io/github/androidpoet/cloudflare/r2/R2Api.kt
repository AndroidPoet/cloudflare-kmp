package io.github.androidpoet.cloudflare.r2

import io.github.androidpoet.cloudflare.client.api.CloudflareApiClient
import io.github.androidpoet.cloudflare.client.api.decodeEnvelope
import io.github.androidpoet.cloudflare.client.api.decodeEnvelopeUnit
import io.github.androidpoet.cloudflare.client.defaultCloudflareJson
import io.github.androidpoet.cloudflare.core.CloudflareHeaders
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import io.github.androidpoet.cloudflare.core.result.map
import kotlinx.serialization.encodeToString

/**
 * Cloudflare R2 bucket management over the REST API (`/accounts/{account_id}/r2/buckets`).
 *
 * Scope note: the Cloudflare REST API manages buckets only. Object-level operations
 * (GET / PUT / DELETE of objects, multipart uploads, presigned URLs) are NOT part of this REST
 * API — they are performed against the S3-compatible endpoint
 * (`https://<account_id>.r2.cloudflarestorage.com`) with SigV4, or via an R2 binding inside a
 * Worker. This client intentionally does not fake those endpoints.
 */
public class R2Api(
    private val client: CloudflareApiClient,
) {
    private val basePath: String get() = "/accounts/${client.accountId}/r2/buckets"

    /**
     * List R2 buckets. [jurisdiction] sets the `cf-r2-jurisdiction` header
     * ("default", "eu", "fedramp").
     */
    public suspend fun listBuckets(
        jurisdiction: String? = null,
    ): CloudflareResult<List<R2Bucket>> =
        client.get(basePath, headers = jurisdictionHeaders(jurisdiction))
            .decodeEnvelope<R2BucketList>()
            .map { it.buckets }

    /** Create an R2 bucket. */
    public suspend fun createBucket(
        name: String,
        locationHint: String? = null,
        storageClass: String? = null,
        jurisdiction: String? = null,
    ): CloudflareResult<R2Bucket> {
        val body = defaultCloudflareJson.encodeToString(
            R2CreateBucketRequest(
                name = name,
                locationHint = locationHint,
                storageClass = storageClass,
            ),
        )
        return client.post(basePath, body = body, headers = jurisdictionHeaders(jurisdiction))
            .decodeEnvelope()
    }

    /** Get metadata for a single R2 bucket. */
    public suspend fun getBucket(
        name: String,
        jurisdiction: String? = null,
    ): CloudflareResult<R2Bucket> =
        client.get("$basePath/$name", headers = jurisdictionHeaders(jurisdiction)).decodeEnvelope()

    /** Delete an (empty) R2 bucket. */
    public suspend fun deleteBucket(
        name: String,
        jurisdiction: String? = null,
    ): CloudflareResult<Unit> =
        client.delete("$basePath/$name", headers = jurisdictionHeaders(jurisdiction))
            .decodeEnvelopeUnit()

    private fun jurisdictionHeaders(jurisdiction: String?): Map<String, String> =
        if (jurisdiction == null) emptyMap() else mapOf(CloudflareHeaders.R2_JURISDICTION to jurisdiction)
}

/** Open the R2 REST API surface against the given [CloudflareApiClient]. */
public fun CloudflareApiClient.r2(): R2Api = R2Api(this)

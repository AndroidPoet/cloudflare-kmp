package io.github.androidpoet.cloudflare.r2

import io.github.androidpoet.cloudflare.client.CloudflareClient
import io.github.androidpoet.cloudflare.client.decodeJson
import io.github.androidpoet.cloudflare.client.defaultCloudflareJson
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

public class R2Client(
    private val client: CloudflareClient,
) {
    public suspend fun createUploadUrl(
        bucket: String,
        path: String,
        contentType: String,
        expiresInSeconds: Int = 900,
    ): CloudflareResult<R2SignedUrl> {
        val request = R2SignedUrlRequest(path, contentType, expiresInSeconds)
        return client
            .post(
                endpoint = "/r2/$bucket/upload-url",
                body = defaultCloudflareJson.encodeToString(request),
            ).decodeJson()
    }

    public suspend fun createDownloadUrl(
        bucket: String,
        path: String,
        expiresInSeconds: Int = 900,
    ): CloudflareResult<R2SignedUrl> {
        val request = R2DownloadUrlRequest(path, expiresInSeconds)
        return client
            .post(
                endpoint = "/r2/$bucket/download-url",
                body = defaultCloudflareJson.encodeToString(request),
            ).decodeJson()
    }

    public suspend fun deleteObject(
        bucket: String,
        path: String,
    ): CloudflareResult<String> =
        client.delete(endpoint = "/r2/$bucket/object", queryParams = mapOf("path" to path))
}

public fun CloudflareClient.r2(): R2Client = R2Client(this)

@Serializable
public data class R2SignedUrl(
    public val url: String,
    public val method: String,
    public val expiresInSeconds: Int,
)

@Serializable
internal data class R2SignedUrlRequest(
    val path: String,
    val contentType: String,
    val expiresInSeconds: Int,
)

@Serializable
internal data class R2DownloadUrlRequest(
    val path: String,
    val expiresInSeconds: Int,
)

package io.github.androidpoet.cloudflare.core.result

public data class CloudflareError(
    public val message: String,
    public val statusCode: Int? = null,
    public val code: String? = null,
    public val category: CloudflareErrorCategory = CloudflareErrorCategory.Unknown,
    public val cause: Throwable? = null,
) {
    public fun toException(): CloudflareException = CloudflareException(this)
}

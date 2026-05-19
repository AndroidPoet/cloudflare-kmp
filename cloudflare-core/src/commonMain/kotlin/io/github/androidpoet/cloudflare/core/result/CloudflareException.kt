package io.github.androidpoet.cloudflare.core.result

public class CloudflareException(
    public val error: CloudflareError,
) : Exception(error.message, error.cause)

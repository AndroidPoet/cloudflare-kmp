package io.github.androidpoet.cloudflare.core

public data class CloudflareConfig(
    public val workerUrl: String,
    public val publishableKey: String,
    public val accessTokenProvider: suspend () -> String? = { null },
) {
    init {
        require(workerUrl.isNotBlank()) { "workerUrl cannot be blank." }
        require(publishableKey.isNotBlank()) { "publishableKey cannot be blank." }
    }

    public val normalizedWorkerUrl: String = workerUrl.trimEnd('/')
}

package io.github.androidpoet.cloudflare.core

public object CloudflareHeaders {
    /** Publishable key header used by the Worker gateway transport. */
    public const val PUBLISHABLE_KEY: String = "x-cloudflare-publishable-key"
    public const val AUTHORIZATION: String = "Authorization"
    public const val CONTENT_TYPE: String = "Content-Type"

    /** Legacy Cloudflare REST API auth: account email. */
    public const val X_AUTH_EMAIL: String = "X-Auth-Email"

    /** Legacy Cloudflare REST API auth: global / origin CA API key. */
    public const val X_AUTH_KEY: String = "X-Auth-Key"

    /** R2 data-residency jurisdiction selector ("default", "eu", "fedramp"). */
    public const val R2_JURISDICTION: String = "cf-r2-jurisdiction"
}

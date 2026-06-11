package io.github.androidpoet.cloudflare.client.api

import io.ktor.http.encodeURLPathPart

/**
 * Percent-encode a single URL path segment (e.g. a KV key that may contain `/` or spaces).
 *
 * Lives here because ktor is an `implementation` dependency of `cloudflare-client` and is not on
 * the classpath of downstream product modules.
 */
public fun encodeUrlPathPart(value: String): String = value.encodeURLPathPart()

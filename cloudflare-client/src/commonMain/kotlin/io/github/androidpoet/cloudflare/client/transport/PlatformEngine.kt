package io.github.androidpoet.cloudflare.client.transport

import io.ktor.client.engine.HttpClientEngineFactory

internal expect fun platformEngine(): HttpClientEngineFactory<*>

/**
 * The default ktor [HttpClientEngineFactory] for the current platform (OkHttp on JVM/Android,
 * Darwin on Apple, JS on wasmJs). Exposed so other modules (e.g. realtime WebSockets) can build a
 * client without redeclaring the expect/actual engine plumbing.
 */
public fun defaultPlatformEngine(): HttpClientEngineFactory<*> = platformEngine()

package io.github.androidpoet.cloudflare.client.transport

import io.ktor.client.engine.HttpClientEngineFactory

internal expect fun platformEngine(): HttpClientEngineFactory<*>

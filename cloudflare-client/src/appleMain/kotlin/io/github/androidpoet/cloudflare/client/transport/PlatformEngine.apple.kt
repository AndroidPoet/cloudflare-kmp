package io.github.androidpoet.cloudflare.client.transport

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual fun platformEngine(): HttpClientEngineFactory<*> = Darwin

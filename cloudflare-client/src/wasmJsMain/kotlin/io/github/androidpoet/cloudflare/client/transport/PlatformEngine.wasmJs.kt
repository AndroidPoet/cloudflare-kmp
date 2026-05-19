package io.github.androidpoet.cloudflare.client.transport

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

internal actual fun platformEngine(): HttpClientEngineFactory<*> = Js

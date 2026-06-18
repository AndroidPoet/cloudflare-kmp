package io.github.androidpoet.cloudflare.realtime

import io.github.androidpoet.cloudflare.core.CloudflareConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement

public interface RealtimeClient {
    public val connectionState: StateFlow<ConnectionState>

    public suspend fun connect()

    public suspend fun subscribe(
        channel: String,
        onEvent: suspend (RealtimeEvent) -> Unit,
    ): RealtimeSubscription

    public suspend fun broadcast(
        channel: String,
        event: String,
        payload: JsonElement,
    )

    public suspend fun disconnect()
}

public fun createRealtimeClient(
    workerUrl: String,
    publishableKey: String,
    accessTokenProvider: suspend () -> String? = { null },
    reconnect: Boolean = true,
): RealtimeClient =
    WebSocketRealtimeClient(
        config =
            CloudflareConfig(
                workerUrl = workerUrl,
                publishableKey = publishableKey,
                accessTokenProvider = accessTokenProvider,
            ),
        reconnect = reconnect,
    )

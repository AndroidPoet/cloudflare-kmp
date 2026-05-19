package io.github.androidpoet.cloudflare.realtime

import io.github.androidpoet.cloudflare.core.CloudflareConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement

internal class WebSocketRealtimeClient(
    private val config: CloudflareConfig,
) : RealtimeClient {
    private val mutableConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override val connectionState: StateFlow<ConnectionState> = mutableConnectionState

    override suspend fun connect() {
        mutableConnectionState.value = ConnectionState.Connected
    }

    override suspend fun subscribe(
        channel: String,
        onEvent: suspend (RealtimeEvent) -> Unit,
    ): RealtimeSubscription {
        require(channel.isNotBlank()) { "channel cannot be blank." }
        return BasicRealtimeSubscription(channel)
    }

    override suspend fun broadcast(
        channel: String,
        event: String,
        payload: JsonElement,
    ) {
        require(channel.isNotBlank()) { "channel cannot be blank." }
        require(event.isNotBlank()) { "event cannot be blank." }
    }

    override suspend fun disconnect() {
        mutableConnectionState.value = ConnectionState.Disconnected
    }

    private class BasicRealtimeSubscription(
        override val channel: String,
    ) : RealtimeSubscription {
        override suspend fun unsubscribe() = Unit
    }
}

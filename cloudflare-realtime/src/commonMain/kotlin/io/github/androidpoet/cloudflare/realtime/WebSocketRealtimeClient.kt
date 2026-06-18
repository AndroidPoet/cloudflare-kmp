package io.github.androidpoet.cloudflare.realtime

import io.github.androidpoet.cloudflare.client.transport.defaultPlatformEngine
import io.github.androidpoet.cloudflare.core.CloudflareConfig
import io.github.androidpoet.cloudflare.core.CloudflareHeaders
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * WebSocket realtime client. Connects to the Worker gateway's `/realtime` endpoint (backed by a
 * Durable Object), subscribes to channels, broadcasts events, and reconnects with exponential
 * backoff on transport failures.
 */
internal class WebSocketRealtimeClient(
    private val config: CloudflareConfig,
    private val reconnect: Boolean = true,
    private val maxReconnectAttempts: Int = 5,
) : RealtimeClient {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    private val scope = CoroutineScope(SupervisorJob())
    private val mutableConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = mutableConnectionState

    private val mutex = Mutex()
    private val subscriptions = mutableMapOf<String, MutableList<suspend (RealtimeEvent) -> Unit>>()

    private var client: HttpClient? = null
    private var session: DefaultClientWebSocketSession? = null
    private var receiveJob: Job? = null

    override suspend fun connect() {
        mutex.withLock {
            if (session != null) return
            openSession(attempt = 0)
        }
    }

    private suspend fun openSession(attempt: Int) {
        mutableConnectionState.value = ConnectionState.Connecting
        try {
            val httpClient =
                HttpClient(defaultPlatformEngine()) {
                    install(WebSockets)
                }
            val token = config.accessTokenProvider()
            val newSession =
                httpClient.webSocketSession(realtimeUrl()) {
                    header(CloudflareHeaders.PUBLISHABLE_KEY, config.publishableKey)
                    token?.let { header(CloudflareHeaders.AUTHORIZATION, "Bearer $it") }
                }
            client = httpClient
            session = newSession
            mutableConnectionState.value = ConnectionState.Connected
            // Re-subscribe any channels registered before/after the (re)connect.
            subscriptions.keys.forEach { channel -> sendControl("subscribe", channel) }
            receiveJob = scope.launch { receiveLoop(newSession) }
        } catch (cause: Exception) {
            cleanupTransport()
            if (reconnect && attempt < maxReconnectAttempts) {
                val backoffMs = (1L shl attempt).coerceAtMost(30L) * 1_000L
                mutableConnectionState.value = ConnectionState.Connecting
                delay(backoffMs)
                openSession(attempt + 1)
            } else {
                mutableConnectionState.value =
                    ConnectionState.Failed(cause.message ?: "Realtime connection failed.")
            }
        }
    }

    private suspend fun receiveLoop(activeSession: DefaultClientWebSocketSession) {
        try {
            for (frame in activeSession.incoming) {
                if (frame !is Frame.Text) continue
                val event =
                    runCatching {
                        json.decodeFromString(RealtimeEvent.serializer(), frame.readText())
                    }.getOrNull() ?: continue
                dispatch(event)
            }
            // Channel closed normally.
            onDisconnected(reason = null)
        } catch (cause: Exception) {
            onDisconnected(reason = cause.message)
        }
    }

    private suspend fun dispatch(event: RealtimeEvent) {
        val handlers = mutex.withLock { subscriptions[event.channel]?.toList() }.orEmpty()
        handlers.forEach { handler -> runCatching { handler(event) } }
    }

    private suspend fun onDisconnected(reason: String?) {
        cleanupTransport()
        if (reconnect && scope.isActive) {
            openSession(attempt = 0)
        } else {
            mutableConnectionState.value = reason
                ?.let { ConnectionState.Failed(it) }
                ?: ConnectionState.Disconnected
        }
    }

    override suspend fun subscribe(
        channel: String,
        onEvent: suspend (RealtimeEvent) -> Unit,
    ): RealtimeSubscription {
        require(channel.isNotBlank()) { "channel cannot be blank." }
        val isNew =
            mutex.withLock {
                val handlers = subscriptions.getOrPut(channel) { mutableListOf() }
                val firstHandler = handlers.isEmpty()
                handlers.add(onEvent)
                firstHandler
            }
        if (isNew && session != null) {
            sendControl("subscribe", channel)
        }
        return ChannelSubscription(channel, onEvent)
    }

    override suspend fun broadcast(
        channel: String,
        event: String,
        payload: kotlinx.serialization.json.JsonElement,
    ) {
        require(channel.isNotBlank()) { "channel cannot be blank." }
        require(event.isNotBlank()) { "event cannot be blank." }
        val message =
            json.encodeToString(
                RealtimeMessage.serializer(),
                RealtimeMessage(type = "broadcast", channel = channel, event = event, payload = payload),
            )
        session?.send(message)
    }

    override suspend fun disconnect() {
        mutex.withLock {
            receiveJob?.cancelAndJoin()
            receiveJob = null
            session?.close()
            session = null
            client?.close()
            client = null
        }
        mutableConnectionState.value = ConnectionState.Disconnected
    }

    private suspend fun sendControl(type: String, channel: String) {
        val message =
            json.encodeToString(
                RealtimeMessage.serializer(),
                RealtimeMessage(type = type, channel = channel),
            )
        runCatching { session?.send(message) }
    }

    private fun cleanupTransport() {
        runCatching { client?.close() }
        session = null
        client = null
    }

    private fun realtimeUrl(): String {
        val base = config.normalizedWorkerUrl
        val wsBase =
            when {
                base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
                base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
                else -> base
            }
        return "$wsBase/realtime"
    }

    private suspend fun removeHandler(channel: String, handler: suspend (RealtimeEvent) -> Unit) {
        val nowEmpty =
            mutex.withLock {
                val handlers = subscriptions[channel] ?: return@withLock false
                handlers.remove(handler)
                if (handlers.isEmpty()) {
                    subscriptions.remove(channel)
                    true
                } else {
                    false
                }
            }
        if (nowEmpty && session != null) {
            sendControl("unsubscribe", channel)
        }
    }

    private inner class ChannelSubscription(
        override val channel: String,
        private val handler: suspend (RealtimeEvent) -> Unit,
    ) : RealtimeSubscription {
        override suspend fun unsubscribe() {
            removeHandler(channel, handler)
        }
    }
}

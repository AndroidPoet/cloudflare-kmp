package io.github.androidpoet.cloudflare.realtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * A control / broadcast message sent from the client to the realtime Worker.
 *
 * - `subscribe` / `unsubscribe`: register interest in [channel].
 * - `broadcast`: publish [event] with [payload] to [channel].
 */
@Serializable
public data class RealtimeMessage(
    public val type: String,
    public val channel: String,
    public val event: String? = null,
    public val payload: JsonElement? = null,
)

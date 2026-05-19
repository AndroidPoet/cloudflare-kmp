package io.github.androidpoet.cloudflare.realtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class RealtimeEvent(
    public val type: String,
    public val channel: String,
    public val payload: JsonElement,
)

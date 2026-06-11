package io.github.androidpoet.cloudflare.realtime

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RealtimeMessageTest {
    private val json = Json { explicitNulls = false }

    @Test
    fun test_subscribeMessage_omitsEventAndPayload() {
        val encoded = json.encodeToString(
            RealtimeMessage.serializer(),
            RealtimeMessage(type = "subscribe", channel = "room:lobby"),
        )
        assertEquals("{\"type\":\"subscribe\",\"channel\":\"room:lobby\"}", encoded)
    }

    @Test
    fun test_broadcastMessage_includesEventAndPayload() {
        val payload = buildJsonObject { put("text", JsonPrimitive("hi")) }
        val encoded = json.encodeToString(
            RealtimeMessage.serializer(),
            RealtimeMessage(type = "broadcast", channel = "room:lobby", event = "message", payload = payload),
        )
        assertTrue(encoded.contains("\"type\":\"broadcast\""))
        assertTrue(encoded.contains("\"event\":\"message\""))
        assertTrue(encoded.contains("\"text\":\"hi\""))
    }

    @Test
    fun test_realtimeEvent_roundTrips() {
        val payload = buildJsonObject { put("n", JsonPrimitive(1)) }
        val event = RealtimeEvent(type = "message", channel = "c", payload = payload)
        val encoded = json.encodeToString(RealtimeEvent.serializer(), event)
        val decoded = json.decodeFromString(RealtimeEvent.serializer(), encoded)

        assertEquals(event, decoded)
    }
}

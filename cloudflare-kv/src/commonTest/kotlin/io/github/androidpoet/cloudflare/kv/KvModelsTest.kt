package io.github.androidpoet.cloudflare.kv

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KvModelsTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    @Test
    fun test_kvKey_decodesNameExpirationMetadata() {
        val raw = """{"name":"a/b","expiration":1700000000,"metadata":{"k":"v"}}"""
        val key = json.decodeFromString(KvKey.serializer(), raw)

        assertEquals("a/b", key.name)
        assertEquals(1700000000L, key.expiration)
        assertTrue(key.metadata is JsonObject)
    }

    @Test
    fun test_kvKey_withoutOptionalFields() {
        val key = json.decodeFromString(KvKey.serializer(), """{"name":"only"}""")
        assertEquals("only", key.name)
        assertNull(key.expiration)
        assertNull(key.metadata)
    }

    @Test
    fun test_bulkWriteEntry_serializesExpirationTtlSnakeCase() {
        val encoded = json.encodeToString(
            KvBulkWriteEntry.serializer(),
            KvBulkWriteEntry(key = "k", value = "v", expirationTtl = 60),
        )
        assertTrue(encoded.contains("\"key\":\"k\""))
        assertTrue(encoded.contains("\"expiration_ttl\":60"))
    }

    @Test
    fun test_bulkResult_decodesSuccessCountAndUnsuccessfulKeys() {
        val raw = """{"successful_key_count":3,"unsuccessful_keys":["x"]}"""
        val result = json.decodeFromString(KvBulkResult.serializer(), raw)

        assertEquals(3, result.successfulKeyCount)
        assertEquals(listOf("x"), result.unsuccessfulKeys)
    }

    @Test
    fun test_bulkDeleteBody_isJsonArrayOfStrings() {
        val encoded = json.encodeToString(
            ListSerializer(String.serializer()),
            listOf("a", "b"),
        )
        assertEquals("[\"a\",\"b\"]", encoded)
    }
}

package io.github.androidpoet.cloudflare.d1

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class D1ModelsTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun test_d1QueryRequest_serializesSqlAndParams() {
        val encoded =
            json.encodeToString(
                D1QueryRequest.serializer(),
                D1QueryRequest(sql = "SELECT * FROM t WHERE id = ?", params = listOf("1")),
            )
        assertTrue(encoded.contains("\"sql\":\"SELECT * FROM t WHERE id = ?\""))
        assertTrue(encoded.contains("\"params\":[\"1\"]"))
    }

    @Test
    fun test_d1QueryResult_decodesResultsAndMeta() {
        val raw =
            """
            {"success":true,
             "meta":{"duration":0.5,"changes":1,"last_row_id":42,"rows_read":1,"rows_written":1,"served_by_region":"WNAM"},
             "results":[{"id":1,"title":"hi"}]}
            """.trimIndent()
        val result = json.decodeFromString(D1QueryResult.serializer(), raw)

        assertTrue(result.success)
        assertEquals(42L, result.meta?.lastRowId)
        assertEquals(1, result.meta?.changes)
        assertEquals("WNAM", result.meta?.servedByRegion)
        assertTrue(result.results is JsonArray)
    }

    @Test
    fun test_d1Database_mapsSnakeCaseFields() {
        val raw = """{"uuid":"u1","name":"db","version":"production","created_at":"2024-01-01T00:00:00Z","num_tables":3,"file_size":1024}"""
        val db = json.decodeFromString(D1Database.serializer(), raw)

        assertEquals("u1", db.uuid)
        assertEquals(3, db.numTables)
        assertEquals(1024L, db.fileSize)
        assertEquals("2024-01-01T00:00:00Z", db.createdAt)
    }

    @Test
    fun test_d1CreateRequest_omitsNullLocationHint() {
        val encoded =
            Json { explicitNulls = false }.encodeToString(
                D1CreateDatabaseRequest.serializer(),
                D1CreateDatabaseRequest(name = "db"),
            )
        assertEquals("{\"name\":\"db\"}", encoded)
    }
}

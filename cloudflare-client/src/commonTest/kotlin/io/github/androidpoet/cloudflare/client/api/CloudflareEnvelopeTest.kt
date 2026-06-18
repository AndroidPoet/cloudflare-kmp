package io.github.androidpoet.cloudflare.client.api

import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Serializable
private data class Sample(
    val id: String,
    val name: String,
)

class CloudflareEnvelopeTest {
    @Test
    fun test_decodeEnvelope_success_returnsResult() {
        val raw = """{"success":true,"errors":[],"messages":[],"result":{"id":"a","name":"db"}}"""
        val result = CloudflareResult.Success(raw).decodeEnvelope<Sample>()

        assertEquals(Sample("a", "db"), result.getOrNull())
    }

    @Test
    fun test_decodeEnvelope_failure_mapsErrorsToMessage() {
        val raw = """{"success":false,"errors":[{"code":7003,"message":"not found"}],"messages":[],"result":null}"""
        val result = CloudflareResult.Success(raw).decodeEnvelope<Sample>()

        val error = result.errorOrNull()
        assertTrue(result.isFailure)
        assertEquals("7003: not found", error?.message)
        assertEquals("7003", error?.code)
    }

    @Test
    fun test_decodeEnvelopeListPage_parsesResultInfoCursor() {
        val raw =
            """
            {"success":true,"errors":[],"messages":[],
             "result":[{"id":"1","name":"one"},{"id":"2","name":"two"}],
             "result_info":{"page":1,"per_page":2,"count":2,"cursor":"next123"}}
            """.trimIndent()
        val result = CloudflareResult.Success(raw).decodeEnvelopeListPage<Sample>()

        val page = result.getOrNull()
        assertEquals(2, page?.result?.size)
        assertEquals("next123", page?.resultInfo?.cursor)
        assertEquals(1, page?.resultInfo?.page)
    }

    @Test
    fun test_decodeEnvelopeUnit_successWithEmptyResult() {
        val raw = """{"success":true,"errors":[],"messages":[],"result":{}}"""
        val result = CloudflareResult.Success(raw).decodeEnvelopeUnit()

        assertTrue(result.isSuccess)
    }

    @Test
    fun test_decodeEnvelopeUnit_failurePropagatesErrors() {
        val raw = """{"success":false,"errors":[{"code":10000,"message":"auth"}],"messages":[],"result":null}"""
        val result = CloudflareResult.Success(raw).decodeEnvelopeUnit()

        assertTrue(result.isFailure)
        assertEquals("10000: auth", result.errorOrNull()?.message)
    }

    @Test
    fun test_decodeEnvelope_transportFailure_propagatesUnchanged() {
        val failure: CloudflareResult<String> =
            CloudflareResult.Failure(
                io.github.androidpoet.cloudflare.core.result.CloudflareError(
                    message = "network down",
                    category = io.github.androidpoet.cloudflare.core.result.CloudflareErrorCategory.Network,
                ),
            )
        val result = failure.decodeEnvelope<Sample>()

        assertEquals("network down", result.errorOrNull()?.message)
    }

    @Test
    fun test_encodeUrlPathPart_escapesSlashAndSpace() {
        assertEquals("users%2Fjane%20doe", encodeUrlPathPart("users/jane doe"))
    }
}

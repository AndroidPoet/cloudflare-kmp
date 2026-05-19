package io.github.androidpoet.cloudflare.core

import io.github.androidpoet.cloudflare.core.result.CloudflareError
import io.github.androidpoet.cloudflare.core.result.CloudflareErrorCategory
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import io.github.androidpoet.cloudflare.core.result.flatMap
import io.github.androidpoet.cloudflare.core.result.getOrElse
import io.github.androidpoet.cloudflare.core.result.map
import io.github.androidpoet.cloudflare.core.result.onFailure
import io.github.androidpoet.cloudflare.core.result.onSuccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudflareResultTest {
    @Test
    fun test_result_successExposesValue_expectedValueReturned() {
        val result: CloudflareResult<Int> = CloudflareResult.Success(42)

        val value = result.getOrNull()

        assertEquals(42, value)
        assertTrue(result.isSuccess)
    }

    @Test
    fun test_result_failureExposesError_expectedErrorReturned() {
        val error = CloudflareError("No access", category = CloudflareErrorCategory.Unauthorized)
        val result: CloudflareResult<Int> = CloudflareResult.Failure(error)

        val value = result.getOrNull()

        assertNull(value)
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun test_map_success_transformsValue() {
        val result = CloudflareResult.Success(10).map { it * 2 }

        assertEquals(20, result.getOrNull())
    }

    @Test
    fun test_flatMap_failure_keepsOriginalError() {
        val error = CloudflareError("boom")
        val result: CloudflareResult<Int> = CloudflareResult.Failure(error)

        val chained = result.flatMap { CloudflareResult.Success(it.toString()) }

        assertEquals(error, chained.errorOrNull())
    }

    @Test
    fun test_callbacks_successAndFailure_invokesMatchingCallbackOnly() {
        var successValue = 0
        var failureMessage: String? = null

        CloudflareResult.Success(7)
            .onSuccess { successValue = it }
            .onFailure { failureMessage = it.message }

        assertEquals(7, successValue)
        assertNull(failureMessage)
    }

    @Test
    fun test_getOrElse_failure_returnsFallbackValue() {
        val result: CloudflareResult<Int> = CloudflareResult.Failure(CloudflareError("missing"))

        val value = result.getOrElse { -1 }

        assertEquals(-1, value)
    }
}

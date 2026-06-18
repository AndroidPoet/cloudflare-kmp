package io.github.androidpoet.cloudflare.client.api

import io.github.androidpoet.cloudflare.client.defaultCloudflareJson
import io.github.androidpoet.cloudflare.core.api.CloudflareEnvelope
import io.github.androidpoet.cloudflare.core.api.CloudflareResultInfo
import io.github.androidpoet.cloudflare.core.result.CloudflareError
import io.github.androidpoet.cloudflare.core.result.CloudflareErrorCategory
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import io.github.androidpoet.cloudflare.core.result.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

/**
 * A successfully-decoded envelope paged result: the [result] plus its [resultInfo] (if any).
 */
public data class CloudflarePage<out T>(
    public val result: T,
    public val resultInfo: CloudflareResultInfo? = null,
)

/**
 * Decode a raw Cloudflare envelope into [CloudflarePage], preserving pagination metadata.
 *
 * - A network/transport failure is propagated unchanged.
 * - `success: false` (or a missing result) is mapped to a [CloudflareError] built from the
 *   envelope's `errors` array.
 * - A JSON parse failure becomes a [CloudflareErrorCategory.Serialization] error.
 */
public inline fun <reified T> CloudflareResult<String>.decodeEnvelopePage(
    json: Json = defaultCloudflareJson,
): CloudflareResult<CloudflarePage<T>> = decodeEnvelopePage(json, serializer<T>())

@PublishedApi
internal fun <T> CloudflareResult<String>.decodeEnvelopePage(
    json: Json,
    elementSerializer: KSerializer<T>,
): CloudflareResult<CloudflarePage<T>> =
    when (this) {
        is CloudflareResult.Failure -> this
        is CloudflareResult.Success ->
            try {
                val envelope =
                    json.decodeFromString(
                        CloudflareEnvelope.serializer(elementSerializer),
                        value,
                    )
                if (!envelope.success) {
                    CloudflareResult.Failure(envelope.toError())
                } else {
                    val result = envelope.result
                    if (result == null) {
                        CloudflareResult.Failure(
                            CloudflareError(
                                message = "Cloudflare response did not include a result.",
                                category = CloudflareErrorCategory.Serialization,
                            ),
                        )
                    } else {
                        CloudflareResult.Success(CloudflarePage(result, envelope.resultInfo))
                    }
                }
            } catch (exception: SerializationException) {
                CloudflareResult.Failure(
                    CloudflareError(
                        message = exception.message ?: "Failed to decode Cloudflare envelope.",
                        category = CloudflareErrorCategory.Serialization,
                        cause = exception,
                    ),
                )
            }
    }

/** Decode an envelope and return only its `result` (discarding pagination metadata). */
public inline fun <reified T> CloudflareResult<String>.decodeEnvelope(
    json: Json = defaultCloudflareJson,
): CloudflareResult<T> = decodeEnvelopePage<T>(json).map { it.result }

/** Decode an envelope whose `result` is a list, returning the list and pagination metadata. */
public inline fun <reified T> CloudflareResult<String>.decodeEnvelopeListPage(
    json: Json = defaultCloudflareJson,
): CloudflareResult<CloudflarePage<List<T>>> =
    decodeEnvelopePage(json, ListSerializer(serializer<T>()))

/** Decode an envelope whose `result` is a list, returning just the list. */
public inline fun <reified T> CloudflareResult<String>.decodeEnvelopeList(
    json: Json = defaultCloudflareJson,
): CloudflareResult<List<T>> = decodeEnvelopeListPage<T>(json).map { it.result }

/**
 * Decode an envelope where only `success`/`errors` matter (no typed `result`), e.g. delete calls.
 */
public fun CloudflareResult<String>.decodeEnvelopeUnit(
    json: Json = defaultCloudflareJson,
): CloudflareResult<Unit> =
    when (this) {
        is CloudflareResult.Failure -> this
        is CloudflareResult.Success ->
            try {
                val envelope =
                    json.decodeFromString(
                        CloudflareEnvelope.serializer(JsonElement.serializer()),
                        value,
                    )
                if (envelope.success) {
                    CloudflareResult.Success(Unit)
                } else {
                    CloudflareResult.Failure(envelope.toError())
                }
            } catch (exception: SerializationException) {
                CloudflareResult.Failure(
                    CloudflareError(
                        message = exception.message ?: "Failed to decode Cloudflare envelope.",
                        category = CloudflareErrorCategory.Serialization,
                        cause = exception,
                    ),
                )
            }
    }

@PublishedApi
internal fun CloudflareEnvelope<*>.toError(): CloudflareError {
    val first = errors.firstOrNull()
    val message =
        errors
            .joinToString("; ") { "${it.code}: ${it.message}" }
            .ifBlank { "Cloudflare request was not successful." }
    return CloudflareError(
        message = message,
        code = first?.code?.toString(),
        category = CloudflareErrorCategory.Unknown,
    )
}

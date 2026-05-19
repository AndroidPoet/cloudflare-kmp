package io.github.androidpoet.cloudflare.client

import io.github.androidpoet.cloudflare.core.result.CloudflareError
import io.github.androidpoet.cloudflare.core.result.CloudflareErrorCategory
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public val defaultCloudflareJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

public inline fun <reified T> CloudflareResult<String>.decodeJson(
    json: Json = defaultCloudflareJson,
): CloudflareResult<T> = when (this) {
    is CloudflareResult.Success -> try {
        CloudflareResult.Success(json.decodeFromString<T>(value))
    } catch (exception: SerializationException) {
        CloudflareResult.Failure(
            CloudflareError(
                message = exception.message ?: "Failed to decode Cloudflare response.",
                category = CloudflareErrorCategory.Serialization,
                cause = exception,
            ),
        )
    }
    is CloudflareResult.Failure -> this
}

public inline fun <reified T> Json.encodeBody(value: T): String = encodeToString(value)

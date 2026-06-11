package io.github.androidpoet.cloudflare.client.api

import io.github.androidpoet.cloudflare.core.CloudflareApiConfig
import io.github.androidpoet.cloudflare.core.CloudflareHeaders
import io.github.androidpoet.cloudflare.core.auth.CloudflareCredentials
import io.github.androidpoet.cloudflare.core.result.CloudflareError
import io.github.androidpoet.cloudflare.core.result.CloudflareErrorCategory
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * Low-level HTTP transport for the Cloudflare REST API. Adds Bearer / legacy auth headers and
 * returns the raw response body, leaving envelope decoding to callers.
 */
internal class CloudflareApiTransport(
    private val config: CloudflareApiConfig,
    engineFactory: HttpClientEngineFactory<*>,
) {
    private val client = HttpClient(engineFactory) {
        expectSuccess = false
    }

    suspend fun get(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        requestHeaders: Map<String, String> = emptyMap(),
    ): CloudflareResult<String> = execute {
        client.get(urlFor(path)) {
            applyCommon(requestHeaders)
            queryParams.forEach { (key, value) -> parameter(key, value) }
        }
    }

    suspend fun post(
        path: String,
        body: String? = null,
        contentType: String,
        queryParams: Map<String, String> = emptyMap(),
        requestHeaders: Map<String, String> = emptyMap(),
    ): CloudflareResult<String> = execute {
        client.post(urlFor(path)) {
            applyCommon(requestHeaders)
            contentType(ContentType.parse(contentType))
            queryParams.forEach { (key, value) -> parameter(key, value) }
            if (body != null) setBody(body)
        }
    }

    suspend fun put(
        path: String,
        body: String? = null,
        contentType: String,
        queryParams: Map<String, String> = emptyMap(),
        requestHeaders: Map<String, String> = emptyMap(),
    ): CloudflareResult<String> = execute {
        client.put(urlFor(path)) {
            applyCommon(requestHeaders)
            contentType(ContentType.parse(contentType))
            queryParams.forEach { (key, value) -> parameter(key, value) }
            if (body != null) setBody(body)
        }
    }

    suspend fun delete(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        requestHeaders: Map<String, String> = emptyMap(),
    ): CloudflareResult<String> = execute {
        client.delete(urlFor(path)) {
            applyCommon(requestHeaders)
            queryParams.forEach { (key, value) -> parameter(key, value) }
        }
    }

    fun close() {
        client.close()
    }

    private fun urlFor(path: String): String =
        "${config.normalizedBaseUrl}/${path.trimStart('/')}"

    private fun HttpRequestBuilder.applyCommon(requestHeaders: Map<String, String>) {
        when (val credentials = config.credentials) {
            is CloudflareCredentials.ApiToken ->
                header(CloudflareHeaders.AUTHORIZATION, "Bearer ${credentials.token}")
            is CloudflareCredentials.ApiKey -> {
                header(CloudflareHeaders.X_AUTH_EMAIL, credentials.email)
                header(CloudflareHeaders.X_AUTH_KEY, credentials.key)
            }
        }
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        requestHeaders.forEach { (key, value) -> header(key, value) }
    }

    private suspend fun execute(
        request: suspend () -> HttpResponse,
    ): CloudflareResult<String> =
        try {
            val response = request()
            val text = response.bodyAsText()
            if (response.status.value in 200..299) {
                CloudflareResult.Success(text)
            } else {
                CloudflareResult.Failure(response.status.toError(text))
            }
        } catch (exception: Exception) {
            CloudflareResult.Failure(
                CloudflareError(
                    message = exception.message ?: "Network request failed.",
                    category = CloudflareErrorCategory.Network,
                    cause = exception,
                ),
            )
        }

    private fun HttpStatusCode.toError(responseText: String): CloudflareError =
        CloudflareError(
            message = responseText.ifBlank { description },
            statusCode = value,
            category = toCategory(),
        )

    private fun HttpStatusCode.toCategory(): CloudflareErrorCategory =
        when (value) {
            400 -> CloudflareErrorCategory.Unknown
            401 -> CloudflareErrorCategory.Unauthorized
            403 -> CloudflareErrorCategory.Forbidden
            404 -> CloudflareErrorCategory.NotFound
            409 -> CloudflareErrorCategory.Conflict
            429 -> CloudflareErrorCategory.RateLimited
            in 500..599 -> CloudflareErrorCategory.Server
            else -> CloudflareErrorCategory.Unknown
        }
}

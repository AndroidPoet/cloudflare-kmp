package io.github.androidpoet.cloudflare.client.transport

import io.github.androidpoet.cloudflare.core.CloudflareConfig
import io.github.androidpoet.cloudflare.core.CloudflareHeaders
import io.github.androidpoet.cloudflare.core.result.CloudflareError
import io.github.androidpoet.cloudflare.core.result.CloudflareErrorCategory
import io.github.androidpoet.cloudflare.core.result.CloudflareResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal class HttpTransport(
    private val config: CloudflareConfig,
    engineFactory: HttpClientEngineFactory<*>,
) {
    private val client =
        HttpClient(engineFactory) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    },
                )
            }
        }

    suspend fun get(
        endpoint: String,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>,
    ): CloudflareResult<String> =
        execute {
            client.get(urlFor(endpoint)) {
                addCloudflareHeaders(requestHeaders)
                queryParams.forEach { (key, value) -> parameter(key, value) }
            }
        }

    suspend fun post(
        endpoint: String,
        body: String?,
        requestHeaders: Map<String, String>,
    ): CloudflareResult<String> =
        execute {
            client.post(urlFor(endpoint)) {
                addCloudflareHeaders(requestHeaders)
                contentType(ContentType.Application.Json)
                if (body != null) setBody(body)
            }
        }

    suspend fun patch(
        endpoint: String,
        body: String?,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>,
    ): CloudflareResult<String> =
        execute {
            client.patch(urlFor(endpoint)) {
                addCloudflareHeaders(requestHeaders)
                contentType(ContentType.Application.Json)
                queryParams.forEach { (key, value) -> parameter(key, value) }
                if (body != null) setBody(body)
            }
        }

    suspend fun delete(
        endpoint: String,
        queryParams: Map<String, String>,
        requestHeaders: Map<String, String>,
    ): CloudflareResult<String> =
        execute {
            client.delete(urlFor(endpoint)) {
                addCloudflareHeaders(requestHeaders)
                queryParams.forEach { (key, value) -> parameter(key, value) }
            }
        }

    fun close() {
        client.close()
    }

    private fun urlFor(endpoint: String): String =
        "${config.normalizedWorkerUrl}/${endpoint.trimStart('/')}"

    private suspend fun io.ktor.client.request.HttpRequestBuilder.addCloudflareHeaders(
        requestHeaders: Map<String, String>,
    ) {
        header(CloudflareHeaders.PUBLISHABLE_KEY, config.publishableKey)
        config.accessTokenProvider()?.let { token ->
            header(CloudflareHeaders.AUTHORIZATION, "Bearer $token")
        }
        headers {
            requestHeaders.forEach { (key, value) -> append(key, value) }
            if (!contains(HttpHeaders.Accept)) append(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
    }

    private suspend fun execute(
        request: suspend () -> io.ktor.client.statement.HttpResponse,
    ): CloudflareResult<String> =
        try {
            val response = request()
            val text = response.bodyAsText()
            if (response.status.isSuccess()) {
                CloudflareResult.Success(text)
            } else {
                CloudflareResult.Failure(response.toCloudflareError(text))
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

    private fun HttpStatusCode.isSuccess(): Boolean = value in 200..299

    private suspend fun io.ktor.client.statement.HttpResponse.toCloudflareError(
        responseText: String,
    ): CloudflareError {
        val message = responseText.ifBlank { status.description }
        return CloudflareError(
            message = message,
            statusCode = status.value,
            category = status.toCategory(),
        )
    }

    private fun HttpStatusCode.toCategory(): CloudflareErrorCategory =
        when (value) {
            401 -> CloudflareErrorCategory.Unauthorized
            403 -> CloudflareErrorCategory.Forbidden
            404 -> CloudflareErrorCategory.NotFound
            409 -> CloudflareErrorCategory.Conflict
            429 -> CloudflareErrorCategory.RateLimited
            in 500..599 -> CloudflareErrorCategory.Server
            else -> CloudflareErrorCategory.Unknown
        }
}

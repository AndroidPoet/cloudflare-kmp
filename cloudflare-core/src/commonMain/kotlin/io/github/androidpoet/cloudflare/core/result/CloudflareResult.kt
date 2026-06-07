package io.github.androidpoet.cloudflare.core.result

import kotlinx.coroutines.CancellationException

public sealed interface CloudflareResult<out T> {
    public data class Success<out T>(public val value: T) : CloudflareResult<T>
    public data class Failure(public val error: CloudflareError) : CloudflareResult<Nothing>

    public val isSuccess: Boolean get() = this is Success
    public val isFailure: Boolean get() = this is Failure

    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error.toException()
    }

    public fun errorOrNull(): CloudflareError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    public companion object {
        public inline fun <T> catching(block: () -> T): CloudflareResult<T> =
            try {
                Success(block())
            } catch (exception: CloudflareException) {
                Failure(exception.error)
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception
                Failure(
                    CloudflareError(
                        message = exception.message ?: "Unknown error",
                        cause = exception,
                    ),
                )
            }
    }
}

public inline fun <T, R> CloudflareResult<T>.map(
    transform: (T) -> R,
): CloudflareResult<R> = when (this) {
    is CloudflareResult.Success -> CloudflareResult.Success(transform(value))
    is CloudflareResult.Failure -> this
}

public inline fun <T, R> CloudflareResult<T>.flatMap(
    transform: (T) -> CloudflareResult<R>,
): CloudflareResult<R> = when (this) {
    is CloudflareResult.Success -> transform(value)
    is CloudflareResult.Failure -> this
}

public inline fun <T> CloudflareResult<T>.onSuccess(
    action: (T) -> Unit,
): CloudflareResult<T> = apply {
    if (this is CloudflareResult.Success) action(value)
}

public inline fun <T> CloudflareResult<T>.onFailure(
    action: (CloudflareError) -> Unit,
): CloudflareResult<T> = apply {
    if (this is CloudflareResult.Failure) action(error)
}

public inline fun <T> CloudflareResult<T>.onFailureCategory(
    category: CloudflareErrorCategory,
    action: (CloudflareError) -> Unit,
): CloudflareResult<T> = apply {
    if (this is CloudflareResult.Failure && error.category == category) action(error)
}

public inline fun <T> CloudflareResult<T>.onUnauthorized(
    action: (CloudflareError) -> Unit,
): CloudflareResult<T> = onFailureCategory(CloudflareErrorCategory.Unauthorized, action)

public inline fun <T> CloudflareResult<T>.onNotFound(
    action: (CloudflareError) -> Unit,
): CloudflareResult<T> = onFailureCategory(CloudflareErrorCategory.NotFound, action)

public inline fun <T> CloudflareResult<T>.onRateLimited(
    action: (CloudflareError) -> Unit,
): CloudflareResult<T> = onFailureCategory(CloudflareErrorCategory.RateLimited, action)

public inline fun <T> CloudflareResult<T>.recover(
    transform: (CloudflareError) -> T,
): CloudflareResult<T> = when (this) {
    is CloudflareResult.Success -> this
    is CloudflareResult.Failure -> CloudflareResult.Success(transform(error))
}

public inline fun <T> CloudflareResult<T>.getOrElse(
    defaultValue: (CloudflareError) -> T,
): T = when (this) {
    is CloudflareResult.Success -> value
    is CloudflareResult.Failure -> defaultValue(error)
}

public fun <T> CloudflareResult<T>.toKotlinResult(): Result<T> = when (this) {
    is CloudflareResult.Success -> Result.success(value)
    is CloudflareResult.Failure -> Result.failure(error.toException())
}

public inline fun <T> Result<T>.toCloudflareResult(
    mapThrowable: (Throwable) -> CloudflareError = { throwable ->
        val cloudflareException = throwable as? CloudflareException
        cloudflareException?.error ?: CloudflareError(
            message = throwable.message ?: "Unknown error",
            cause = throwable,
        )
    },
): CloudflareResult<T> = fold(
    onSuccess = { CloudflareResult.Success(it) },
    onFailure = { throwable ->
        if (throwable is CancellationException) throw throwable
        CloudflareResult.Failure(mapThrowable(throwable))
    },
)

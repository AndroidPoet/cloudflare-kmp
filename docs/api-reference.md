# API Reference

This is the alpha API surface. Names may change before the first stable release.

## Client

```kotlin
fun createCloudflareClient(
    workerUrl: String,
    publishableKey: String,
    accessTokenProvider: suspend () -> String? = { null },
): CloudflareClient
```

`CloudflareClient` exposes low-level HTTP methods:

```kotlin
suspend fun get(endpoint: String, queryParams: Map<String, String> = emptyMap()): CloudflareResult<String>
suspend fun post(endpoint: String, body: String? = null): CloudflareResult<String>
suspend fun patch(endpoint: String, body: String? = null, queryParams: Map<String, String> = emptyMap()): CloudflareResult<String>
suspend fun delete(endpoint: String, queryParams: Map<String, String> = emptyMap()): CloudflareResult<String>
```

Most apps should use product modules instead.

## Result

```kotlin
sealed interface CloudflareResult<out T> {
    data class Success<out T>(val value: T) : CloudflareResult<T>
    data class Failure(val error: CloudflareError) : CloudflareResult<Nothing>
}
```

Helpers:

```kotlin
map
flatMap
onSuccess
onFailure
onUnauthorized
onNotFound
onRateLimited
recover
getOrElse
```

## D1

```kotlin
val d1 = cloudflare.d1()
val todos = d1.from("todos")
```

Select:

```kotlin
todos.select<Todo> {
    eq("done", "false")
    order("created_at", descending = true)
    limit(25)
}
```

Insert:

```kotlin
todos.insert(Todo(id = "1", title = "Write docs", done = false))
```

Update:

```kotlin
todos.update(Todo(id = "1", title = "Ship docs", done = true)) {
    eq("id", "1")
}
```

Delete:

```kotlin
todos.delete {
    eq("id", "1")
}
```

## KV

```kotlin
val kv = cloudflare.kv()
```

Text:

```kotlin
kv.get("APP_KV", "settings/theme")
kv.putText("APP_KV", "settings/theme", "dark")
kv.delete("APP_KV", "settings/theme")
```

JSON:

```kotlin
kv.getJson<UserProfile>("APP_KV", "users/$userId/profile")
kv.putJson("APP_KV", "users/$userId/profile", profile)
```

## R2

```kotlin
val r2 = cloudflare.r2()
```

Upload URL:

```kotlin
r2.createUploadUrl(
    bucket = "avatars",
    path = "users/$userId/avatar.png",
    contentType = "image/png",
)
```

Download URL:

```kotlin
r2.createDownloadUrl(
    bucket = "avatars",
    path = "users/$userId/avatar.png",
)
```

The SDK API exists. The MVP Worker still needs SigV4 signing.

## Realtime

```kotlin
val realtime = createRealtimeClient(
    workerUrl = "https://api.example.workers.dev",
    publishableKey = "cfpub_live_xxx",
)
```

```kotlin
realtime.connect()

val subscription = realtime.subscribe("room:lobby") { event ->
    println(event)
}

realtime.disconnect()
```

The realtime transport is not production-complete yet.

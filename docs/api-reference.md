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

Realtime connects to the Worker `/realtime` endpoint over a WebSocket (`wss://`), sends
`subscribe` / `unsubscribe` / `broadcast` control frames, dispatches incoming `RealtimeEvent`
frames to channel handlers, and reconnects with exponential backoff on transport failures.

## Direct REST API

In addition to the Worker gateway, the SDK ships a direct client for the official Cloudflare REST
API (`https://api.cloudflare.com/client/v4`). These calls use account-scoped credentials and must
only run server-side (admin tooling, CI) — never inside an untrusted app.

```kotlin
val api = CloudflareApiClient.withToken(
    accountId = "<account id>",
    token = "<api token>",
)
// or the legacy scheme:
val api = CloudflareApiClient.withApiKey(accountId, email = "...", apiKey = "...")
```

Every response is the standard Cloudflare envelope
`{ success, errors, messages, result, result_info }`; the decode helpers
(`decodeEnvelope`, `decodeEnvelopeList`, `decodeEnvelopePage`, `decodeEnvelopeUnit`) surface
`errors` as a `CloudflareError` and preserve `result_info` pagination.

### D1 (`api.d1()`)

```kotlin
api.d1().listDatabases()                       // GET  /accounts/{acct}/d1/database
api.d1().createDatabase(name = "app")          // POST /accounts/{acct}/d1/database
api.d1().getDatabase(databaseId)               // GET  .../d1/database/{id}
api.d1().deleteDatabase(databaseId)            // DELETE .../d1/database/{id}
api.d1().query(databaseId, "SELECT * FROM t WHERE id = ?", listOf("1"))   // POST .../query
api.d1().raw(databaseId, "SELECT * FROM t")    // POST .../raw  (column arrays)
```

`query` / `raw` return `List<D1QueryResult>`, each with `results` plus a `D1Meta`
(`changes`, `rowsRead`, `rowsWritten`, `lastRowId`, `servedByRegion`, ...).

### KV (`api.kv()`)

```kotlin
api.kv().listNamespaces()                       // GET    .../storage/kv/namespaces
api.kv().createNamespace(title)                 // POST   .../namespaces
api.kv().getNamespace(nsId)                     // GET    .../namespaces/{id}
api.kv().renameNamespace(nsId, title)           // PUT    .../namespaces/{id}
api.kv().deleteNamespace(nsId)                  // DELETE .../namespaces/{id}
api.kv().listKeys(nsId, prefix = "user/", cursor = page.resultInfo?.cursor)  // GET .../keys
api.kv().readValue(nsId, key)                   // GET    .../values/{key}
api.kv().writeValue(nsId, key, value, expirationTtl = 3600)  // PUT .../values/{key}
api.kv().deleteValue(nsId, key)                 // DELETE .../values/{key}
api.kv().readMetadata(nsId, key)                // GET    .../metadata/{key}
api.kv().bulkWrite(nsId, entries)               // PUT    .../bulk
api.kv().bulkDelete(nsId, keys)                 // POST   .../bulk/delete
```

Key listing is cursor-paginated (`resultInfo.cursor`). Attaching metadata to a single value
requires the multipart variant (not portable across all KMP engines); use `bulkWrite` (JSON
metadata) for that.

### R2 (`api.r2()`)

```kotlin
api.r2().listBuckets()                          // GET    .../r2/buckets
api.r2().createBucket("avatars", locationHint = "wnam")  // POST .../r2/buckets
api.r2().getBucket("avatars")                   // GET    .../r2/buckets/{name}
api.r2().deleteBucket("avatars")                // DELETE .../r2/buckets/{name}
```

Scope note: the Cloudflare REST API manages **buckets only**. Object operations (GET/PUT/DELETE of
objects, multipart uploads, presigned URLs) are not part of this REST API — they go through the
S3-compatible endpoint (SigV4) or an R2 binding in a Worker. This client does not fake those.

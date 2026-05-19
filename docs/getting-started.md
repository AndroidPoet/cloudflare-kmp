# Getting Started

Cloudflare KMP has two parts:

- A Kotlin Multiplatform SDK used by Android, iOS, JVM, and web clients.
- A Cloudflare Worker gateway that safely talks to Cloudflare bindings.

The app never receives Cloudflare account API tokens.

## 1. Create the client

```kotlin
val cloudflare = createCloudflareClient(
    workerUrl = "https://api.example.workers.dev",
    publishableKey = "cfpub_live_xxx",
)
```

For logged-in users, provide a bearer token:

```kotlin
val cloudflare = createCloudflareClient(
    workerUrl = "https://api.example.workers.dev",
    publishableKey = "cfpub_live_xxx",
    accessTokenProvider = { sessionStore.currentToken },
)
```

Every request includes:

```http
x-cloudflare-publishable-key: cfpub_live_xxx
Authorization: Bearer optional-user-token
```

## 2. Query D1

```kotlin
@Serializable
data class Todo(
    val id: String,
    val title: String,
    val done: Boolean,
)

val result = cloudflare
    .d1()
    .from("todos")
    .select<Todo> {
        eq("done", "false")
        order("created_at", descending = true)
        limit(25)
    }

result
    .onSuccess { todos -> println(todos) }
    .onFailure { error -> println(error.message) }
```

## 3. Insert into D1

```kotlin
val created = cloudflare
    .d1()
    .from("todos")
    .insert(Todo(id = "todo_1", title = "Ship SDK", done = false))
```

## 4. Read and write KV

```kotlin
val kv = cloudflare.kv()

kv.putText("APP_KV", "settings/theme", "dark")

val theme = kv.get("APP_KV", "settings/theme")
```

Typed JSON:

```kotlin
@Serializable
data class UserProfile(val name: String)

val profile = kv.getJson<UserProfile>("APP_KV", "users/$userId/profile")
```

## 5. Worker routes

The MVP Worker supports:

```text
GET    /health
GET    /d1/{table}?eq.id=123&limit=1
POST   /d1/{table}
PATCH  /d1/{table}?eq.id=123
DELETE /d1/{table}?eq.id=123
GET    /kv/{namespace}/{key}
POST   /kv/{namespace}/{key}
DELETE /kv/{namespace}/{key}
```

## 6. Current limitations

- D1 filters currently support `eq.*` in the Worker template. The SDK has a broader query-builder shape, but the Worker intentionally starts small.
- R2 client APIs exist, but Worker signing is not implemented.
- Realtime client APIs exist, but Durable Object WebSocket routing is not implemented.

# Cloudflare KMP

Kotlin Multiplatform SDK plus a Cloudflare Worker gateway for using Cloudflare as an app backend.

Cloudflare has strong backend primitives: D1, KV, R2, Workers, and Durable Objects. What it does not provide is a mobile-safe `anonKey` model like Supabase. This project adds that missing application layer:

```text
KMP app -> Cloudflare KMP SDK -> your Worker gateway -> D1 / KV / R2 / Durable Objects
```

The app receives only:

```kotlin
val cloudflare = createCloudflareClient(
    workerUrl = "https://api.example.workers.dev",
    publishableKey = "cfpub_live_xxx",
)
```

The real Cloudflare capabilities stay behind Worker bindings.

## Status

Alpha scaffold. The SDK compiles, the Kotlin/JS Worker dry-runs with Wrangler, and D1/KV routes are implemented in the Worker template.

Implemented:

- `CloudflareResult` and typed error model.
- Ktor-based KMP HTTP client.
- Supabase-style D1 table client.
- KV text and typed JSON helpers.
- R2 signed URL SDK shape.
- Realtime SDK surface.
- Kotlin/JS Worker gateway for D1 and KV.

Not implemented yet:

- R2 SigV4 presigned URL generation in the Worker.
- Durable Object WebSocket realtime server.
- Auth policy hooks beyond publishable-key validation.

## Modules

| Module | Purpose |
| --- | --- |
| `cloudflare-core` | Shared config, headers, `CloudflareResult`, errors |
| `cloudflare-client` | HTTP transport and client factory |
| `cloudflare-d1` | D1 table API |
| `cloudflare-kv` | KV text and JSON helpers |
| `cloudflare-r2` | R2 signed URL API shape |
| `cloudflare-realtime` | Realtime channel API surface |
| `worker-template` | Kotlin/JS Worker gateway |

## Quick Start

```kotlin
@Serializable
data class Todo(
    val id: String,
    val title: String,
    val done: Boolean,
)

val cloudflare = createCloudflareClient(
    workerUrl = "https://api.example.workers.dev",
    publishableKey = "cfpub_live_xxx",
)

val todos = cloudflare
    .d1()
    .from("todos")
    .select<Todo> {
        eq("done", "false")
        order("created_at", descending = true)
        limit(25)
    }
```

## Docs

- [Getting Started](docs/getting-started.md)
- [Architecture](docs/architecture.md)
- [Security Model](docs/security.md)
- [API Reference](docs/api-reference.md)
- [Worker Deployment](docs/worker-deployment.md)
- [Publishing](docs/publishing.md)
- [Roadmap](docs/roadmap.md)

## Build

```bash
./gradlew :cloudflare-core:jvmTest \
  :cloudflare-client:compileKotlinJvm \
  :cloudflare-d1:compileKotlinJvm \
  :cloudflare-kv:compileKotlinJvm \
  :cloudflare-r2:compileKotlinJvm \
  :cloudflare-realtime:compileKotlinJvm \
  :worker-template:jsProductionExecutableCompileSync
```

Worker dry-run:

```bash
cd worker-template
wrangler deploy --dry-run --outdir dist
```

## License

License has not been selected yet. Pick one before publishing publicly.

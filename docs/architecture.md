# Architecture

Cloudflare KMP is not a direct Cloudflare REST API wrapper.

It is an SDK plus gateway architecture:

```text
Client app
  -> cloudflare-kmp SDK
  -> Cloudflare Worker gateway
  -> Cloudflare bindings
       - D1
       - KV
       - R2
       - Durable Objects
```

## Why a Worker gateway exists

Supabase exposes an app-safe `anonKey`. Cloudflare account API tokens are not app-safe. A mobile app must not hold credentials that can administer D1, KV, R2, or Workers.

The Worker gateway gives us a safe app-facing contract:

```text
workerUrl + publishableKey
```

The Worker then talks to resources through bindings:

```kotlin
env.DB
env.APP_KV
env.MY_BUCKET
env.REALTIME_ROOM
```

Cloudflare bindings act as both permission and API. The app cannot access them directly.

## SDK modules

`cloudflare-core` owns shared primitives:

- `CloudflareConfig`
- `CloudflareHeaders`
- `CloudflareResult`
- `CloudflareError`

`cloudflare-client` owns transport:

- Ktor HTTP client
- publishable-key header
- optional bearer auth
- typed JSON decode helpers

Product modules build on top:

- `cloudflare-d1`
- `cloudflare-kv`
- `cloudflare-r2`
- `cloudflare-realtime`

## Worker template

The Worker template is Kotlin/JS compiled to ES modules and exposed through a tiny module adapter:

```text
worker-template/src/jsMain/kotlin/.../Main.kt
worker-template/src/worker.mjs
```

`wrangler.toml` points at `src/worker.mjs`.

This keeps the Worker entrypoint compatible with modern Cloudflare module Workers while still letting the app gateway be authored in Kotlin.

## Realtime design

The planned realtime model is:

```text
SDK WebSocket client
  -> Worker /realtime/{room}
  -> Durable Object per room/channel
  -> broadcast, presence, table-change events
```

D1 does not currently provide automatic Supabase-style row-level realtime. To support table-change events, writes must pass through the Worker so the Worker can broadcast the mutation after a successful write.

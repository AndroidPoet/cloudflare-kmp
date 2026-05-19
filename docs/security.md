# Security Model

The core rule:

```text
Never put Cloudflare account API tokens in a mobile or browser app.
```

Cloudflare KMP uses a Worker gateway because Cloudflare resources are accessed through server-side bindings.

## Public configuration

Apps may receive:

```text
workerUrl
publishableKey
```

Example:

```kotlin
createCloudflareClient(
    workerUrl = "https://api.example.workers.dev",
    publishableKey = "cfpub_live_xxx",
)
```

The `publishableKey` is not a Cloudflare credential. It only identifies allowed client apps to your Worker.

## Private configuration

Keep these server-side:

- Cloudflare account API tokens
- R2 S3 access key ID
- R2 S3 secret access key
- Worker secrets
- D1/KV/R2 bindings

## Request validation

The MVP Worker validates:

```http
x-cloudflare-publishable-key
```

Next-stage auth should validate:

```http
Authorization: Bearer user-jwt
```

Recommended policy layers:

- Publishable key: app/project-level access.
- User JWT: user identity.
- Route policy: what tables, namespaces, buckets, and channels this user can access.
- Row policy: tenant/user scoped filters injected by the Worker.

## D1 safety

The Worker template validates table and column identifiers with:

```text
^[A-Za-z_][A-Za-z0-9_]*$
```

Values are passed through prepared-statement bindings.

Do not expose arbitrary SQL execution to clients.

## KV safety

The MVP Worker allows namespace names from the URL and looks them up on `env`.

Before production, restrict namespaces through a server-side allowlist:

```text
APP_KV only
```

or map public aliases to private binding names:

```text
profiles -> APP_KV
```

## R2 safety

Presigned URLs should be short lived and scoped to:

- bucket
- object path
- method
- content type for uploads
- user/tenant prefix

Do not let clients request arbitrary bucket names or paths in production.

# Worker Deployment

The Worker gateway lives in `worker-template`.

## Build

From the repo root:

```bash
./gradlew :worker-template:jsProductionExecutableCompileSync
```

## Configure bindings

Edit `worker-template/wrangler.toml`:

```toml
name = "cloudflare-kmp-worker"
main = "src/worker.mjs"
compatibility_date = "2026-05-19"
workers_dev = true

[vars]
PUBLISHABLE_KEY = "cfpub_dev_replace_me"

[[d1_databases]]
binding = "DB"
database_name = "cloudflare_kmp_dev"
database_id = "replace-me"

[[kv_namespaces]]
binding = "APP_KV"
id = "replace-me"
```

Create resources:

```bash
wrangler d1 create cloudflare_kmp_dev
wrangler kv namespace create APP_KV
```

Copy the generated IDs into `wrangler.toml`.

## Dry-run

```bash
cd worker-template
wrangler deploy --dry-run --outdir dist
```

Expected bindings:

```text
env.APP_KV
env.DB
env.PUBLISHABLE_KEY
```

## Deploy

```bash
cd worker-template
npm install
npm run build
wrangler deploy
```

## Smoke test

```bash
curl "https://cloudflare-kmp-worker.<subdomain>.workers.dev/health" \
  -H "x-cloudflare-publishable-key: cfpub_dev_replace_me"
```

Expected:

```json
{"ok":true}
```

## D1 table example

Create a table:

```bash
wrangler d1 execute cloudflare_kmp_dev --command '
CREATE TABLE todos (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  done INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
'
```

Insert:

```bash
curl -X POST "https://cloudflare-kmp-worker.<subdomain>.workers.dev/d1/todos" \
  -H "content-type: application/json" \
  -H "x-cloudflare-publishable-key: cfpub_dev_replace_me" \
  -d '{"id":"todo_1","title":"Ship Cloudflare KMP","done":0}'
```

Select:

```bash
curl "https://cloudflare-kmp-worker.<subdomain>.workers.dev/d1/todos?eq.id=todo_1" \
  -H "x-cloudflare-publishable-key: cfpub_dev_replace_me"
```

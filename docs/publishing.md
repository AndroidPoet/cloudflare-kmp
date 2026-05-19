# Publishing

Publishing has three separate targets. Do not mix them up.

## 1. Publish the Worker

This deploys your backend gateway to Cloudflare.

Prerequisites:

- Cloudflare account.
- Wrangler authenticated with `wrangler login`.
- Real D1 database ID in `worker-template/wrangler.toml`.
- Real KV namespace ID in `worker-template/wrangler.toml`.
- Production `PUBLISHABLE_KEY`.

Commands:

```bash
cd worker-template
npm install
npm run build
wrangler deploy
```

This cannot be done safely until the Cloudflare account/resources are selected.

## 2. Publish the SDK

The SDK is not configured for Maven Central yet.

Before publishing:

- Choose package coordinates.
- Choose a license.
- Add Maven publishing metadata.
- Add signing configuration.
- Add CI release workflow.
- Run all target builds.

Suggested coordinates:

```text
io.github.androidpoet:cloudflare-core
io.github.androidpoet:cloudflare-client
io.github.androidpoet:cloudflare-d1
io.github.androidpoet:cloudflare-kv
io.github.androidpoet:cloudflare-r2
io.github.androidpoet:cloudflare-realtime
```

Current first version:

```text
0.1.0-alpha01
```

## 3. Publish the repository

Before pushing publicly:

- Add a license.
- Initialize Git.
- Create a GitHub repo.
- Push the default branch.
- Add topics: `kotlin`, `kmp`, `cloudflare`, `d1`, `workers`, `r2`, `kv`.

Commands after creating the GitHub repo:

```bash
git init
git add .
git commit -m "Initial Cloudflare KMP scaffold"
git branch -M main
git remote add origin git@github.com:AndroidPoet/cloudflare-kmp.git
git push -u origin main
```

## Release checklist

- `./gradlew :cloudflare-core:jvmTest`
- `./gradlew :cloudflare-client:compileKotlinJvm`
- `./gradlew :cloudflare-d1:compileKotlinJvm`
- `./gradlew :cloudflare-kv:compileKotlinJvm`
- `./gradlew :cloudflare-r2:compileKotlinJvm`
- `./gradlew :cloudflare-realtime:compileKotlinJvm`
- `./gradlew :worker-template:jsProductionExecutableCompileSync`
- `cd worker-template && wrangler deploy --dry-run --outdir dist`

## What is blocked right now

Actual external publishing requires:

- GitHub repository target.
- Maven Central credentials.
- Signing key.
- Cloudflare account/resources, if Worker deployment is desired.

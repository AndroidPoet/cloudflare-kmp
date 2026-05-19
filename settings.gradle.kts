pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cloudflare-kmp"

include(":cloudflare-core")
include(":cloudflare-client")
include(":cloudflare-d1")
include(":cloudflare-r2")
include(":cloudflare-kv")
include(":cloudflare-realtime")
include(":worker-template")

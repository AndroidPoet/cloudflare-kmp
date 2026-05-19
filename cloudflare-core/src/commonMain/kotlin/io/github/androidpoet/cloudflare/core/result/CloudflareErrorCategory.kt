package io.github.androidpoet.cloudflare.core.result

public enum class CloudflareErrorCategory {
    Unauthorized,
    Forbidden,
    NotFound,
    Conflict,
    RateLimited,
    Server,
    Network,
    Serialization,
    Unknown,
}

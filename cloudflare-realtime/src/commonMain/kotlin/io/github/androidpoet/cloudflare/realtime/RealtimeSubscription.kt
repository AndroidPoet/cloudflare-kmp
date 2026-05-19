package io.github.androidpoet.cloudflare.realtime

public interface RealtimeSubscription {
    public val channel: String

    public suspend fun unsubscribe()
}

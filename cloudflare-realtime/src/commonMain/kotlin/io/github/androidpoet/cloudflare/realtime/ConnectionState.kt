package io.github.androidpoet.cloudflare.realtime

public sealed interface ConnectionState {
    public data object Disconnected : ConnectionState
    public data object Connecting : ConnectionState
    public data object Connected : ConnectionState
    public data class Failed(public val reason: String) : ConnectionState
}

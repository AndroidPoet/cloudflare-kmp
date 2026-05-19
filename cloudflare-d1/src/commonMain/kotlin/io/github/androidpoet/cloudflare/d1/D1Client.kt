package io.github.androidpoet.cloudflare.d1

import io.github.androidpoet.cloudflare.client.CloudflareClient

public class D1Client(
    private val client: CloudflareClient,
) {
    public fun from(table: String): D1Table = D1Table(client, table)
}

public fun CloudflareClient.d1(): D1Client = D1Client(this)

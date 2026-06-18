package io.github.androidpoet.cloudflare.d1

public class D1QueryBuilder {
    private val params = linkedMapOf<String, String>()

    public fun eq(column: String, value: String) {
        params["eq.$column"] = value
    }

    public fun neq(column: String, value: String) {
        params["neq.$column"] = value
    }

    public fun gt(column: String, value: String) {
        params["gt.$column"] = value
    }

    public fun gte(column: String, value: String) {
        params["gte.$column"] = value
    }

    public fun lt(column: String, value: String) {
        params["lt.$column"] = value
    }

    public fun lte(column: String, value: String) {
        params["lte.$column"] = value
    }

    public fun order(column: String, descending: Boolean = false) {
        params["order"] = if (descending) "$column.desc" else "$column.asc"
    }

    public fun limit(count: Int) {
        require(count > 0) { "limit must be greater than zero." }
        params["limit"] = count.toString()
    }

    @PublishedApi
    internal fun build(): Map<String, String> = params.toMap()
}

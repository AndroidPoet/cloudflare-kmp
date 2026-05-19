package io.github.androidpoet.cloudflare.worker

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import kotlin.js.Promise
import org.w3c.fetch.Request
import org.w3c.fetch.Response
import org.w3c.fetch.ResponseInit

private val identifierPattern = Regex("^[A-Za-z_][A-Za-z0-9_]*$")

@OptIn(ExperimentalJsExport::class)
@JsExport
fun fetch(request: Request, env: dynamic): Promise<Response> =
    GlobalScope.promise {
        handleRequest(request, env)
    }

private suspend fun handleRequest(request: Request, env: dynamic): Response {
    if (!isAuthorized(request, env)) {
        return json("""{"error":"Invalid publishable key"}""", status = 401)
    }

    val url = URL(request.url)
    val method = request.method.uppercase()
    val segments = url.pathname.trim('/').split('/').filter { it.isNotBlank() }

    return when {
        segments.firstOrNull() == "health" -> json("""{"ok":true}""")
        segments.firstOrNull() == "d1" && segments.size == 2 -> handleD1(method, segments[1], url, request, env)
        segments.firstOrNull() == "kv" && segments.size >= 3 -> handleKv(method, segments[1], segments.drop(2).joinToString("/"), request, env)
        segments.firstOrNull() == "r2" -> json("""{"error":"R2 signing is not implemented in the MVP Worker template"}""", status = 501)
        else -> json("""{"error":"Route not found"}""", status = 404)
    }
}

private fun isAuthorized(request: Request, env: dynamic): Boolean {
    val expectedKey = env.PUBLISHABLE_KEY as? String ?: return true
    val providedKey = request.headers.get("x-cloudflare-publishable-key")
    return providedKey == expectedKey
}

private suspend fun handleD1(
    method: String,
    table: String,
    url: URL,
    request: Request,
    env: dynamic,
): Response {
    if (!isSafeIdentifier(table)) {
        return json("""{"error":"Invalid table name"}""", status = 400)
    }

    val database = env.DB ?: return json("""{"error":"D1 binding DB is missing"}""", status = 500)
    return when (method) {
        "GET" -> selectRows(database, table, url)
        "POST" -> insertRow(database, table, request)
        "PATCH" -> updateRows(database, table, url, request)
        "DELETE" -> deleteRows(database, table, url)
        else -> json("""{"error":"Method not allowed"}""", status = 405)
    }
}

private suspend fun selectRows(database: dynamic, table: String, url: URL): Response {
    val where = buildWhereClause(url)
    val order = buildOrderClause(url)
    val limit = buildLimitClause(url)
    val statement = database.prepare("SELECT * FROM $table${where.sql}$order$limit")
    val result = bindAll(statement, where.values).all().unsafeCast<Promise<dynamic>>().await()
    return json(JSON.stringify(result.results ?: emptyArray<dynamic>()))
}

private suspend fun insertRow(database: dynamic, table: String, request: Request): Response {
    val bodyText: String = request.text().await()
    val body = js("JSON").parse(bodyText)
    val columns = objectKeys(body).filter(::isSafeIdentifier)
    if (columns.isEmpty()) {
        return json("""{"error":"Request body must contain at least one safe column"}""", status = 400)
    }

    val placeholders = columns.joinToString(", ") { "?" }
    val columnSql = columns.joinToString(", ")
    val values = columns.map { body[it] }.toTypedArray()
    val statement = database.prepare("INSERT INTO $table ($columnSql) VALUES ($placeholders) RETURNING *")
    val result = bindAll(statement, values).all().unsafeCast<Promise<dynamic>>().await()
    val inserted = firstResultOrEmpty(result.results)
    return json(JSON.stringify(inserted), status = 201)
}

private suspend fun updateRows(database: dynamic, table: String, url: URL, request: Request): Response {
    val bodyText: String = request.text().await()
    val body = js("JSON").parse(bodyText)
    val columns = objectKeys(body).filter(::isSafeIdentifier)
    if (columns.isEmpty()) {
        return json("""{"error":"Request body must contain at least one safe column"}""", status = 400)
    }

    val where = buildWhereClause(url)
    if (where.values.isEmpty()) {
        return json("""{"error":"PATCH requires at least one filter"}""", status = 400)
    }

    val assignments = columns.joinToString(", ") { "$it = ?" }
    val values = columns.map { body[it] }.toTypedArray() + where.values
    val statement = database.prepare("UPDATE $table SET $assignments${where.sql} RETURNING *")
    val result = bindAll(statement, values).all().unsafeCast<Promise<dynamic>>().await()
    return json(JSON.stringify(result.results ?: emptyArray<dynamic>()))
}

private suspend fun deleteRows(database: dynamic, table: String, url: URL): Response {
    val where = buildWhereClause(url)
    if (where.values.isEmpty()) {
        return json("""{"error":"DELETE requires at least one filter"}""", status = 400)
    }

    val statement = database.prepare("DELETE FROM $table${where.sql}")
    bindAll(statement, where.values).run().unsafeCast<Promise<dynamic>>().await()
    return json("""{"ok":true}""")
}

private suspend fun handleKv(
    method: String,
    namespace: String,
    key: String,
    request: Request,
    env: dynamic,
): Response {
    val binding = env[namespace] ?: return json("""{"error":"KV namespace binding not found"}""", status = 404)
    return when (method) {
        "GET" -> {
            val value = binding.get(key).unsafeCast<Promise<String?>>().await()
            if (value == null) json("""{"error":"Key not found"}""", status = 404) else text(value)
        }
        "POST" -> {
            binding.put(key, request.text().await()).unsafeCast<Promise<dynamic>>().await()
            json("""{"ok":true}""")
        }
        "DELETE" -> {
            binding.delete(key).unsafeCast<Promise<dynamic>>().await()
            json("""{"ok":true}""")
        }
        else -> json("""{"error":"Method not allowed"}""", status = 405)
    }
}

private data class WhereClause(
    val sql: String,
    val values: Array<dynamic>,
)

private fun buildWhereClause(url: URL): WhereClause {
    val filters = mutableListOf<String>()
    val values = mutableListOf<dynamic>()
    for (entry in searchParamEntries(url.searchParams)) {
        val key = entry[0] as String
        val value = entry[1]
        if (!key.startsWith("eq.")) continue

        val column = key.removePrefix("eq.")
        if (!isSafeIdentifier(column)) continue

        filters += "$column = ?"
        values.add(value)
    }

    return if (filters.isEmpty()) {
        WhereClause("", emptyArray())
    } else {
        WhereClause(" WHERE ${filters.joinToString(" AND ")}", values.toTypedArray())
    }
}

private fun buildOrderClause(url: URL): String {
    val order = url.searchParams.get("order") ?: return ""
    val parts = order.split('.')
    val column = parts.firstOrNull() ?: return ""
    if (!isSafeIdentifier(column)) return ""

    val direction = if (parts.getOrNull(1) == "desc") "DESC" else "ASC"
    return " ORDER BY $column $direction"
}

private fun buildLimitClause(url: URL): String {
    val limit = url.searchParams.get("limit")?.toIntOrNull() ?: return ""
    return if (limit > 0) " LIMIT $limit" else ""
}

private fun bindAll(statement: dynamic, values: Array<dynamic>): dynamic =
    if (values.isEmpty()) statement else statement.bind.apply(statement, values)

private fun firstResultOrEmpty(results: dynamic): dynamic =
    if (js("Array").isArray(results) as Boolean && results.length > 0) results[0] else js("{}")

private fun isSafeIdentifier(value: String): Boolean = identifierPattern.matches(value)

private fun json(body: String, status: Int = 200): Response =
    response(body, status, "application/json")

private fun text(body: String, status: Int = 200): Response =
    response(body, status, "text/plain; charset=utf-8")

private fun response(body: String, status: Int, contentType: String): Response {
    val headers: dynamic = object {}
    headers["content-type"] = contentType

    val init: dynamic = object {}
    init.status = status
    init.headers = headers

    return Response(body, init.unsafeCast<ResponseInit>())
}

private fun objectKeys(value: dynamic): Array<String> =
    js("Object").keys(value).unsafeCast<Array<String>>()

private fun searchParamEntries(value: dynamic): Array<dynamic> =
    js("Array").from(value.entries()).unsafeCast<Array<dynamic>>()

private external class URL(url: String) {
    val pathname: String
    val searchParams: URLSearchParams
}

private external class URLSearchParams {
    fun get(name: String): String?
}

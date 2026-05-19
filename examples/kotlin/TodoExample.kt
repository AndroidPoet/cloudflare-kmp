import io.github.androidpoet.cloudflare.client.createCloudflareClient
import io.github.androidpoet.cloudflare.core.result.onFailure
import io.github.androidpoet.cloudflare.core.result.onSuccess
import io.github.androidpoet.cloudflare.d1.d1
import kotlinx.serialization.Serializable

@Serializable
data class Todo(
    val id: String,
    val title: String,
    val done: Boolean,
)

suspend fun main() {
    val cloudflare = createCloudflareClient(
        workerUrl = "https://api.example.workers.dev",
        publishableKey = "cfpub_live_xxx",
    )

    cloudflare
        .d1()
        .from("todos")
        .select<Todo> {
            eq("done", "false")
            order("created_at", descending = true)
            limit(25)
        }
        .onSuccess { todos ->
            println(todos)
        }
        .onFailure { error ->
            println(error.message)
        }
}

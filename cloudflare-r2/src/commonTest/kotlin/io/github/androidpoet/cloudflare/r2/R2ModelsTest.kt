package io.github.androidpoet.cloudflare.r2

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class R2ModelsTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

    @Test
    fun test_bucket_decodesSnakeCaseFields() {
        val raw =
            """{"name":"avatars","creation_date":"2024-05-01T00:00:00Z",""" +
                """"location":"wnam","storage_class":"Standard","jurisdiction":"default"}"""
        val bucket = json.decodeFromString(R2Bucket.serializer(), raw)

        assertEquals("avatars", bucket.name)
        assertEquals("2024-05-01T00:00:00Z", bucket.creationDate)
        assertEquals("wnam", bucket.location)
        assertEquals("Standard", bucket.storageClass)
    }

    @Test
    fun test_bucketList_decodesBucketsArray() {
        val raw = """{"buckets":[{"name":"a"},{"name":"b"}]}"""
        val list = json.decodeFromString(R2BucketList.serializer(), raw)

        assertEquals(2, list.buckets.size)
        assertEquals("a", list.buckets.first().name)
    }

    @Test
    fun test_createRequest_serializesLocationHintAndStorageClass() {
        val encoded =
            json.encodeToString(
                R2CreateBucketRequest.serializer(),
                R2CreateBucketRequest(name = "b", locationHint = "weur", storageClass = "InfrequentAccess"),
            )
        assertTrue(encoded.contains("\"name\":\"b\""))
        assertTrue(encoded.contains("\"locationHint\":\"weur\""))
        assertTrue(encoded.contains("\"storageClass\":\"InfrequentAccess\""))
    }

    @Test
    fun test_createRequest_omitsNullOptionalFields() {
        val encoded =
            json.encodeToString(
                R2CreateBucketRequest.serializer(),
                R2CreateBucketRequest(name = "b"),
            )
        assertEquals("{\"name\":\"b\"}", encoded)
    }
}

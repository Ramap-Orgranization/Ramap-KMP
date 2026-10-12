package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assume.assumeTrue
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.time.measureTimedValue

/** Explicitly opted in real HTTP benchmark. Never run against production in ordinary CI. */
@OptIn(SupabaseInternal::class)
class ImageUrlLiveBenchmarkTest {
    @Test
    fun compareActualDataSources() =
        runBlocking {
            assumeTrue(System.getenv("RAMAP_RUN_IMAGE_BENCHMARK") == "1")
            val root = File(requireNotNull(System.getenv("RAMAP_BENCHMARK_ROOT")))
            val fixtureFile = File(requireNotNull(System.getenv("RAMAP_BENCHMARK_FIXTURE")))
            val output = File(requireNotNull(System.getenv("RAMAP_BENCHMARK_OUTPUT")))
            check(!output.exists()) { "Refusing to overwrite evidence" }
            val properties = Properties().apply { File(root, "local.properties").inputStream().use(::load) }
            val base = properties.getProperty("supabase.url")
            val key = properties.getProperty("supabase.anon_key")
            val jwt =
                Json
                    .parseToJsonElement(
                        String(
                            java.util.Base64
                                .getUrlDecoder()
                                .decode(key.split('.')[1]),
                        ),
                    ).jsonObject
            check(jwt.getValue("role").jsonPrimitive.content == "anon") { "Only anon allowed" }
            val reviews =
                Json
                    .parseToJsonElement(fixtureFile.readText())
                    .jsonObject
                    .getValue("reviews")
                    .jsonArray
            val images =
                reviews.flatMap { review ->
                    val record = review.jsonObject
                    val entries = record.getValue("images").jsonArray.toList() + listOfNotNull(record["avatar"]?.takeUnless { it == JsonNull })
                    entries.map { entry ->
                        SignedImageRequest(
                            entry.jsonObject
                                .getValue("bucket")
                                .jsonPrimitive.content,
                            entry.jsonObject
                                .getValue("path")
                                .jsonPrimitive.content,
                        )
                    }
                }
            check(images.distinct().size == 22 && images.size == 41) { "Unexpected workload" }
            val storageRequests = AtomicInteger()
            val edgeRequests = AtomicInteger()
            val metrics = AtomicReference<JsonObject?>()
            val counter =
                createClientPlugin("BenchmarkRequestCounter") {
                    onRequest { request, _ ->
                        when {
                            request.url
                                .build()
                                .encodedPath
                                .startsWith("/storage/v1/object/sign/") -> storageRequests.incrementAndGet()
                            request.url.build().encodedPath == "/functions/v1/image-signed-urls" -> edgeRequests.incrementAndGet()
                            else -> error("Unexpected network request")
                        }
                    }
                    onResponse { response ->
                        response.headers["x-image-metrics"]?.let { metrics.set(Json.parseToJsonElement(it).jsonObject) }
                    }
                }
            val client =
                createSupabaseClient(base, key) {
                    defaultLogLevel = LogLevel.NONE
                    install(Storage)
                    install(Functions)
                    install(Auth) {
                        minimalConfig()
                        autoSetupPlatform = false
                    }
                    httpConfig { install(counter) }
                }
            client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
            val direct = RemoteImageUrlDataSource(client)
            val legacy = LegacyImageUrlBenchmarkDataSource(client)
            val samples = mutableListOf<JsonObject>()
            val startedAt = Instant.now().toString()
            var completed = false
            val reportJson = Json { prettyPrint = true }

            fun save() {
                val report =
                    buildJsonObject {
                        put("schemaVersion", 1)
                        put("startedAt", startedAt)
                        put("updatedAt", Instant.now().toString())
                        put("completed", completed)
                        put("platform", "Android host JVM / macOS / OkHttp; not an emulator or UI timing")
                        put("fixtureSha256", sha256(fixtureFile.readBytes()))
                        put("productionSourceSha256", sha256(File(root, "data/src/commonMain/kotlin/com/peto/ramap/data/datasource/image/RemoteImageUrlDataSource.kt").readBytes()))
                        put("legacySourceSha256", sha256(File(root, "data/src/androidHostTest/kotlin/com/peto/ramap/data/datasource/image/LegacyImageUrlBenchmarkDataSource.kt").readBytes()))
                        put("benchmarkSourceSha256", sha256(File(root, "data/src/androidHostTest/kotlin/com/peto/ramap/data/datasource/image/ImageUrlLiveBenchmarkTest.kt").readBytes()))
                        put("occurrences", images.size)
                        put("unique", images.distinct().size)
                        put("rounds", 30)
                        put("delayMs", 1500)
                        put("retries", 0)
                        put("samples", JsonArray(samples))
                    }
                output.writeText(reportJson.encodeToString(JsonObject.serializer(), report))
            }

            suspend fun sample(
                variant: String,
                round: Int,
            ) {
                delay(1500)
                storageRequests.set(0)
                edgeRequests.set(0)
                metrics.set(null)
                var errors = 22
                var elapsedMs = 0.0
                try {
                    val measured = measureTimedValue { (if (variant == "native_batch") direct else legacy).fetchUrls(images) }
                    elapsedMs = measured.duration.inWholeMicroseconds / 1000.0
                    errors = images.distinct().count { measured.value[it] == null }
                } finally {
                    samples.add(
                        buildJsonObject {
                            put("variant", variant)
                            put("round", round)
                            put("at", Instant.now().toString())
                            put("wallMs", elapsedMs)
                            put("errors", errors)
                            put("storageHttpRequests", storageRequests.get())
                            put("edgeHttpRequests", edgeRequests.get())
                            put("server", metrics.get() ?: JsonNull)
                        },
                    )
                    save()
                }
                check(errors == 0) { "Image failure; partial evidence retained" }
                if (variant == "native_batch") check(storageRequests.get() == 2 && edgeRequests.get() == 0)
                if (variant == "redis_warm" && round > 0) {
                    check(
                        metrics
                            .get()
                            ?.get("cacheHits")
                            ?.jsonPrimitive
                            ?.content == "22",
                    ) { "Cache state mismatch; evidence retained" }
                    check(
                        metrics
                            .get()
                            ?.get("issued")
                            ?.jsonPrimitive
                            ?.content == "0",
                    )
                }
            }
            try {
                save()
                sample("native_batch", 0)
                sample("redis_warm", 0)
                for (round in 1..30) {
                    val order = if (round % 2 == 1) listOf("native_batch", "redis_warm") else listOf("redis_warm", "native_batch")
                    for (variant in order) sample(variant, round)
                }
                completed = true
                save()
            } finally {
                client.close()
            }
        }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

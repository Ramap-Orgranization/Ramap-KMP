package com.peto.ramap.network.client.media

import com.peto.ramap.network.config.RamapSecrets
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class MediaStorageClient(
    private val supabase: SupabaseClient,
    private val http: HttpClient,
    private val baseUrl: String = RamapSecrets.mediaApiUrl,
    private val r2AccountId: String = RamapSecrets.r2AccountId,
    private val r2PrivateBucket: String = RamapSecrets.r2PrivateBucket,
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && R2_ACCOUNT_PATTERN.matches(r2AccountId) && r2PrivateBucket.isNotBlank()

    suspend fun upload(
        bucket: String,
        path: String,
        bytes: ByteArray,
        mimeType: String,
    ) {
        if (!isConfigured) {
            uploadLegacy(bucket, path, bytes, mimeType)
            return
        }
        val userId = currentUserId()
        val prepared =
            request(
                ACTION_PREPARE,
                buildPreparePayload(bucket, path, mimeType, bytes.size),
            ).jsonObject
        checkSessionUnchanged(userId)

        if (prepared[KEY_PROVIDER]?.jsonPrimitive?.content == PROVIDER_SUPABASE) {
            uploadLegacy(bucket, path, bytes, mimeType)
            return
        }
        if (prepared[KEY_READY]?.jsonPrimitive?.content == VALUE_TRUE) return

        val id = requireNotNull(prepared[KEY_ID]?.jsonPrimitive?.content)
        val target = Url(requireNotNull(prepared[KEY_URL]?.jsonPrimitive?.content))
        val provider = prepared[KEY_PROVIDER]?.jsonPrimitive?.content
        val validTarget = provider == PROVIDER_R2 && validR2Url(target) && target.encodedPath == "/$r2PrivateBucket/$PATH_PENDING/$id"
        check(validTarget) { ERROR_INVALID_TARGET }

        val response =
            http.put(target) {
                contentType(ContentType.parse(mimeType))
                header(HttpHeaders.IfNoneMatch, VALUE_ANY)
                setBody(bytes)
            }
        check(response.status.isSuccess() || response.status.value == HTTP_PRECONDITION_FAILED) { ERROR_UPLOAD_FAILED }
        checkSessionUnchanged(userId)

        request(ACTION_FINALIZE, buildFinalizePayload(id))
        checkSessionUnchanged(userId)
    }

    suspend fun delete(
        bucket: String,
        path: String,
    ) {
        if (!isConfigured) {
            supabase.storage.from(bucket).delete(path)
            return
        }
        request(
            ACTION_DELETE,
            buildDeletePayload(bucket, path),
        )
    }

    suspend fun fetchUrls(
        bucket: String,
        paths: List<String>,
    ): List<MediaImageUrl> {
        require(paths.size <= MAX_BATCH_SIZE)
        val userId = currentUserId()
        val result =
            request(
                ACTION_RESOLVE,
                buildResolvePayload(bucket, paths),
            ).jsonArray
        checkSessionUnchanged(userId)

        val now = Clock.System.now()
        return result.mapNotNull { element ->
            parseResolvedUrl(element.jsonObject, bucket, paths, now)
        }
    }

    private fun parseResolvedUrl(
        row: JsonObject,
        bucket: String,
        paths: List<String>,
        now: Instant,
    ): MediaImageUrl? {
        val path = row[KEY_PATH]?.jsonPrimitive?.contentOrNull ?: return null
        if (path !in paths || row[KEY_BUCKET]?.jsonPrimitive?.content != bucket) return null

        val url = row[KEY_URL]?.jsonPrimitive?.contentOrNull?.let { runCatching { Url(it) }.getOrNull() } ?: return null
        val expiry = row[KEY_EXPIRES_AT]?.jsonPrimitive?.contentOrNull?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null

        if (expiry <= now + MIN_EXPIRY_MARGIN || expiry > now + MAX_EXPIRY_MARGIN) return null

        val legacy = Url(supabase.storage.resolveUrl("$LEGACY_SIGN_PATH_PREFIX/$bucket/$path"))
        val validLegacy =
            url.protocol == legacy.protocol &&
                url.host == legacy.host &&
                url.port == legacy.port &&
                url.encodedPath == legacy.encodedPath &&
                !url.parameters[PARAM_TOKEN].isNullOrBlank()

        val validR2 = validR2Url(url) && url.encodedPath == "/$r2PrivateBucket/$bucket/$path"
        if ((!validLegacy && !validR2) || url.fragment.isNotEmpty() || !url.user.isNullOrEmpty() || !url.password.isNullOrEmpty()) return null

        return MediaImageUrl(path, url.toString(), expiry)
    }

    private suspend fun request(
        action: String,
        body: JsonObject,
    ): JsonElement {
        validateEndpointUrl(baseUrl)
        val response =
            http.post("${baseUrl.trimEnd('/')}/$API_VERSION_R2/$action") {
                contentType(ContentType.Application.Json)
                supabase.auth.currentAccessTokenOrNull()?.let { header(HttpHeaders.Authorization, "$AUTH_SCHEME_BEARER $it") }
                setBody(body.toString())
            }
        check(response.status.isSuccess()) { ERROR_REQUEST_FAILED }
        return Json.parseToJsonElement(response.bodyAsText())
    }

    private fun validateEndpointUrl(url: String) {
        val endpoint = Url(url)
        check(
            endpoint.protocol.name == PROTOCOL_HTTPS &&
                endpoint.user.isNullOrEmpty() &&
                endpoint.password.isNullOrEmpty() &&
                endpoint.parameters.isEmpty() &&
                endpoint.fragment.isEmpty(),
        ) { ERROR_INVALID_TARGET }
    }

    private fun validR2Url(url: Url): Boolean =
        R2_ACCOUNT_PATTERN.matches(r2AccountId) &&
            r2PrivateBucket.isNotBlank() &&
            url.protocol.name == PROTOCOL_HTTPS &&
            url.host == "$r2AccountId.r2.cloudflarestorage.com" &&
            url.port == PORT_HTTPS &&
            url.user.isNullOrEmpty() &&
            url.password.isNullOrEmpty() &&
            url.fragment.isEmpty() &&
            url.parameters[PARAM_X_AMZ_ALGORITHM] == VALUE_AWS_SIGNATURE &&
            !url.parameters[PARAM_X_AMZ_SIGNATURE].isNullOrBlank()

    private suspend fun uploadLegacy(
        bucket: String,
        path: String,
        bytes: ByteArray,
        mimeType: String,
    ) {
        supabase.storage.from(bucket).upload(path, bytes) { contentType = ContentType.parse(mimeType) }
    }

    private fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    private fun checkSessionUnchanged(expectedUserId: String?) {
        check(currentUserId() == expectedUserId) { ERROR_SESSION_CHANGED }
    }

    private fun buildPreparePayload(
        bucket: String,
        path: String,
        mimeType: String,
        byteSize: Int,
    ): JsonObject =
        buildJsonObject {
            put(KEY_BUCKET, bucket)
            put(KEY_PATH, path)
            put(KEY_CONTENT_TYPE, mimeType)
            put(KEY_BYTE_SIZE, byteSize)
        }

    private fun buildFinalizePayload(id: String): JsonObject =
        buildJsonObject {
            put(KEY_ID, id)
        }

    private fun buildDeletePayload(
        bucket: String,
        path: String,
    ): JsonObject =
        buildJsonObject {
            put(KEY_BUCKET, bucket)
            put(KEY_PATH, path)
        }

    private fun buildResolvePayload(
        bucket: String,
        paths: List<String>,
    ): JsonObject =
        buildJsonObject {
            put(
                KEY_IMAGES,
                buildJsonArray {
                    paths.forEach { path ->
                        add(
                            buildJsonObject {
                                put(KEY_BUCKET, bucket)
                                put(KEY_PATH, path)
                            },
                        )
                    }
                },
            )
        }

    private companion object {
        const val MAX_BATCH_SIZE = 64
        const val API_VERSION_R2 = "v2"

        const val ACTION_PREPARE = "prepare"
        const val ACTION_FINALIZE = "finalize"
        const val ACTION_DELETE = "delete"
        const val ACTION_RESOLVE = "resolve"

        const val KEY_BUCKET = "bucket"
        const val KEY_PATH = "path"
        const val KEY_CONTENT_TYPE = "content_type"
        const val KEY_BYTE_SIZE = "byte_size"
        const val KEY_PROVIDER = "provider"
        const val KEY_READY = "ready"
        const val KEY_ID = "id"
        const val KEY_URL = "url"
        const val KEY_IMAGES = "images"
        const val KEY_EXPIRES_AT = "expires_at"

        const val PROVIDER_SUPABASE = "supabase"
        const val PROVIDER_R2 = "r2"
        const val VALUE_ANY = "*"
        const val PARAM_X_AMZ_SIGNATURE = "X-Amz-Signature"
        const val PARAM_X_AMZ_ALGORITHM = "X-Amz-Algorithm"
        const val VALUE_AWS_SIGNATURE = "AWS4-HMAC-SHA256"
        val R2_ACCOUNT_PATTERN = Regex("[a-f0-9]{32}")
        const val VALUE_TRUE = "true"

        const val PATH_PENDING = "pending"
        const val LEGACY_SIGN_PATH_PREFIX = "object/sign"

        const val PROTOCOL_HTTPS = "https"
        const val PORT_HTTPS = 443
        const val HTTP_PRECONDITION_FAILED = 412

        const val AUTH_SCHEME_BEARER = "Bearer"
        const val PARAM_TOKEN = "token"

        val MIN_EXPIRY_MARGIN = 1.minutes
        val MAX_EXPIRY_MARGIN = 6.minutes

        const val ERROR_SESSION_CHANGED = "Media session changed"
        const val ERROR_INVALID_TARGET = "Invalid media endpoint"
        const val ERROR_UPLOAD_FAILED = "Media upload failed"
        const val ERROR_REQUEST_FAILED = "Media request failed"
    }
}

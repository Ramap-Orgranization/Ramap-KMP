package com.peto.ramap.notification

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

class NotificationDeepLinkParser {
    private val externalUrlValidator = NotificationExternalUrlValidator()

    fun parse(value: String?): NotificationDeepLink? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() && it.length <= MAX_PAYLOAD_LENGTH } ?: return null
        if (raw.startsWith(LEGACY_EVENT_PREFIX)) return parseLegacyEvent(raw)
        val payload = runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull() ?: return null
        val version = payload["version"] as? JsonPrimitive ?: return null
        if (version.isString || version.intOrNull !in 1..PROTOCOL_VERSION) return null
        return when (payload.string("action")) {
            "event_detail" -> {
                if (payload.keys != setOf("version", "action", "event_id")) return null
                payload.string("event_id")?.takeIf(EVENT_ID::matches)?.let { NotificationDeepLink.Event(it) }
            }
            "operating_notices" -> {
                when {
                    payload.keys == setOf("version", "action") -> NotificationDeepLink.OperatingNotices
                    version.intOrNull == 2 && payload.keys == setOf("version", "action", "notice_id") ->
                        payload.string("notice_id")?.takeIf(EVENT_ID::matches)?.let { NotificationDeepLink.OperatingNotice(it) }
                    else -> null
                }
            }
            "external_url" -> {
                if (payload.keys != setOf("version", "action", "url")) return null
                payload.string("url")?.takeIf(externalUrlValidator::isValid)?.let { NotificationDeepLink.ExternalUrl(it) }
            }
            "none" -> if (payload.keys == setOf("version", "action")) NotificationDeepLink.None else null
            else -> null
        }
    }

    private fun parseLegacyEvent(raw: String): NotificationDeepLink? {
        val parameters = raw.removePrefix(LEGACY_EVENT_PREFIX).split('&')
        val ids = parameters.filter { it.startsWith("event_id=") }.map { it.removePrefix("event_id=") }
        return ids.singleOrNull()?.takeIf(EVENT_ID::matches)?.let { NotificationDeepLink.Event(it) }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    companion object {
        const val PROTOCOL_VERSION = 2
        private const val MAX_PAYLOAD_LENGTH = 4096
        private const val LEGACY_EVENT_PREFIX = "ramap://notification/event?"
        private val EVENT_ID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}

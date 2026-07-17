package com.peto.ramap.notification

import co.touchlab.kermit.Logger

class NotificationDeepLinkParser {
    private val logger = Logger.withTag("NotificationDeepLinkParser")

    fun parse(value: String?): NotificationDeepLink? {
        val deepLink = normalizedDeepLink(value) ?: return null
        if (!hasSupportedEventPrefix(deepLink)) return null

        val eventId = extractEventId(deepLink) ?: return null
        if (!EVENT_ID_REGEX.matches(eventId)) {
            logger.d { "딥 링크 파싱 실패: event_id 형식이 올바르지 않음: $eventId" }
            return null
        }

        return NotificationDeepLink.Event(eventId)
    }

    private fun normalizedDeepLink(value: String?): String? {
        val deepLink = value?.trim().orEmpty()
        if (deepLink.isNotEmpty()) return deepLink

        logger.d { "딥 링크 파싱 실패: 값이 비어 있음" }
        return null
    }

    private fun hasSupportedEventPrefix(deepLink: String): Boolean {
        if (deepLink.startsWith(EVENT_PREFIX)) return true

        logger.d { "딥 링크 파싱 실패: 지원하지 않는 경로: $deepLink" }
        return false
    }

    private fun extractEventId(deepLink: String): String? {
        val eventId =
            deepLink
                .substringAfter(QUERY_DELIMITER, missingDelimiterValue = "")
                .split(PARAMETER_DELIMITER)
                .firstNotNullOfOrNull(::eventIdParameterValue)
        if (eventId != null) return eventId

        logger.d { "딥 링크 파싱 실패: event_id 파라미터가 없음" }
        return null
    }

    private fun eventIdParameterValue(parameter: String): String? {
        val parts = parameter.split(KEY_VALUE_DELIMITER, limit = KEY_VALUE_SPLIT_LIMIT)
        return parts
            .takeIf { it.size == KEY_VALUE_SPLIT_LIMIT && it[0] == EVENT_ID_PARAMETER }
            ?.get(PARAMETER_VALUE_INDEX)
    }

    private companion object {
        private const val DEEP_LINK_SCHEME = "ramap"
        private const val EVENT_HOST_PATH = "notification/event"
        private const val QUERY_DELIMITER = '?'
        private const val EVENT_PREFIX = "$DEEP_LINK_SCHEME://$EVENT_HOST_PATH$QUERY_DELIMITER"

        private const val PARAMETER_DELIMITER = '&'
        private const val KEY_VALUE_DELIMITER = '='
        private const val KEY_VALUE_SPLIT_LIMIT = 2
        private const val PARAMETER_VALUE_INDEX = 1

        private const val EVENT_ID_PARAMETER = "event_id"
        val EVENT_ID_REGEX = Regex("[0-9a-fA-F-]{36}")
    }
}

package com.peto.ramap.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationDeepLinkParserTest {
    private val parser = NotificationDeepLinkParser()

    @Test
    fun parsesEventDeepLink() {
        val eventId = "123e4567-e89b-12d3-a456-426614174000"

        assertEquals(
            NotificationDeepLink.Event(eventId),
            parser.parse("ramap://notification/event?event_id=$eventId"),
        )
    }

    @Test
    fun rejectsUnsupportedOrInvalidDeepLink() {
        assertNull(parser.parse(null))
        assertNull(parser.parse("ramap://auth/callback"))
        assertNull(parser.parse("ramap://notification/event?source=notification"))
        assertNull(parser.parse("ramap://notification/event?event_id=invalid"))
    }
}

package com.peto.ramap.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopEventParticipantResponseTest {
    @Test
    fun `이벤트 참여자 응답의 이벤트 ID와 nullable 매장 ID를 역직렬화한다`() {
        val response =
            Json.decodeFromString<ShopEventParticipantResponse>(
                """{"event_id":"event-1","shop_id":null}""",
            )

        assertEquals("event-1", response.eventId)
        assertEquals(null, response.shopId)
    }
}

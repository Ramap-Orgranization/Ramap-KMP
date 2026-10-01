package com.peto.ramap.debug.admin.data.model.request

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminOperatingNoticeRequestTest {
    @Test
    fun `delayed opening list request sends explicit list action`() {
        assertEquals(
            "{\"action\":\"list\"}",
            Json.encodeToString(AdminOperatingNoticeRequest(action = "list")),
        )
    }

    @Test
    fun `delayed opening release request sends explicit release action and id`() {
        assertEquals(
            "{\"action\":\"release\",\"id\":\"notice-id\"}",
            Json.encodeToString(AdminOperatingNoticeRequest(action = "release", id = "notice-id")),
        )
    }
}

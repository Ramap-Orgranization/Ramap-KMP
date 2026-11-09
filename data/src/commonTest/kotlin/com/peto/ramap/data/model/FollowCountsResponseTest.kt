package com.peto.ramap.data.model

import com.peto.ramap.domain.model.follow.FollowList
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowCountsResponseTest {
    @Test
    fun `새 팔로워와 신청의 확인 상태를 각각 전달한다`() {
        val counts =
            Json
                .decodeFromString<FollowCountsResponse>(
                    """{"followers":1,"following":5,"requests":1,"has_unread_followers":true,"has_unread_requests":false}""",
                ).toDomain()

        assertTrue(counts.hasUnreadConnections)
        assertTrue(counts.hasUnread(FollowList.FOLLOWERS))
        assertFalse(counts.hasUnread(FollowList.REQUESTS))
        assertFalse(counts.hasUnread(FollowList.FOLLOWING))
    }

    @Test
    fun `기존 서버 응답은 신규 표시 없이 정상적으로 읽는다`() {
        val counts = Json.decodeFromString<FollowCountsResponse>("""{"followers":1,"following":5,"requests":1}""").toDomain()

        assertFalse(counts.hasUnreadConnections)
    }
}

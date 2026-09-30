package com.peto.ramap.data.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockedUserResponseTest {
    @Test
    fun `차단 시각을 서울 날짜로 변환하고 프로필 이미지를 전달한다`() {
        val response =
            BlockedUserResponse(
                userId = "user1",
                nickname = "라멘팬",
                blockedAt = "2024-02-27T16:00:00Z",
                avatarUrl = "https://example.com/avatar.png",
            )

        val user = response.toDomain()

        assertEquals(LocalDate(2024, 2, 28), user.blockedOn)
        assertEquals("https://example.com/avatar.png", user.profile.avatarUrl)
    }
}

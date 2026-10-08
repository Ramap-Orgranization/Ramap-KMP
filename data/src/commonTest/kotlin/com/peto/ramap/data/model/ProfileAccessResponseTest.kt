package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.ProfileAccess
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProfileAccessResponseTest {
    private val profile = PublicProfileResponse(userId = "author", nickname = "라멘팬")

    @Test
    fun `공개 및 차단 프로필에만 사용자 정보를 매핑한다`() {
        val visible = ProfileAccessResponse("visible", profile).toDomain()
        val blocked = ProfileAccessResponse("blocked", profile).toDomain()

        assertEquals("author", assertIs<ProfileAccess.Visible>(visible).profile.userId)
        assertEquals("author", assertIs<ProfileAccess.Blocked>(blocked).profile.userId)
        assertEquals(ProfileAccess.Private, ProfileAccessResponse("private").toDomain())
        assertEquals(ProfileAccess.Unavailable, ProfileAccessResponse("unavailable").toDomain())
    }

    @Test
    fun `서버 응답이 불완전하면 오류로 처리한다`() {
        assertFailsWith<IllegalArgumentException> { ProfileAccessResponse("visible").toDomain() }
        assertFailsWith<IllegalArgumentException> { ProfileAccessResponse("blocked").toDomain() }
        assertFailsWith<IllegalStateException> { ProfileAccessResponse("unexpected").toDomain() }
    }

    @Test
    fun `프로필 접근 응답의 전체 개수를 목록과 별도로 매핑한다`() {
        val response =
            Json.decodeFromString<ProfileAccessResponse>(
                """{"status":"visible","profile":{"user_id":"author","nickname":"라멘팬"},"review_count":43,"saved_shop_count":67}""",
            )

        val access = assertIs<ProfileAccess.Visible>(response.toDomain())

        assertEquals(43, access.reviewCount)
        assertEquals(67, access.savedShopCount)
    }

    @Test
    fun `전체 개수가 없는 응답과 접근할 수 없는 개수는 영으로 바꾸지 않는다`() {
        val missing = assertIs<ProfileAccess.Visible>(ProfileAccessResponse("visible", profile).toDomain())
        val response =
            Json.decodeFromString<ProfileAccessResponse>(
                """{"status":"visible","profile":{"user_id":"author","nickname":"라멘팬"},"review_count":0,"saved_shop_count":null}""",
            )
        val access = assertIs<ProfileAccess.Visible>(response.toDomain())

        assertNull(missing.reviewCount)
        assertNull(missing.savedShopCount)
        assertEquals(0, access.reviewCount)
        assertNull(access.savedShopCount)
    }
}

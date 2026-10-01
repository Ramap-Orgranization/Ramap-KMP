package com.peto.ramap.data.model

import com.peto.ramap.domain.model.community.ProfileAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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
}

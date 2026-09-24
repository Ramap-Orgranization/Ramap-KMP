package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.profile.ProfileDataSource
import com.peto.ramap.data.model.ProfileResponse
import com.peto.ramap.domain.model.profile.AccountProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultProfileRepositoryTest {
    @Test
    fun returnsSignedPhotoAndCapturedOwner() =
        runTest {
            val source = ProfileDataSourceFake()
            val result = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source).fetchMyProfile())
            assertEquals("owner", source.requestOwner)
            assertEquals(AccountProfile("owner", "라멘", "signed:owner/old.jpg"), result.data)
        }

    @Test
    fun discardsResponseAfterAccountSwitch() =
        runTest {
            val source = ProfileDataSourceFake()
            source.afterFetch = { source.userId = "other" }
            assertIs<RamapResult.Error>(DefaultProfileRepository(source).fetchMyProfile())
        }

    @Test
    fun discardsSignedPhotoAfterAccountSwitch() =
        runTest {
            val source = ProfileDataSourceFake()
            source.afterSignedUrl = { source.userId = "other" }
            assertIs<RamapResult.Error>(DefaultProfileRepository(source).fetchMyProfile())
        }
}

internal class ProfileDataSourceFake : ProfileDataSource {
    var userId: String? = "owner"
    override val sessionUserIds = MutableStateFlow(userId)
    var requestOwner: String? = null
    var afterFetch: () -> Unit = {}
    var afterSignedUrl: () -> Unit = {}

    override fun currentUserId() = userId

    override suspend fun fetchProfile(userId: String): ProfileResponse {
        requestOwner = userId
        afterFetch()
        return ProfileResponse(userId, "라멘", "$userId/old.jpg")
    }

    override suspend fun signedPhotoUrl(
        userId: String,
        path: String,
    ): String {
        afterSignedUrl()
        return "signed:$path"
    }
}

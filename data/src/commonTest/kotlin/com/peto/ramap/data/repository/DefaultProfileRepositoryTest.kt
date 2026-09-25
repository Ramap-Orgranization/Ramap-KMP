package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.profile.ProfileDataSource
import com.peto.ramap.data.model.ProfileResponse
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
    fun rejectsInvalidInputBeforeUploading() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source)
            assertIs<RamapResult.Error>(repository.updateMyProfile("a", photo()))
            assertIs<RamapResult.Error>(repository.updateMyProfile("라멘", photo(), true))
            assertTrue(source.uploaded.isEmpty())
        }

    @Test
    fun retainsUploadedPhotoWhenRpcOutcomeIsUnknown() =
        runTest {
            val source = ProfileDataSourceFake()
            source.updateFailure = IllegalStateException("response lost after commit")
            assertIs<RamapResult.Error>(DefaultProfileRepository(source).updateMyProfile("라멘", photo()))
            assertEquals(1, source.uploaded.size)
            assertTrue(source.deleted.isEmpty())
            assertEquals("owner", source.requestOwner)
        }

    @Test
    fun cancellationPropagatesWithoutDeletingPotentiallyCommittedPhoto() =
        runTest {
            val source = ProfileDataSourceFake()
            source.updateFailure = CancellationException("cancelled")
            assertFailsWith<CancellationException> { DefaultProfileRepository(source).updateMyProfile("라멘", photo()) }
            assertTrue(source.deleted.isEmpty())
        }

    @Test
    fun failedUploadCleansOnlyNewPathAndPreservesCancellation() =
        runTest {
            val source = ProfileDataSourceFake()
            source.uploadFailure = CancellationException("cancelled")
            assertFailsWith<CancellationException> { DefaultProfileRepository(source).updateMyProfile("라멘", photo()) }
            assertEquals(source.uploaded, source.deleted)
            assertTrue(source.deleted.single() != "owner/old.jpg")
        }

    @Test
    fun accountSwitchDuringUploadNeverSubmitsUpdateAsNewUser() =
        runTest {
            val source = ProfileDataSourceFake()
            source.afterUpload = { source.userId = "other" }
            assertIs<RamapResult.Error>(DefaultProfileRepository(source).updateMyProfile("라멘", photo()))
            assertEquals(0, source.updateCount)
            assertTrue(source.deleted.isEmpty())
        }

    @Test
    fun confirmedReplacementDeletesOnlyPreviousPhoto() =
        runTest {
            val source = ProfileDataSourceFake()
            assertIs<RamapResult.Success<*>>(DefaultProfileRepository(source).updateMyProfile("라멘", photo()))
            assertEquals(listOf("owner/old.jpg"), source.deleted)
            assertEquals(1, source.uploaded.size)
        }

    @Test
    fun confirmedRemovalDeletesPreviousPhoto() =
        runTest {
            val source = ProfileDataSourceFake()
            assertIs<RamapResult.Success<*>>(DefaultProfileRepository(source).updateMyProfile("라멘", removePhoto = true))
            assertEquals(listOf("owner/old.jpg"), source.deleted)
        }

    @Test
    fun savesFetchesPreservesAndClearsBio() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source)
            val bio = "🍜".repeat(50)
            val saved = assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("라멘", bio = bio))
            assertEquals(bio, saved.data.bio)
            assertEquals(bio, assertIs<RamapResult.Success<AccountProfile>>(repository.fetchMyProfile()).data.bio)
            assertEquals(bio, assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("라멘")).data.bio)
            assertEquals("", assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("라멘", bio = "")).data.bio)
        }

    @Test
    fun rejectsInvalidBioBeforeNetworkOrPhotoUpload() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source)
            for (bio in listOf("가".repeat(51), "한 줄\n두 줄")) {
                assertIs<RamapResult.Error>(repository.updateMyProfile("라멘", photo(), bio = bio))
            }
            assertEquals(null, source.requestOwner)
            assertEquals(0, source.updateCount)
            assertTrue(source.uploaded.isEmpty())
        }

    @Test
    fun normalizesFetchesPreservesAndRemovesInstagram() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source)
            val saved = repository.updateMyProfile("라멘", instagramUsername = "https://www.instagram.com/Ramap.official/?igsh=123")
            assertEquals("ramap.official", assertIs<RamapResult.Success<AccountProfile>>(saved).data.instagramUsername)
            assertEquals("ramap.official", source.instagramUsername)
            assertEquals("ramap.official", assertIs<RamapResult.Success<AccountProfile>>(repository.fetchMyProfile()).data.instagramUsername)
            assertEquals("ramap.official", assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("라멘")).data.instagramUsername)
            assertEquals("", assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("라멘", instagramUsername = "")).data.instagramUsername)
        }

    @Test
    fun rejectsInvalidInstagramBeforeNetworkOrPhotoUpload() =
        runTest {
            val source = ProfileDataSourceFake()
            val result = DefaultProfileRepository(source).updateMyProfile("라멘", photo(), instagramUsername = "https://evil.test/ramap")
            assertIs<RamapResult.Error>(result)
            assertEquals(null, source.requestOwner)
            assertEquals(0, source.updateCount)
            assertTrue(source.uploaded.isEmpty())
        }

    private fun photo() = ProfileImage(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()), "image/jpeg")

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
    var afterUpload: () -> Unit = {}
    var updateFailure: Throwable? = null
    var uploadFailure: Throwable? = null
    var updateCount = 0
    var bio = ""
    var instagramUsername = ""
    val uploaded = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    var afterSignedUrl: () -> Unit = {}

    override fun currentUserId() = userId

    override suspend fun fetchProfile(userId: String): ProfileResponse {
        requestOwner = userId
        afterFetch()
        return ProfileResponse(userId, "라멘", "$userId/old.jpg", bio, instagramUsername)
    }

    override suspend fun isNicknameAvailable(nickname: String): Boolean = true

    override suspend fun updateProfile(
        userId: String,
        nickname: String,
        avatarPath: String?,
        removePhoto: Boolean,
        bio: String?,
        instagramUsername: String?,
    ): ProfileResponse {
        requestOwner = userId
        updateCount++
        updateFailure?.let { throw it }
        this.bio = bio ?: this.bio
        this.instagramUsername = instagramUsername ?: this.instagramUsername
        return ProfileResponse(userId, nickname, if (removePhoto) null else avatarPath ?: "$userId/old.jpg", this.bio, this.instagramUsername)
    }

    override suspend fun uploadPhoto(
        userId: String,
        path: String,
        image: ProfileImage,
    ) {
        uploaded += path
        afterUpload()
        uploadFailure?.let { throw it }
    }

    override suspend fun deletePhoto(
        userId: String,
        path: String,
    ) {
        deleted += path
    }

    override suspend fun signedPhotoUrl(
        userId: String,
        path: String,
    ): String {
        afterSignedUrl()
        return "signed:$path"
    }
}

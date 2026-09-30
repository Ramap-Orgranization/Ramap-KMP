package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.profile.ProfileDataSource
import com.peto.ramap.data.model.ProfileResponse
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class DefaultProfileRepositoryTest {
    @Test
    fun profileSavePublishesUpdatedProfile() =
        runTest {
            val repository = DefaultProfileRepository(ProfileDataSourceFake())
            val update = async { repository.observeProfileUpdates().first() }
            runCurrent()

            val saved = assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("새닉네임"))

            assertEquals(saved.data, update.await())
        }

    @Test
    fun returnsSignedPhotoAndCapturedOwner() =
        runTest {
            val source = ProfileDataSourceFake()
            val result = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source).fetchMyProfile())
            assertEquals("owner", source.requestOwner)
            assertEquals(AccountProfile("owner", "라멘", "signed:owner/old.jpg"), result.data)
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
    fun savesFetchesPreservesAndClearsBio() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source)
            val bio = "🍜".repeat(30)
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
            for (bio in listOf("가".repeat(31), "한 줄\n두 줄")) {
                assertIs<RamapResult.Error>(repository.updateMyProfile("라멘", photo(), bio = bio))
            }
            assertEquals(null, source.requestOwner)
            assertEquals(0, source.updateCount)
            assertTrue(source.uploaded.isEmpty())
        }

    @Test
    fun uncertainProfileSaveFailureKeepsUploadedPhotoAndOriginalError() =
        runTest {
            val failure = IllegalStateException("save rejected")
            val source = ProfileDataSourceFake().apply { updateFailure = failure }

            val result = assertIs<RamapResult.Error>(DefaultProfileRepository(source).updateMyProfile("라멘", photo()))

            assertSame(failure, assertIs<RamapError.Unknown>(result.error).cause)
            assertTrue(source.deleted.isEmpty())
            assertEquals(1, source.uploaded.size)
        }

    @Test
    fun confirmedRateLimitDeletesOnlyTheNewUploadAndPreservesOriginalError() =
        runTest {
            val failure = IllegalStateException("HTTP 429")
            val source = ProfileDataSourceFake()
            val path = "owner/new.jpg"
            source.uploaded += path
            val repository = DefaultProfileRepository(source)

            val thrown =
                assertFailsWith<IllegalStateException> {
                    repository.rethrowProfileUpdateFailure("owner", path, 429, failure)
                }

            assertSame(failure, thrown)
            assertEquals(listOf(path), source.deleted)
        }

    @Test
    fun cleanupFailureDoesNotReplaceConfirmedSaveRejection() =
        runTest {
            val failure = IllegalStateException("HTTP 429")
            val source = ProfileDataSourceFake().apply { deleteFailure = IllegalStateException("cleanup failed") }
            val repository = DefaultProfileRepository(source)

            val thrown =
                assertFailsWith<IllegalStateException> {
                    repository.rethrowProfileUpdateFailure("owner", "owner/new.jpg", 429, failure)
                }

            assertSame(failure, thrown)
            assertEquals(listOf("owner/new.jpg"), source.deleted)
        }

    @Test
    fun timeoutAndServerErrorsDoNotDeleteAnUploadedPhoto() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source)
            val failure = IllegalStateException("response unknown")

            for (status in listOf(408, 500, null)) {
                val thrown =
                    assertFailsWith<IllegalStateException> {
                        repository.rethrowProfileUpdateFailure("owner", "owner/new.jpg", status, failure)
                    }
                assertSame(failure, thrown)
            }

            assertTrue(source.deleted.isEmpty())
        }

    @Test
    fun profileSaveCancellationDoesNotDeleteUploadedPhotoAndStillPropagates() =
        runTest {
            val source = ProfileDataSourceFake().apply { updateFailure = CancellationException("cancelled") }

            assertFailsWith<CancellationException> {
                DefaultProfileRepository(source).updateMyProfile("라멘", photo())
            }

            assertEquals(1, source.uploaded.size)
            assertTrue(source.deleted.isEmpty())
        }

    @Test
    fun visibilitySaveSucceedsWhenAvatarSigningFails() =
        runTest {
            val source = ProfileDataSourceFake()
            source.afterSignedUrl = { error("image unavailable") }
            val saved = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source).updateProfileVisibility(true))
            assertTrue(saved.data.isPublic)
            assertEquals(null, saved.data.avatarUrl)
        }

    @Test
    fun visibilitySavePropagatesAvatarSigningCancellation() =
        runTest {
            val source = ProfileDataSourceFake()
            source.afterSignedUrl = { throw CancellationException() }

            assertFailsWith<CancellationException> {
                DefaultProfileRepository(source).updateProfileVisibility(true)
            }
        }

    @Test
    fun profileSaveSucceedsWhenAvatarSigningFailsAfterTheRpcCommit() =
        runTest {
            val source = ProfileDataSourceFake().apply { afterSignedUrl = { error("image unavailable") } }

            val saved = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source).updateMyProfile("라멘"))

            assertEquals("라멘", saved.data.nickname)
            assertEquals(null, saved.data.avatarUrl)
        }

    @Test
    fun refreshesSignedPhotoUrlBeforeItsThirtyMinuteExpiry() =
        runTest {
            var currentTime = Instant.fromEpochSeconds(0)
            val source = ProfileDataSourceFake()
            var signedCount = 0
            source.afterSignedUrl = { signedCount++ }
            val repository = DefaultProfileRepository(source) { currentTime }

            repository.fetchMyProfile()
            currentTime += 28.minutes
            repository.fetchMyProfile()
            currentTime += 1.minutes
            repository.fetchMyProfile()

            assertEquals(2, signedCount)
        }

    @Test
    fun successfulPhotoReplacementDeletesThePreviousPhoto() =
        runTest {
            val source = ProfileDataSourceFake()

            assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source).updateMyProfile("라멘", photo()))

            assertEquals(listOf("owner/old.jpg"), source.deleted)
        }

    @Test
    fun successfulPhotoRemovalDeletesThePreviousPhoto() =
        runTest {
            val source = ProfileDataSourceFake()

            assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source).updateMyProfile("라멘", removePhoto = true))

            assertEquals(listOf("owner/old.jpg"), source.deleted)
        }

    @Test
    fun oldPhotoCleanupFailureDoesNotTurnASuccessfulSaveIntoAFailure() =
        runTest {
            val source = ProfileDataSourceFake().apply { deleteFailure = IllegalStateException("cleanup failed") }

            val saved = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source).updateMyProfile("라멘", removePhoto = true))

            assertEquals("라멘", saved.data.nickname)
            assertEquals(listOf("owner/old.jpg"), source.deleted)
        }

    private fun photo() = ProfileImage(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()), "image/jpeg")

    @Test
    fun updatesProfileVisibilityWithTheCurrentAccount() =
        runTest {
            val source = ProfileDataSourceFake()

            val result =
                assertIs<RamapResult.Success<AccountProfile>>(
                    DefaultProfileRepository(source).updateProfileVisibility(true),
                )

            assertEquals("owner", source.visibilityOwner)
            assertTrue(result.data.isPublic)
        }

    @Test
    fun reusesSignedPhotoUrlWhenAvatarPathIsUnchanged() =
        runTest {
            val source = ProfileDataSourceFake()
            var signedCount = 0
            source.afterSignedUrl = { signedCount++ }
            val repository = DefaultProfileRepository(source)

            val first = assertIs<RamapResult.Success<AccountProfile>>(repository.fetchMyProfile())
            val second = assertIs<RamapResult.Success<AccountProfile>>(repository.fetchMyProfile())

            assertEquals(1, signedCount)
            assertEquals(first.data.avatarUrl, second.data.avatarUrl)
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
    var deleteFailure: Throwable? = null
    var updateCount = 0
    var bio = ""
    val uploaded = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    var afterSignedUrl: () -> Unit = {}
    var visibilityOwner: String? = null

    override fun currentUserId() = userId

    override suspend fun fetchProfile(userId: String): ProfileResponse {
        requestOwner = userId
        afterFetch()
        return ProfileResponse(userId, "라멘", "$userId/old.jpg", bio)
    }

    override suspend fun isNicknameAvailable(nickname: String): Boolean = true

    override suspend fun updateProfile(
        userId: String,
        nickname: String,
        avatarPath: String?,
        removePhoto: Boolean,
        bio: String?,
    ): ProfileResponse {
        requestOwner = userId
        updateCount++
        updateFailure?.let { throw it }
        this.bio = bio ?: this.bio
        return ProfileResponse(userId, nickname, if (removePhoto) null else avatarPath ?: "$userId/old.jpg", this.bio)
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
        deleteFailure?.let { throw it }
    }

    override suspend fun signedPhotoUrl(
        userId: String,
        path: String,
    ): String {
        afterSignedUrl()
        return "signed:$path"
    }

    override suspend fun updateProfileVisibility(isPublic: Boolean): ProfileResponse {
        val owner = requireNotNull(userId)
        visibilityOwner = owner
        return ProfileResponse(owner, "라멘", "$owner/old.jpg", bio, isPublic)
    }
}

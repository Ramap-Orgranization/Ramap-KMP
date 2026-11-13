package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.profile.ProfileDataSource
import com.peto.ramap.data.model.ProfileResponse
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.data.testReviewChanges
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.domain.model.profile.ProfileVisibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class DefaultProfileRepositoryTest {
    @Test
    fun `금칙어 닉네임은 중복 검사와 사진 업로드 전에 거부한다`() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())
            assertIs<RamapResult.Error>(repository.isNicknameAvailable("씨_발"))
            assertIs<RamapResult.Error>(repository.updateMyProfile("씨_발", photo()))
            assertEquals(0, source.nicknameCheckCount)
            assertEquals(0, source.updateCount)
            assertTrue(source.uploaded.isEmpty())
        }

    @Test
    fun `프로필 저장은 갱신된 프로필을 반환한다`() =
        runTest {
            val repository = DefaultProfileRepository(ProfileDataSourceFake(), changes = testReviewChanges())

            val saved = assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("새닉네임"))

            assertEquals("새닉네임", saved.data.nickname)
        }

    @Test
    fun `서명된 사진과 요청 시점의 소유자를 반환한다`() =
        runTest {
            val source = ProfileDataSourceFake()
            val result = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source, changes = testReviewChanges()).fetchMyProfile())
            assertEquals("owner", source.requestOwner)
            assertEquals(AccountProfile("owner", "라멘", "signed:owner/old.jpg"), result.data)
        }

    @Test
    fun `잘못된 입력은 사진 업로드 전에 거부한다`() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())
            assertIs<RamapResult.Error>(repository.updateMyProfile("a", photo()))
            assertIs<RamapResult.Error>(repository.updateMyProfile("라멘", photo(), true))
            assertTrue(source.uploaded.isEmpty())
        }

    @Test
    fun `소개글을 저장하고 조회하며 유지하거나 비울 수 있다`() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())
            val bio = "🍜".repeat(30)
            val saved = assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("라멘", bio = bio))
            assertEquals(bio, saved.data.bio)
            assertEquals(bio, assertIs<RamapResult.Success<AccountProfile>>(repository.fetchMyProfile()).data.bio)
            assertEquals(bio, assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("라멘")).data.bio)
            assertEquals("", assertIs<RamapResult.Success<AccountProfile>>(repository.updateMyProfile("라멘", bio = "")).data.bio)
        }

    @Test
    fun `잘못된 소개글은 네트워크 요청과 사진 업로드 전에 거부한다`() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())
            for (bio in listOf("가".repeat(31), "한 줄\n두 줄")) {
                assertIs<RamapResult.Error>(repository.updateMyProfile("라멘", photo(), bio = bio))
            }
            assertEquals(null, source.requestOwner)
            assertEquals(0, source.updateCount)
            assertTrue(source.uploaded.isEmpty())
        }

    @Test
    fun `저장 여부가 불확실한 실패는 업로드한 사진과 원래 오류를 유지한다`() =
        runTest {
            val failure = IllegalStateException("save rejected")
            val source = ProfileDataSourceFake().apply { updateFailure = failure }

            val result = assertIs<RamapResult.Error>(DefaultProfileRepository(source, changes = testReviewChanges()).updateMyProfile("라멘", photo()))

            assertSame(failure, assertIs<RamapError.Unknown>(result.error).cause)
            assertTrue(source.deleted.isEmpty())
            assertEquals(1, source.uploaded.size)
        }

    @Test
    fun `요청 제한이 확인되면 새로 업로드한 사진만 삭제하고 원래 오류를 유지한다`() =
        runTest {
            val failure = IllegalStateException("HTTP 429")
            val source = ProfileDataSourceFake()
            val path = "owner/new.jpg"
            source.uploaded += path
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())

            val thrown =
                assertFailsWith<IllegalStateException> {
                    repository.rethrowProfileUpdateFailure("owner", path, 429, failure)
                }

            assertSame(failure, thrown)
            assertEquals(listOf(path), source.deleted)
        }

    @Test
    fun `사진 정리 실패는 확인된 저장 거부 오류를 바꾸지 않는다`() =
        runTest {
            val failure = IllegalStateException("HTTP 429")
            val source = ProfileDataSourceFake().apply { deleteFailure = IllegalStateException("cleanup failed") }
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())

            val thrown =
                assertFailsWith<IllegalStateException> {
                    repository.rethrowProfileUpdateFailure("owner", "owner/new.jpg", 429, failure)
                }

            assertSame(failure, thrown)
            assertEquals(listOf("owner/new.jpg"), source.deleted)
        }

    @Test
    fun `시간 초과와 서버 오류는 업로드한 사진을 삭제하지 않는다`() =
        runTest {
            val source = ProfileDataSourceFake()
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())
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
    fun `프로필 저장 취소는 업로드한 사진을 삭제하지 않고 전파한다`() =
        runTest {
            val source = ProfileDataSourceFake().apply { updateFailure = CancellationException("cancelled") }

            assertFailsWith<CancellationException> {
                DefaultProfileRepository(source, changes = testReviewChanges()).updateMyProfile("라멘", photo())
            }

            assertEquals(1, source.uploaded.size)
            assertTrue(source.deleted.isEmpty())
        }

    @Test
    fun `아바타 서명이 실패해도 공개 범위 저장은 성공한다`() =
        runTest {
            val source = ProfileDataSourceFake()
            source.afterSignedUrl = { error("image unavailable") }
            val saved = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source, changes = testReviewChanges()).updateProfileVisibility(true))
            assertTrue(saved.data.isPublic)
            assertEquals(null, saved.data.avatarUrl)
        }

    @Test
    fun `공개 범위 저장은 아바타 서명 취소를 전파한다`() =
        runTest {
            val source = ProfileDataSourceFake()
            source.afterSignedUrl = { throw CancellationException("Cancelled") }

            assertFailsWith<CancellationException> {
                DefaultProfileRepository(source, changes = testReviewChanges()).updateProfileVisibility(true)
            }
        }

    @Test
    fun `저장 응답 이후 아바타 서명이 실패해도 프로필 저장은 성공한다`() =
        runTest {
            val source = ProfileDataSourceFake().apply { afterSignedUrl = { error("image unavailable") } }

            val saved = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source, changes = testReviewChanges()).updateMyProfile("라멘"))

            assertEquals("라멘", saved.data.nickname)
            assertEquals(null, saved.data.avatarUrl)
        }

    @Test
    fun `반환된 사진 URL이 만료되기 전에 서명을 갱신한다`() =
        runTest {
            var currentTime = Instant.fromEpochSeconds(0)
            val source = ProfileDataSourceFake().apply { signedExpiry = { currentTime + 5.minutes } }
            var signedCount = 0
            source.afterSignedUrl = { signedCount++ }
            val repository = DefaultProfileRepository(source, changes = testReviewChanges()) { currentTime }

            repository.fetchMyProfile()
            currentTime += 3.minutes
            repository.fetchMyProfile()
            currentTime += 1.minutes
            repository.fetchMyProfile()

            assertEquals(2, signedCount)
        }

    @Test
    fun `사진 교체가 성공하면 이전 사진을 삭제한다`() =
        runTest {
            val source = ProfileDataSourceFake()

            assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source, changes = testReviewChanges()).updateMyProfile("라멘", photo()))

            assertEquals(listOf("owner/old.jpg"), source.deleted)
        }

    @Test
    fun `사진 제거가 성공하면 이전 사진을 삭제한다`() =
        runTest {
            val source = ProfileDataSourceFake()

            assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source, changes = testReviewChanges()).updateMyProfile("라멘", removePhoto = true))

            assertEquals(listOf("owner/old.jpg"), source.deleted)
        }

    @Test
    fun `이전 사진 정리 실패는 성공한 저장을 실패로 바꾸지 않는다`() =
        runTest {
            val source = ProfileDataSourceFake().apply { deleteFailure = IllegalStateException("cleanup failed") }

            val saved = assertIs<RamapResult.Success<AccountProfile>>(DefaultProfileRepository(source, changes = testReviewChanges()).updateMyProfile("라멘", removePhoto = true))

            assertEquals("라멘", saved.data.nickname)
            assertEquals(listOf("owner/old.jpg"), source.deleted)
        }

    private fun photo() = ProfileImage(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()), "image/jpeg")

    @Test
    fun `현재 계정을 공개로 바꿀 때 팔로워 열람 설정을 유지한다`() =
        runTest {
            val source =
                ProfileDataSourceFake().apply {
                    visibility = ProfileVisibility(isPublic = false, followersCanReadReviews = false)
                }

            val result =
                assertIs<RamapResult.Success<AccountProfile>>(
                    DefaultProfileRepository(source, changes = testReviewChanges()).updateProfileVisibility(true),
                )

            assertEquals("owner", source.visibilityOwner)
            assertTrue(result.data.isPublic)
            assertEquals(ProfileVisibility(isPublic = true, followersCanReadReviews = false), source.visibility)
        }

    @Test
    fun `기존 공개 범위를 읽는 동안 계정이 바뀌면 공개 설정을 저장하지 않는다`() =
        runTest {
            val source = ProfileDataSourceFake().apply { afterFetch = { userId = "other" } }
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())

            assertIs<RamapResult.Error>(repository.updateProfileVisibility(true))
            assertEquals(null, source.visibilityOwner)
        }

    @Test
    fun `아바타 경로가 같으면 서명된 사진 URL을 재사용한다`() =
        runTest {
            val source = ProfileDataSourceFake()
            var signedCount = 0
            source.afterSignedUrl = { signedCount++ }
            val repository = DefaultProfileRepository(source, changes = testReviewChanges())

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
    var nicknameCheckCount = 0
    var bio = ""
    val uploaded = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    var afterSignedUrl: () -> Unit = {}
    var signedExpiry: () -> Instant = { Clock.System.now() + 5.minutes }
    var visibilityOwner: String? = null
    var visibility = ProfileVisibility(isPublic = false)

    override fun currentUserId() = userId

    override suspend fun fetchProfile(userId: String): ProfileResponse {
        requestOwner = userId
        afterFetch()
        return profileResponse(userId)
    }

    override suspend fun isNicknameAvailable(nickname: String): Boolean {
        nicknameCheckCount++
        return true
    }

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
    ): SignedImageUrl {
        afterSignedUrl()
        return SignedImageUrl(url = "signed:$path", expiresAt = signedExpiry())
    }

    override suspend fun updateProfileVisibility(visibility: ProfileVisibility): ProfileResponse {
        val owner = requireNotNull(userId)
        visibilityOwner = owner
        this.visibility = visibility
        return profileResponse(owner)
    }

    private fun profileResponse(owner: String): ProfileResponse =
        ProfileResponse(
            userId = owner,
            nickname = "라멘",
            avatarPath = "$owner/old.jpg",
            bio = bio,
            isPublic = visibility.isPublic,
            followersCanReadReviews = visibility.followersCanReadReviews,
            followersCanReadSavedShops = visibility.followersCanReadSavedShops,
        )
}

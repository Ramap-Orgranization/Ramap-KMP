package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.image.ExpiredImageUrlParser
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.domain.model.review.change.ReviewChangeJournal
import com.peto.ramap.domain.store.ReviewChangeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class DefaultImageAccessRepositoryTest {
    private val sessions = MutableStateFlow<String?>("viewer")
    private val journal = MutableStateFlow(ReviewChangeJournal(userId = "viewer"))
    private val changes =
        object : ReviewChangeStore {
            override val journal = this@DefaultImageAccessRepositoryTest.journal
        }
    private var beforeResponse: () -> Unit = {}
    private var allowed = true
    private val requests = mutableListOf<SignedImageRequest>()
    private val remote =
        object : ImageUrlDataSource {
            override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
                requests += images
                beforeResponse()
                return images.associateWith { if (allowed) SignedImageUrl("$URL&renewed=true", Clock.System.now() + 5.minutes) else null }
            }
        }
    private val repository =
        DefaultImageAccessRepository(
            ExpiredImageUrlParser("https://fixture.invalid/storage/v1/object/sign", ACCOUNT, "private"),
            remote,
            changes,
            sessions,
            { sessions.value },
        )

    @Test
    fun `서명 토큰이 바뀌어도 파일과 열람 범위가 같으면 메모리 키를 유지한다`() =
        runTest {
            val first = repository.authorize(URL)!!
            val second = repository.authorize(URL.replace("signature", "other-signature"))!!
            assertEquals(first.memoryKey, second.memoryKey)
            assertEquals(listOf(IMAGE, IMAGE), requests)
            journal.value = journal.value.copy(imageRevision = 2)
            assertNotEquals(first.memoryKey, repository.authorize(URL)!!.memoryKey)
        }

    @Test
    fun `서버에서 접근을 거절하면 원래 서명 주소도 재사용하지 않는다`() =
        runTest {
            allowed = false
            assertNull(repository.authorize(URL))
        }

    @Test
    fun `권한 조회 도중 계정이 바뀌면 결과를 버린다`() =
        runTest {
            beforeResponse = { sessions.value = "another" }
            assertNull(repository.authorize(URL))
        }

    @Test
    fun `현재 저장소의 권한 사진만 분류하고 잘못된 주소는 허용하지 않는다`() =
        runTest {
            assertTrue(repository.isProtected(URL))
            assertFalse(repository.isProtected("https://images.example.com/public.jpg"))
            assertFalse(repository.isProtected(URL.replace(ACCOUNT, "b".repeat(32))))
            assertNull(repository.authorize(URL.replace("X-Amz-Algorithm=AWS4-HMAC-SHA256", "invalid=true")))
            assertEquals(emptyList(), requests)
        }

    private companion object {
        const val ACCOUNT = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val PATH = "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002.jpg"
        val IMAGE = SignedImageRequest("profile-avatars", PATH)
        const val URL = "https://$ACCOUNT.r2.cloudflarestorage.com/private/profile-avatars/$PATH?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20990101T000000Z&X-Amz-Expires=300&X-Amz-Signature=signature"
    }
}

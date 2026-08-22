package com.peto.ramap

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.appnotice.AppNotice
import com.peto.ramap.domain.repository.AppNoticeRepository
import com.peto.ramap.platform.storage.AppNoticeStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppNoticeGateTest {
    @Test
    fun `활성 공지가 있고 다시 보지 않음을 선택하지 않았으면 공지를 표시한다`() =
        runTest {
            val notice = AppNotice(id = "notice-1", title = "제목", message = "본문")

            val result = fetchVisibleAppNotice(FakeAppNoticeRepository(notice), FakeAppNoticeStorage(), "android")

            assertEquals(notice, result)
        }

    @Test
    fun `공지 조회 예외가 발생하면 공지 없이 진행한다`() =
        runTest {
            val result = fetchVisibleAppNotice(ThrowingAppNoticeRepository(), FakeAppNoticeStorage(), "android")

            assertNull(result)
        }

    @Test
    fun `같은 공지 ID만 다시 보지 않음으로 숨긴다`() =
        runTest {
            val notice = AppNotice(id = "notice-1", title = "제목", message = "본문")

            assertTrue(isAppNoticeHidden(notice, hiddenNoticeId = "notice-1"))
            assertFalse(isAppNoticeHidden(notice, hiddenNoticeId = "notice-2"))
            assertNull(
                fetchVisibleAppNotice(
                    repository = FakeAppNoticeRepository(notice),
                    storage = FakeAppNoticeStorage(hiddenNoticeId = notice.id),
                    platform = "android",
                ),
            )
        }

    private class FakeAppNoticeRepository(
        private val appNotice: AppNotice?,
    ) : AppNoticeRepository {
        override suspend fun fetchActiveAppNotice(platform: String): RamapResult<AppNotice?> = RamapResult.Success(appNotice)
    }

    private class ThrowingAppNoticeRepository : AppNoticeRepository {
        override suspend fun fetchActiveAppNotice(platform: String): RamapResult<AppNotice?> = throw IllegalStateException()
    }

    private class FakeAppNoticeStorage(
        private val hiddenNoticeId: String? = null,
    ) : AppNoticeStorage {
        override suspend fun fetchHiddenNoticeId(): String? = hiddenNoticeId

        override suspend fun hideNotice(noticeId: String) = Unit
    }
}

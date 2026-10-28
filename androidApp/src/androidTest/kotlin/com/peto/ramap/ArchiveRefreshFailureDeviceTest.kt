package com.peto.ramap

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.peto.ramap.domain.store.ReviewChangeStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(SupabaseInternal::class)
class ArchiveRefreshFailureDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `갱신_실패와_복구_동안_아카이브와_기존_계정을_유지한다`() {
        val koin = GlobalContext.get()
        val auth = koin.get<SupabaseClient>().auth
        compose.waitUntil(timeoutMillis = 30_000) { auth.sessionStatus.value is SessionStatus.Authenticated }
        val originalStatus = auth.sessionStatus.value
        assertTrue(originalStatus is SessionStatus.Authenticated, "로그인된 실기기에서 실행해야 합니다")
        val changes = koin.get<ReviewChangeStore>()
        val originalUserId = assertNotNull(changes.journal.value.userId)
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithContentDescription("다녀온 매장").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("다녀온 매장").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodes(hasContentDescription("지도") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        try {
            compose.runOnIdle {
                auth.setSessionStatus(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IllegalStateException("E2E offline refresh"))))
            }
            compose.waitForIdle()
            compose.onNodeWithContentDescription("다녀온 매장").assertDoesNotExist()
            compose.onNode(hasContentDescription("지도") and hasClickAction()).assertExists()
            assertEquals(originalUserId, changes.journal.value.userId, "갱신 실패가 이전 계정의 기록을 지우면 안 됩니다")
        } finally {
            compose.runOnIdle { auth.setSessionStatus(originalStatus) }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("다녀온 매장").assertDoesNotExist()
        assertEquals(originalUserId, changes.journal.value.userId)
    }
}

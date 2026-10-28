package com.peto.ramap.ui.main.map.archive

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.archive.component.ArchiveReviewTimelineItem
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapUiState
import com.peto.ramap.ui.main.map.archive.model.rememberArchiveMarkerImages
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ArchiveMapGuidanceTest {
    @BeforeTest
    fun registerTestActivity() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
    }

    @Test
    fun `빈 방문 기록의 안내와 매장 찾기는 네이티브 지도 준비를 기다리지 않는다`() =
        runComposeUiTest {
            var findCount = 0
            setContent {
                withBackDispatcher {
                    ArchiveMapContent(
                        state = ArchiveMapUiState(hasLoaded = true),
                        isBackEnabled = true,
                        onReturnToMap = {},
                        onFindVisitedShop = { findCount++ },
                        onIntent = {},
                        onLocationPermissionBlocked = {},
                        onCurrentLocationTimeout = {},
                    )
                }
            }

            onNodeWithText("다녀온 라멘을\n지도에 남겨보세요").assertExists()
            onNodeWithText("내가 남긴 리뷰와 사진을 불러오는 중이에요.").assertDoesNotExist()
            onNodeWithText("다녀온 매장 찾기").performClick()
            runOnIdle { assertEquals(1, findCount) }
        }

    @Test
    fun `조회 오류는 첫 기록 안내로 표시하지 않는다`() =
        runComposeUiTest {
            setContent {
                withBackDispatcher {
                    ArchiveMapContent(
                        state = ArchiveMapUiState(hasError = true),
                        isBackEnabled = true,
                        onReturnToMap = {},
                        onFindVisitedShop = {},
                        onIntent = {},
                        onLocationPermissionBlocked = {},
                        onCurrentLocationTimeout = {},
                    )
                }
            }

            onNodeWithText("다녀온 매장을 불러오지 못했어요.").assertExists()
            onNodeWithText("다녀온 매장 찾기").assertDoesNotExist()
        }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `대표 사진 서명 실패 시 대기 중 숨겨졌던 매장에 기본 마커가 표시된다`() =
        runComposeUiTest {
            val shops = RamenShops(listOf(ramenShopFixture(id = "shop")))
            val photos = mapOf("shop" to MyReviewPhoto(null, "image"))
            var failures by mutableStateOf(emptySet<String>())
            var visibleShops = emptySet<String>()
            setContent {
                RamapTheme {
                    val markers =
                        rememberArchiveMarkerImages(
                            shops = shops,
                            photos = photos,
                            failedPhotoReferences = failures,
                            onPhotoDownloadFailed = { _, _ -> },
                        )
                    SideEffect { visibleShops = markers.keys }
                }
            }

            runOnIdle { assertEquals(emptySet(), visibleShops) }
            runOnIdle { failures = setOf("image") }
            runOnIdle { assertEquals(setOf("shop"), visibleShops) }
        }

    @Test
    fun `방문 기록에는 저장된 방문 차수와 좋아요 영 개도 표시하고 좋아요를 누를 수 없다`() =
        runComposeUiTest {
            setContent {
                RamapTheme {
                    ArchiveReviewTimelineItem(
                        review = Review("review", "shop", "사진 없는 방문 기록", "2026-10-27T00:00:00Z", visitNumber = 5, likeCount = 0),
                        isLatest = true,
                        isLast = true,
                        onEdit = {},
                        onDelete = {},
                        actionsEnabled = true,
                        onPhotoDownloadFailed = {},
                        onPhotoDownloadSucceeded = {},
                        photoRetryKeys = emptyMap(),
                    )
                }
            }

            onNodeWithText("2026.10.27 · 5번째 방문").assertExists()
            onNodeWithContentDescription("좋아요 0").assert(hasClickAction().not())
        }

    @Composable
    private fun withBackDispatcher(content: @Composable () -> Unit) {
        val dispatcher = remember { NavigationEventDispatcher() }
        DisposableEffect(dispatcher) { onDispose { dispatcher.dispose() } }
        val owner =
            remember(dispatcher) {
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = dispatcher
                }
            }
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
            RamapTheme { content() }
        }
    }
}

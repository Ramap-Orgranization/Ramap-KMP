package com.peto.ramap

import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ArchiveRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReviewRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.lifecycle.CompositionViewModelStoreOwner
import com.peto.ramap.ui.main.map.archive.ArchiveMapRoute
import com.peto.ramap.ui.main.map.archive.ArchiveMapViewModel
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapIntent
import com.peto.ramap.ui.main.map.archive.model.CameraPosition
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class ArchiveOverlappingMarkersDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun `클러스터가_풀린_줌에서도_같은_위치의_두_매장을_각각_선택한다`() {
        val location = Location(37.551, 126.921)
        val first = ramenShopFixture(id = "overlap-first").copy(name = "겹친 테스트 매장 1", location = location)
        val second = ramenShopFixture(id = "overlap-second").copy(name = "겹친 테스트 매장 2", location = location)
        val shops = RamenShops(listOf(first, second))
        val rows =
            shops.values.map { shop ->
                MyReview(
                    review = Review("review-${shop.id}", shop.id, "기기 안에서만 사용하는 테스트 기록입니다", "2026-10-27T15:12:00Z", isPublic = false),
                    shopName = shop.name,
                    shop = shop,
                )
            }
        val repository =
            object : ArchiveRepository {
                override suspend fun fetchArchiveShops() = RamapResult.Success(ArchiveShops(rows, shops))

                override suspend fun fetchShops(shopIds: Set<String>) = RamapResult.Success(shops.filterByShopIds(shopIds))

                override suspend fun resolvePhotos(references: Set<String>) = RamapResult.Success(emptyMap<String, MyReviewPhoto>())
            }
        val storeOwner = CompositionViewModelStoreOwner()
        val dispatcher = NavigationEventDispatcher()
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        lateinit var viewModel: ArchiveMapViewModel
        compose.runOnIdle {
            val factory =
                viewModelFactory {
                    initializer { ArchiveMapViewModel(repository, FakeReviewChangeStore(), ReviewChangeReducer(), FakeShopReviewRepository()) }
                }
            viewModel = ViewModelProvider.create(storeOwner, factory)[ArchiveMapViewModel::class]
            viewModel.dispatch(ArchiveMapIntent.Enter)
        }
        try {
            compose.waitUntil { viewModel.uiState.value.hasLoaded }
            compose.runOnIdle {
                assertEquals(2, viewModel.uiState.value.shops.size)
                viewModel.dispatch(ArchiveMapIntent.FocusConsumed)
                viewModel.dispatch(ArchiveMapIntent.CameraChanged(CameraPosition(location, 18.0)))
            }
            compose.waitUntil { !viewModel.uiState.value.shouldFocusVisits }
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                        RamapTheme {
                            ArchiveMapRoute(
                                isBackEnabled = true,
                                onReturnToMap = {},
                                onFindVisitedShop = {},
                                viewModel = viewModel,
                                toastManager = ToastManager(),
                                appSettingsOpener =
                                    object : AppSettingsOpener {
                                        override fun open() = Unit
                                    },
                            )
                        }
                    }
                }
            }
            var map: NaverMap? = null
            lateinit var mapView: MapView
            compose.runOnIdle {
                mapView = requireNotNull(findMapView(compose.activity.window.decorView))
                mapView.getMapAsync { map = it }
            }
            compose.waitUntil(timeoutMillis = 30_000) {
                var isReady = false
                compose.runOnIdle {
                    isReady = map?.let { it.isFullyRendered && it.isRenderingStable && it.cameraPosition.zoom > 17.0 } == true
                }
                isReady
            }
            compose.waitForIdle()
            val markerPoint = markerPoint(requireNotNull(map), mapView, location)
            compose.onRoot().performTouchInput { click(markerPoint) }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(first.name).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(first.name).assertIsDisplayed()
            compose.onNodeWithText(second.name).assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(second.id, viewModel.uiState.value.selectedShopId) }

            compose.runOnIdle { viewModel.dispatch(ArchiveMapIntent.SelectShop(null)) }
            compose.waitForIdle()
            val nextMarkerPoint = markerPoint(requireNotNull(map), mapView, location)
            compose.onRoot().performTouchInput { click(nextMarkerPoint) }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(first.name).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(first.name).assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(first.id, viewModel.uiState.value.selectedShopId) }
        } finally {
            compose.runOnIdle { storeOwner.viewModelStore.clear() }
            dispatcher.dispose()
        }
    }

    private fun markerPoint(
        map: NaverMap,
        mapView: MapView,
        location: Location,
    ): Offset {
        val rootOffset =
            compose
                .onRoot()
                .fetchSemanticsNode()
                .boundsInWindow.topLeft
        var point = Offset.Zero
        compose.runOnIdle {
            val mapOffset = IntArray(2)
            mapView.getLocationInWindow(mapOffset)
            val projected = map.projection.toScreenLocation(LatLng(location.lat, location.lng))
            point = Offset(projected.x + mapOffset[0], projected.y + mapOffset[1] - 20 * mapView.resources.displayMetrics.density) - rootOffset
        }
        return point
    }

    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view !is ViewGroup) return null
        for (index in 0 until view.childCount) {
            findMapView(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}

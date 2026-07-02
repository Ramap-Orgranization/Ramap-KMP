package com.peto.ramap.domain.model

import com.peto.ramap.core.config.MarkerClusterConfig
import com.peto.ramap.fixture.BOUNDS_FIXTURE
import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkerClusterTest {
    private val markerCluster = MarkerCluster()

    @Test
    fun `가게가 없으면 빈 마커 목록을 반환한다`() {
        // given
        val shops = RamenShops(emptyMap())

        // when
        val result =
            markerCluster.clustering(
                shops = shops,
                bounds = BOUNDS_FIXTURE,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `bounds가 없으면 모든 가게를 단일 마커로 반환한다`() {
        // given
        val shop = ramenShopFixture(id = "1")
        val shops = ramenShopsOf(shop)

        // when
        val result =
            markerCluster.clustering(
                shops = shops,
                bounds = null,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        val marker = assertIs<Marker.SingleMarker>(result.single())
        assertEquals(shop.id, marker.id)
        assertEquals(shop, marker.shop)
    }

    @Test
    fun `viewport 크기가 유효하지 않으면 모든 가게를 단일 마커로 반환한다`() {
        // given
        val shop = ramenShopFixture(id = "1")
        val shops = ramenShopsOf(shop)

        // when
        val result =
            markerCluster.clustering(
                shops = shops,
                bounds = BOUNDS_FIXTURE,
                viewportWidth = 0,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        val marker = assertIs<Marker.SingleMarker>(result.single())
        assertEquals(shop.id, marker.id)
    }

    @Test
    fun `같은 cell의 가게들은 클러스터 마커로 반환한다`() {
        // given
        val firstShop = shopAt(id = "1", lat = 37.501, lng = 126.901)
        val secondShop = shopAt(id = "2", lat = 37.502, lng = 126.902)
        val thirdShop = shopAt(id = "3", lat = 37.503, lng = 126.903)
        val shops = ramenShopsOf(firstShop, secondShop, thirdShop)

        // when
        val result =
            markerCluster.clustering(
                shops = shops,
                bounds = BOUNDS_FIXTURE,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        val marker = assertIs<Marker.ClusterMaker>(result.single())
        assertEquals(MarkerClusterConfig.MIN_SHOP_COUNT, marker.count)
        assertEquals(listOf(firstShop, secondShop, thirdShop), marker.shops)
    }

    @Test
    fun `서로 다른 cell의 가게들은 단일 마커로 반환한다`() {
        // given
        val firstShop = shopAt(id = "1", lat = 37.501, lng = 126.901)
        val secondShop = shopAt(id = "2", lat = 37.520, lng = 126.920)
        val shops = ramenShopsOf(firstShop, secondShop)

        // when
        val result =
            markerCluster.clustering(
                shops = shops,
                bounds = BOUNDS_FIXTURE,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        assertEquals(setOf("1", "2"), result.map { marker -> marker.id }.toSet())
        result.forEach { marker -> assertIs<Marker.SingleMarker>(marker) }
    }

    @Test
    fun `같은 cell의 가게 수가 최소 클러스터 수보다 작으면 모든 가게를 단일 마커로 반환한다`() {
        // given
        val firstShop = shopAt(id = "1", lat = 37.501, lng = 126.901)
        val secondShop = shopAt(id = "2", lat = 37.502, lng = 126.902)
        val shops = ramenShopsOf(firstShop, secondShop)

        // when
        val result =
            markerCluster.clustering(
                shops = shops,
                bounds = BOUNDS_FIXTURE,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        assertEquals(setOf("1", "2"), result.map { marker -> marker.id }.toSet())
        result.forEach { marker -> assertIs<Marker.SingleMarker>(marker) }
    }

    @Test
    fun `줌인하면 기본 줌에서 묶였던 가게들이 단일 마커로 더 빨리 풀린다`() {
        // given
        val firstShop = shopAt(id = "1", lat = 37.5010, lng = 126.9010)
        val secondShop = shopAt(id = "2", lat = 37.5024, lng = 126.9010)
        val thirdShop = shopAt(id = "3", lat = 37.5038, lng = 126.9010)
        val shops = ramenShopsOf(firstShop, secondShop, thirdShop)
        val defaultZoomBounds =
            MapBounds(
                minLat = 37.4925,
                maxLat = 37.5125,
                minLng = 126.8925,
                maxLng = 126.9125,
            )
        val zoomedInBounds =
            MapBounds(
                minLat = 37.4975,
                maxLat = 37.5075,
                minLng = 126.8975,
                maxLng = 126.9075,
            )

        // when
        val defaultZoomMarkers =
            markerCluster.clustering(
                shops = shops,
                bounds = defaultZoomBounds,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )
        val zoomedInMarkers =
            markerCluster.clustering(
                shops = shops,
                bounds = zoomedInBounds,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        assertIs<Marker.ClusterMaker>(defaultZoomMarkers.single())
        assertEquals(setOf("1", "2", "3"), zoomedInMarkers.map { marker -> marker.id }.toSet())
        zoomedInMarkers.forEach { marker -> assertIs<Marker.SingleMarker>(marker) }
    }

    @Test
    fun `현재 bounds 밖의 가게는 클러스터 계산에서 제외한다`() {
        // given
        val visibleShop = shopAt(id = "visible", lat = 37.501, lng = 126.901)
        val leftOutsideShop = shopAt(id = "left-outside", lat = 37.502, lng = 126.899)
        val topOutsideShop = shopAt(id = "top-outside", lat = 37.601, lng = 126.902)
        val shops = ramenShopsOf(visibleShop, leftOutsideShop, topOutsideShop)

        // when
        val result =
            markerCluster.clustering(
                shops = shops,
                bounds = BOUNDS_FIXTURE,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        val marker = assertIs<Marker.SingleMarker>(result.single())
        assertEquals(visibleShop.id, marker.id)
    }

    @Test
    fun `같은 줌에서 지도 이동만으로는 클러스터 구성이 바뀌지 않는다`() {
        // given
        val firstShop = shopAt(id = "1", lat = 37.530, lng = 126.930)
        val secondShop = shopAt(id = "2", lat = 37.531, lng = 126.931)
        val thirdShop = shopAt(id = "3", lat = 37.560, lng = 126.960)
        val shops = ramenShopsOf(firstShop, secondShop, thirdShop)
        val pannedBounds =
            BOUNDS_FIXTURE.copy(
                minLat = BOUNDS_FIXTURE.minLat + 0.02,
                maxLat = BOUNDS_FIXTURE.maxLat + 0.02,
                minLng = BOUNDS_FIXTURE.minLng + 0.02,
                maxLng = BOUNDS_FIXTURE.maxLng + 0.02,
            )

        // when
        val initialMarkers =
            markerCluster.clustering(
                shops = shops,
                bounds = BOUNDS_FIXTURE,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )
        val pannedMarkers =
            markerCluster.clustering(
                shops = shops,
                bounds = pannedBounds,
                viewportWidth = VIEWPORT_SIZE,
                viewportHeight = VIEWPORT_SIZE,
            )

        // then
        assertEquals(
            initialMarkers.map { marker -> marker.id }.toSet(),
            pannedMarkers.map { marker -> marker.id }.toSet(),
        )
    }

    @Test
    fun `클러스터 위치는 포함 가게 좌표의 평균이다`() {
        // given
        val firstShop = shopAt(id = "1", lat = 37.501, lng = 126.901)
        val secondShop = shopAt(id = "2", lat = 37.502, lng = 126.902)
        val thirdShop = shopAt(id = "3", lat = 37.503, lng = 126.903)

        // when
        val marker = clusterOf(firstShop, secondShop, thirdShop)

        // then
        assertEquals(37.502, marker.location.lat, DOUBLE_TOLERANCE)
        assertEquals(126.902, marker.location.lng, DOUBLE_TOLERANCE)
    }

    private fun clusterOf(vararg shops: RamenShop): Marker.ClusterMaker =
        assertIs(
            markerCluster
                .clustering(
                    shops = ramenShopsOf(*shops),
                    bounds = BOUNDS_FIXTURE,
                    viewportWidth = VIEWPORT_SIZE,
                    viewportHeight = VIEWPORT_SIZE,
                ).single(),
        )

    private fun ramenShopsOf(vararg shops: RamenShop): RamenShops =
        RamenShops(
            shops.associateBy { shop -> shop.id },
        )

    private fun shopAt(
        id: String,
        lat: Double,
        lng: Double,
    ): RamenShop =
        ramenShopFixture(
            id = id,
            location = Location(lat = lat, lng = lng),
        )

    companion object {
        private const val VIEWPORT_SIZE = 1000
        private const val DOUBLE_TOLERANCE = 0.000001
    }
}

package com.peto.ramap.ui.map.marker

import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.Marker
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.fake.FakeMarkerRenderAction
import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkerRenderCoordinatorTest {
    private val coordinator =
        MarkerRenderCoordinator(
            keyPolicy = MarkerRenderKeyPolicy(),
        )
    private val action = FakeMarkerRenderAction()

    @Test
    fun `첫 렌더링에서는 모든 마커를 추가한다`() {
        val marker = ramenShopFixture(id = "shop-1").toSingleMarker()

        coordinator.render(
            markers = listOf(marker),
            action = action,
        )

        assertEquals(
            expected = listOf(emptySet()),
            actual = action.removedKeys,
        )
        assertEquals(
            expected = listOf(listOf(MarkerRenderEntry("shop:shop-1", marker))),
            actual = action.addedEntries,
        )
    }

    @Test
    fun `현재 목록에 없는 이전 마커를 제거한다`() {
        val firstMarker = ramenShopFixture(id = "shop-1").toSingleMarker()
        val secondMarker = ramenShopFixture(id = "shop-2").toSingleMarker()

        coordinator.render(
            markers = listOf(firstMarker),
            action = action,
        )
        coordinator.render(
            markers = listOf(secondMarker),
            action = action,
        )

        assertEquals(
            expected = setOf("shop:shop-1"),
            actual = action.removedKeys.last(),
        )
        assertEquals(
            expected = listOf(MarkerRenderEntry("shop:shop-2", secondMarker)),
            actual = action.addedEntries.last(),
        )
    }

    @Test
    fun `같은 key의 마커 내용이 바뀌면 제거 후 다시 추가한다`() {
        val visibleMarker =
            ramenShopFixture(
                id = "shop-1",
                isVisible = true,
            ).toSingleMarker()
        val hiddenMarker =
            ramenShopFixture(
                id = "shop-1",
                isVisible = false,
            ).toSingleMarker()

        coordinator.render(
            markers = listOf(visibleMarker),
            action = action,
        )
        coordinator.render(
            markers = listOf(hiddenMarker),
            action = action,
        )

        assertEquals(
            expected = setOf("shop:shop-1"),
            actual = action.removedKeys.last(),
        )
        assertEquals(
            expected = listOf(MarkerRenderEntry("shop:shop-1", hiddenMarker)),
            actual = action.addedEntries.last(),
        )
    }

    @Test
    fun `변경되지 않은 마커는 다시 추가하지 않는다`() {
        val marker = ramenShopFixture(id = "shop-1").toSingleMarker()

        coordinator.render(
            markers = listOf(marker),
            action = action,
        )
        coordinator.render(
            markers = listOf(marker),
            action = action,
        )

        assertEquals(
            expected = emptySet(),
            actual = action.removedKeys.last(),
        )
        assertEquals(
            expected = emptyList(),
            actual = action.addedEntries.last(),
        )
    }

    @Test
    fun `clear 호출 후에는 같은 마커도 다시 추가한다`() {
        val marker = ramenShopFixture(id = "shop-1").toSingleMarker()

        coordinator.render(
            markers = listOf(marker),
            action = action,
        )
        coordinator.clear()
        coordinator.render(
            markers = listOf(marker),
            action = action,
        )

        assertEquals(
            expected = listOf(MarkerRenderEntry("shop:shop-1", marker)),
            actual = action.addedEntries.last(),
        )
    }

    @Test
    fun `클러스터 마커도 key 기준으로 추가한다`() {
        val marker =
            Marker.ClusterMaker(
                id = "cluster-1",
                location = Location(lat = 37.551, lng = 126.921),
                shops =
                    listOf(
                        ramenShopFixture(id = "shop-1"),
                        ramenShopFixture(id = "shop-2"),
                    ),
            )

        coordinator.render(
            markers = listOf(marker),
            action = action,
        )

        assertEquals(
            expected = listOf(MarkerRenderEntry("cluster:cluster-1", marker)),
            actual = action.addedEntries.single(),
        )
    }
}

private fun RamenShop.toSingleMarker(): Marker.SingleMarker = Marker.SingleMarker(shop = this)

package com.peto.ramap.ui.map.marker

import com.peto.ramap.domain.model.Marker

/**
 * 플랫폼별 지도 SDK 렌더러가 동일한 마커 렌더링 흐름을 따르도록 조정
 *
 * 이전에 렌더링한 마커 상태를 기억하고, 새 마커 목록과 비교해 제거할 마커와 추가할 마커를 계산한 뒤
 * 플랫폼별 [MarkerRenderAction]에 순서대로 위임
 */
internal class MarkerRenderCoordinator(
    private val keyPolicy: MarkerRenderKeyPolicy,
) {
    private var renderedMarkers: Map<String, Marker> = emptyMap()

    /**
     * 현재 마커 목록을 이전 렌더링 상태와 비교해 플랫폼 렌더러에 반영한다.
     *
     * 실행 순서는 마커 바인딩, stale/changed 마커 제거, 신규/변경 마커 추가 순서로 고정된다.
     */
    fun render(
        markers: List<Marker>,
        action: MarkerRenderAction,
    ) {
        val plan = planning(markers)

        applyRenderPlan(plan, action)

        updateRenderedMarkers(plan.currentMarkers)
    }

    /**
     * 새 마커 목록과 기존 렌더링 상태를 비교해 이번 렌더링에서 수행할 작업 계획을 만든다.
     */
    private fun planning(markers: List<Marker>): MarkerRenderPlan {
        val currentEntries = markerRenderEntries(markers)
        val currentMarkers = currentEntries.associate { entry -> entry.key to entry.marker }
        val changedKeys = shouldChangeMarkerKeys(currentMarkers)
        val removeKeys = staleMarkerKeys(currentMarkers) + changedKeys
        val addEntries = shouldAddMarkers(currentEntries, changedKeys)

        return MarkerRenderPlan(
            currentMarkers = currentMarkers,
            removeKeys = removeKeys,
            addEntries = addEntries,
        )
    }

    /**
     * 마커 목록을 렌더링 key와 함께 다루기 위한 entry 목록으로 변환한다.
     */
    private fun markerRenderEntries(markers: List<Marker>): List<MarkerRenderEntry> =
        markers.map { marker ->
            MarkerRenderEntry(
                key = keyPolicy.key(marker),
                marker = marker,
            )
        }

    /**
     * 같은 key로 이미 렌더링됐지만 마커 내용이 달라져 다시 그려야 하는 key를 찾는다.
     */
    private fun shouldChangeMarkerKeys(currentMarkers: Map<String, Marker>): Set<String> =
        currentMarkers
            .filter { (key, marker) ->
                val renderedMarker = renderedMarkers[key]
                renderedMarker != null && renderedMarker != marker
            }.keys

    /**
     * 이전에는 렌더링됐지만 현재 마커 목록에는 없는 key를 찾는다.
     */
    private fun staleMarkerKeys(currentMarkers: Map<String, Marker>): Set<String> = renderedMarkers.keys - currentMarkers.keys

    /**
     * 아직 렌더링되지 않았거나 변경되어 다시 추가해야 하는 마커를 고른다.
     */
    private fun shouldAddMarkers(
        currentEntries: List<MarkerRenderEntry>,
        changedKeys: Set<String>,
    ): List<MarkerRenderEntry> =
        currentEntries.filter { entry ->
            entry.key !in renderedMarkers || entry.key in changedKeys
        }

    /**
     * 계산된 렌더링 계획을 플랫폼별 SDK 작업으로 위임한다.
     */
    private fun applyRenderPlan(
        plan: MarkerRenderPlan,
        action: MarkerRenderAction,
    ) {
        action.bindMarkers(plan.currentMarkers)
        action.removeMarkers(plan.removeKeys)
        action.addMarkers(plan.addEntries)
    }

    /**
     * 다음 렌더링 diff 계산을 위해 현재 렌더링 상태를 저장한다.
     */
    private fun updateRenderedMarkers(markers: Map<String, Marker>) {
        renderedMarkers = markers
    }

    /**
     * 기억 중인 렌더링 상태를 비운다.
     */
    fun clear() {
        renderedMarkers = emptyMap()
    }
}

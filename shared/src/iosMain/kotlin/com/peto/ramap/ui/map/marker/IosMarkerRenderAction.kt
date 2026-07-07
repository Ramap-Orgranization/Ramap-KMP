@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.map.marker

import cocoapods.KakaoMapsSDK.LabelLayer
import com.peto.ramap.domain.model.Marker
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * [MarkerRenderCoordinator]가 계산한 마커 렌더링 작업을 iOS KakaoMap SDK poi layer에 적용한다.
 */
internal class IosMarkerRenderAction(
    private val layer: LabelLayer,
    private val optionFactory: IosMarkerOptionFactory,
    private val markersByPoiId: MutableMap<String, Marker>,
) : MarkerRenderAction {
    override fun removeMarkers(keys: Set<String>) {
        val poiIds = keys.map(optionFactory::poiId)
        layer.removePoisWithPoiIDs(poiIds, callback = null)
        poiIds.forEach(markersByPoiId::remove)
    }

    override fun addMarkers(entries: List<MarkerRenderEntry>) {
        entries.forEach { entry ->
            val poiId = optionFactory.poiId(entry.key)
            val poi =
                layer.addPoiWithOption(
                    option = optionFactory.poiOptions(entry, poiId) ?: return@forEach,
                    at = entry.marker.toMapPoint(),
                    callback = null,
                )

            poi?.show()
            poi?.clickable = true

            if (poi != null) {
                markersByPoiId[poiId] = entry.marker
            }
        }
    }
}

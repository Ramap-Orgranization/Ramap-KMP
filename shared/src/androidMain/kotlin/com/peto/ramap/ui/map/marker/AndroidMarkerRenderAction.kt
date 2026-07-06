package com.peto.ramap.ui.map.marker

import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.label.LabelLayer
import com.peto.ramap.domain.model.Marker
import com.peto.ramap.domain.model.RamenShop

/**
 * [MarkerRenderCoordinator]가 계산한 마커 렌더링 작업을 Android KakaoMap SDK label layer에 적용한다.
 */
internal class AndroidMarkerRenderAction(
    private val kakaoMap: KakaoMap,
    private val labelLayer: LabelLayer,
    private val optionFactory: AndroidMarkerOptionFactory,
    private val renderedLabels: MutableMap<String, String>,
    private val onShopClick: (RamenShop) -> Unit,
    private val onClusterClick: (Marker.ClusterMaker) -> Unit,
) : MarkerRenderAction {
    override fun bindMarkers(markers: Map<String, Marker>) {
        kakaoMap.setOnLabelClickListener { _, _, label ->
            val markerKey =
                label.tag as? String
                    ?: return@setOnLabelClickListener false

            val marker =
                markers[markerKey]
                    ?: return@setOnLabelClickListener false

            handleMarkerClick(marker)
        }
    }

    override fun removeMarkers(keys: Set<String>) {
        keys.forEach { markerKey ->
            val labelId = renderedLabels[markerKey] ?: return@forEach
            labelLayer.getLabel(labelId)?.remove()
            renderedLabels.remove(markerKey)
        }
    }

    override fun addMarkers(entries: List<MarkerRenderEntry>) {
        if (entries.isEmpty()) return

        val labelOptions = entries.mapNotNull(optionFactory::labelOptions)
        labelLayer.addLabels(labelOptions)
        entries.forEach(::rememberRenderedLabel)
    }

    private fun handleMarkerClick(marker: Marker): Boolean {
        when (marker) {
            is Marker.SingleMarker -> onShopClick(marker.shop)
            is Marker.ClusterMaker -> onClusterClick(marker)
        }
        return true
    }

    private fun rememberRenderedLabel(entry: MarkerRenderEntry) {
        renderedLabels[entry.key] = optionFactory.labelId(entry.marker)
    }
}

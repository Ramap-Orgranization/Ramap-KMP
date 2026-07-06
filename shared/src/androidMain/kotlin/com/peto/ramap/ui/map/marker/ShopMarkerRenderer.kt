package com.peto.ramap.ui.map.marker

import android.graphics.Bitmap
import com.kakao.vectormap.KakaoMap
import com.peto.ramap.domain.model.Marker
import com.peto.ramap.domain.model.RamenShop

/**
 * 지도에 표시할 단일 매장 마커와 클러스터 마커를 KakaoMap label layer에 동기화한다.
 *
 * 마커 목록의 diff 계산은 [MarkerRenderCoordinator]에 위임하고, Android SDK label 작업은
 * [AndroidMarkerRenderAction]이 수행한다.
 */
internal class ShopMarkerRenderer {
    private val renderCoordinator = MarkerRenderCoordinator(MarkerRenderKeyPolicy())
    private val renderedLabelIds = mutableMapOf<String, String>()

    /**
     * 현재 지도 상태에서 계산된 마커 목록을 KakaoMap label layer에 반영한다.
     */
    fun render(
        kakaoMap: KakaoMap,
        markerBitmap: Bitmap,
        clusterMarkerBitmap: Bitmap,
        markers: List<Marker>,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ) {
        val manager = kakaoMap.labelManager ?: return
        val labelLayer = manager.layer ?: return

        renderCoordinator.render(
            markers = markers,
            action =
                AndroidMarkerRenderAction(
                    kakaoMap = kakaoMap,
                    labelLayer = labelLayer,
                    optionFactory =
                        AndroidMarkerOptionFactory(
                            manager = manager,
                            markerBitmap = markerBitmap,
                            clusterMarkerBitmap = clusterMarkerBitmap,
                        ),
                    renderedLabels = renderedLabelIds,
                    onShopClick = onShopClick,
                    onClusterClick = onClusterClick,
                ),
        )
    }
}

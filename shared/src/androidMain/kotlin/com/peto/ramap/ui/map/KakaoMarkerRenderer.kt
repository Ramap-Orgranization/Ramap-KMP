package com.peto.ramap.ui.map

import android.graphics.Bitmap
import android.graphics.Color
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import com.peto.ramap.core.config.MarkerClusterConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.Marker
import com.peto.ramap.domain.model.RamenShop

/**
 * 지도에 표시할 단일 매장 마커와 클러스터 마커를 KakaoMap label layer에 동기화한다.
 *
 * 렌더링된 label ID를 마커 key 기준으로 기억해 현재 마커 목록에 없는 label만 제거하고
 * 새로 등장한 마커 label만 추가한다.
 */
internal class KakaoMarkerRenderer(
    private val clusterBitmapFactory: RamenShopClusterBitmapFactory,
    private val myLocationMarkerBitmapFactory: MyLocationMarkerBitmapFactory = MyLocationMarkerBitmapFactory(),
) {
    private val renderedLabelIdsByKey = mutableMapOf<String, String>()

    /**
     * 현재 지도 상태에서 계산된 마커 목록을 KakaoMap label layer에 반영한다.
     */
    fun render(
        kakaoMap: KakaoMap,
        markerBitmap: Bitmap,
        markers: List<Marker>,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ) {
        val markersByKey = markers.associateBy(::markerKey)
        setUpKaKaoMapListener(kakaoMap, markersByKey, onShopClick, onClusterClick)

        val manager = kakaoMap.labelManager ?: return
        val labelLayer = manager.layer ?: return

        removeStaleMarkers(markersByKey.keys, labelLayer)
        if (markers.isEmpty()) return

        addNewMarkers(labelLayer, manager, markerBitmap, clusterBitmapFactory, markers)
    }

    /**
     * 현재 위치 마커를 고정 label id로 추가하거나 최신 좌표로 갱신한다.
     */
    fun renderMyLocation(
        kakaoMap: KakaoMap,
        location: Location,
    ) {
        val manager = kakaoMap.labelManager ?: return
        val labelLayer = manager.layer ?: return
        val styles = createMyLocationStyles(manager) ?: return

        labelLayer.getLabel(MY_LOCATION_LABEL_ID)?.remove()
        labelLayer.addLabels(
            listOf(
                LabelOptions
                    .from(
                        MY_LOCATION_LABEL_ID,
                        LatLng.from(location.lat, location.lng),
                    ).setStyles(styles)
                    .setClickable(false),
            ),
        )
    }

    /**
     * label 클릭 이벤트를 마커 타입별 동작으로 변환한다.
     */
    private fun setUpKaKaoMapListener(
        kakaoMap: KakaoMap,
        markersByKey: Map<String, Marker>,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ) {
        kakaoMap.setOnLabelClickListener { _, _, label ->
            val markerKey = label.tag as? String ?: return@setOnLabelClickListener false
            val marker = markersByKey[markerKey] ?: return@setOnLabelClickListener false
            handleMarkerClick(marker, onShopClick, onClusterClick)
        }
    }

    /**
     * 마커 타입에 따라 단일 매장 선택 또는 클러스터 선택 콜백을 실행한다.
     */
    private fun handleMarkerClick(
        marker: Marker,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ): Boolean {
        when (marker) {
            is Marker.SingleMarker -> onShopClick(marker.shop)
            is Marker.ClusterMaker -> onClusterClick(marker)
        }
        return true
    }

    /**
     * 현재 마커 목록에 없는 label만 SDK layer에서 제거한다.
     */
    private fun removeStaleMarkers(
        visibleMarkerKeys: Set<String>,
        labelLayer: LabelLayer,
    ) {
        val staleMarkerKeys = renderedLabelIdsByKey.keys - visibleMarkerKeys

        staleMarkerKeys.forEach { markerKey ->
            val labelId = renderedLabelIdsByKey[markerKey] ?: return@forEach
            labelLayer.getLabel(labelId)?.remove()
            renderedLabelIdsByKey.remove(markerKey)
        }
    }

    /**
     * 아직 label layer에 없는 마커만 LabelOptions로 변환해 추가한다.
     */
    private fun addNewMarkers(
        labelLayer: LabelLayer,
        manager: LabelManager,
        markerBitmap: Bitmap,
        clusterBitmapFactory: RamenShopClusterBitmapFactory,
        markers: List<Marker>,
    ) {
        val newMarkers = markers.filterNot { marker -> markerKey(marker) in renderedLabelIdsByKey }
        if (newMarkers.isEmpty()) return

        val markerStyles = createMarkerStyles(manager, markerBitmap) ?: return
        val labelOptions =
            newMarkers.mapNotNull { marker ->
                labelOptions(marker, manager, markerStyles, clusterBitmapFactory)
            }

        labelLayer.addLabels(labelOptions)
        newMarkers.forEach(::rememberRenderedLabel)
    }

    /**
     * 단일 매장 마커에 사용할 기존 라멘 마커 스타일을 가져오거나 등록한다.
     */
    private fun createMarkerStyles(
        manager: LabelManager,
        markerBitmap: Bitmap,
    ): LabelStyles? =
        manager.getLabelStyles(MarkerConfig.Single.STYLE_ID)
            ?: manager.addLabelStyles(
                LabelStyles.from(
                    MarkerConfig.Single.STYLE_ID,
                    baseLabelStyle(markerBitmap).setAnchorPoint(0.5f, 1.0f),
                ),
            )

    /**
     * 클러스터 마커에 사용할 별도 label style을 가져오거나 등록한다.
     */
    private fun createClusterStyles(
        manager: LabelManager,
        clusterBitmapFactory: RamenShopClusterBitmapFactory,
        cluster: Marker.ClusterMaker,
    ): LabelStyles? {
        val styleId = clusterStyleId(cluster.count)

        return manager.getLabelStyles(styleId)
            ?: manager.addLabelStyles(
                LabelStyles.from(
                    styleId,
                    LabelStyle
                        .from(clusterBitmapFactory.create(cluster.count))
                        .setAnchorPoint(0.5f, 0.5f),
                ),
            )
    }

    private fun createMyLocationStyles(manager: LabelManager): LabelStyles? =
        manager.getLabelStyles(MY_LOCATION_STYLE_ID)
            ?: manager.addLabelStyles(
                LabelStyles.from(
                    MY_LOCATION_STYLE_ID,
                    LabelStyle
                        .from(myLocationMarkerBitmapFactory.create())
                        .setAnchorPoint(0.5f, 0.5f),
                ),
            )

    /**
     * 매장/클러스터 마커가 공유하는 이미지와 텍스트 스타일을 만든다.
     */
    private fun baseLabelStyle(markerBitmap: Bitmap): LabelStyle =
        LabelStyle
            .from(markerBitmap)
            .setTextStyles(
                LabelTextStyle.from(
                    MarkerConfig.Single.LABEL_TEXT_SIZE,
                    MarkerConfig.Single.LABEL_TEXT_COLOR,
                    MarkerConfig.Single.LABEL_STROKE_WIDTH,
                    Color.WHITE,
                ),
            )

    /**
     * 마커 타입에 맞는 label option을 만든다.
     */
    private fun labelOptions(
        marker: Marker,
        manager: LabelManager,
        markerStyles: LabelStyles,
        clusterBitmapFactory: RamenShopClusterBitmapFactory,
    ): LabelOptions? =
        when (marker) {
            is Marker.SingleMarker -> singleMarkerLabelOptions(marker, markerStyles)
            is Marker.ClusterMaker -> {
                val clusterStyles = createClusterStyles(manager, clusterBitmapFactory, marker)
                clusterStyles?.let { styles -> baseLabelOptions(marker, styles) }
            }
        }

    /**
     * 단일 매장 마커 label option을 만든다.
     */
    private fun singleMarkerLabelOptions(
        marker: Marker.SingleMarker,
        markerStyles: LabelStyles,
    ): LabelOptions =
        baseLabelOptions(marker, markerStyles)
            .setTexts(LabelTextBuilder().setTexts(marker.shop.name))

    /**
     * 마커 공통 좌표, 클릭 여부, tag 값을 가진 label option을 만든다.
     */
    private fun baseLabelOptions(
        marker: Marker,
        styles: LabelStyles,
    ): LabelOptions =
        LabelOptions
            .from(
                markerLabelId(marker),
                LatLng.from(marker.location.lat, marker.location.lng),
            ).setStyles(styles)
            .setClickable(true)
            .setTag(markerKey(marker))

    /**
     * SDK label 제거를 위해 렌더링한 마커 key와 label ID를 저장한다.
     */
    private fun rememberRenderedLabel(marker: Marker) {
        renderedLabelIdsByKey[markerKey(marker)] = markerLabelId(marker)
    }

    /**
     * 마커 타입이 달라도 충돌하지 않는 내부 식별 key를 만든다.
     */
    private fun markerKey(marker: Marker): String =
        when (marker) {
            is Marker.SingleMarker -> "$SINGLE_MARKER_KEY_PREFIX${marker.id}"
            is Marker.ClusterMaker -> "$CLUSTER_MARKER_KEY_PREFIX${marker.id}"
        }

    /**
     * KakaoMap label layer에서 사용할 타입별 label ID를 만든다.
     */
    private fun markerLabelId(marker: Marker): String =
        when (marker) {
            is Marker.SingleMarker -> "$SINGLE_MARKER_LABEL_PREFIX${marker.id}"
            is Marker.ClusterMaker -> "$CLUSTER_MARKER_LABEL_PREFIX${marker.id}"
        }

    private fun clusterStyleId(count: Int): String = "${MarkerConfig.Cluster.STYLE_ID}-${clusterCountBucket(count)}"

    private fun clusterCountBucket(count: Int): String = MarkerClusterConfig.countStyleBucket(count)

    companion object {
        private const val SINGLE_MARKER_KEY_PREFIX = "shop:"
        private const val CLUSTER_MARKER_KEY_PREFIX = "cluster:"
        private const val SINGLE_MARKER_LABEL_PREFIX = "ramen-shop-"
        private const val CLUSTER_MARKER_LABEL_PREFIX = "ramen-cluster-"
        private const val MY_LOCATION_STYLE_ID = "my-location-marker-style"
        private const val MY_LOCATION_LABEL_ID = "my-location-marker"
    }
}

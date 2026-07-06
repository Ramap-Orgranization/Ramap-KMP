package com.peto.ramap.ui.map.marker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.core.config.MarkerClusterConfig
import com.peto.ramap.core.config.MarkerConfig
import com.peto.ramap.domain.model.Marker
import com.peto.ramap.ui.map.RamenShopClusterBitmapFactory

/**
 * 마커를 Android KakaoMap SDK label option과 style로 변환한다.
 */
internal class AndroidMarkerOptionFactory(
    private val manager: LabelManager,
    private val markerBitmap: Bitmap,
    private val clusterMarkerBitmap: Bitmap,
) {
    private val clusterBitmapFactory = RamenShopClusterBitmapFactory()

    fun labelOptions(entry: MarkerRenderEntry): LabelOptions? {
        val markerStyles = markerStyle() ?: return null
        val hiddenMarkerStyles = hiddenMarkerStyle() ?: return null

        return when (val marker = entry.marker) {
            is Marker.SingleMarker ->
                singleMarkerLabelOptions(
                    markerKey = entry.key,
                    marker = marker,
                    markerStyles = if (marker.shop.isVisible) markerStyles else hiddenMarkerStyles,
                )
            is Marker.ClusterMaker -> {
                val clusterStyles = clusterStyle(marker)
                clusterStyles?.let { styles ->
                    baseLabelOptions(
                        markerKey = entry.key,
                        marker = marker,
                        styles = styles,
                    )
                }
            }
        }
    }

    fun labelId(marker: Marker): String =
        when (marker) {
            is Marker.SingleMarker -> "$SINGLE_MARKER_LABEL_PREFIX${marker.id}"
            is Marker.ClusterMaker -> "$CLUSTER_MARKER_LABEL_PREFIX${marker.id}"
        }

    private fun markerStyle(): LabelStyles? =
        manager.getLabelStyles(MarkerConfig.Single.STYLE_ID)
            ?: manager.addLabelStyles(
                LabelStyles.from(
                    MarkerConfig.Single.STYLE_ID,
                    baseLabelStyle(markerBitmap).setAnchorPoint(0.5f, 1.0f),
                ),
            )

    private fun hiddenMarkerStyle(): LabelStyles? =
        manager.getLabelStyles(MarkerConfig.Single.HIDDEN_STYLE_ID)
            ?: manager.addLabelStyles(
                LabelStyles.from(
                    MarkerConfig.Single.HIDDEN_STYLE_ID,
                    baseLabelStyle(markerBitmap.withAlpha(MapInteractionConfig.HIDDEN_SHOP_ALPHA))
                        .setAnchorPoint(0.5f, 1.0f),
                ),
            )

    private fun clusterStyle(cluster: Marker.ClusterMaker): LabelStyles? {
        val styleId = clusterStyleId(cluster.count)

        return manager.getLabelStyles(styleId)
            ?: manager.addLabelStyles(
                LabelStyles.from(
                    styleId,
                    LabelStyle
                        .from(
                            clusterBitmapFactory.create(
                                count = cluster.count,
                                markerBitmap = clusterMarkerBitmap,
                            ),
                        ).setAnchorPoint(0.5f, 0.5f),
                ),
            )
    }

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

    private fun singleMarkerLabelOptions(
        markerKey: String,
        marker: Marker.SingleMarker,
        markerStyles: LabelStyles,
    ): LabelOptions =
        baseLabelOptions(
            markerKey = markerKey,
            marker = marker,
            styles = markerStyles,
        ).setTexts(LabelTextBuilder().setTexts(marker.shop.name))

    private fun baseLabelOptions(
        markerKey: String,
        marker: Marker,
        styles: LabelStyles,
    ): LabelOptions =
        LabelOptions
            .from(
                labelId(marker),
                LatLng.from(marker.location.lat, marker.location.lng),
            ).setStyles(styles)
            .setClickable(true)
            .setTag(markerKey)

    private fun clusterStyleId(count: Int): String = "${MarkerConfig.Cluster.STYLE_ID}-${clusterCountBucket(count)}"

    private fun clusterCountBucket(count: Int): String = MarkerClusterConfig.countStyleBucket(count)

    private companion object {
        private const val SINGLE_MARKER_LABEL_PREFIX = "ramen-shop-"
        private const val CLUSTER_MARKER_LABEL_PREFIX = "ramen-cluster-"

        private fun Bitmap.withAlpha(alpha: Float): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.alpha = (alpha * ALPHA_MAX).toInt()
                }

            Canvas(bitmap).drawBitmap(this, 0f, 0f, paint)

            return bitmap
        }

        private const val ALPHA_MAX = 255
    }
}

package com.peto.ramap.ui.map.marker

import android.graphics.Bitmap
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.peto.ramap.domain.model.Location

internal class MyLocationRenderer {
    /**
     * 현재 위치 마커를 고정 label id로 추가하거나 최신 좌표로 갱신한다.
     */
    fun render(
        kakaoMap: KakaoMap,
        markerBitmap: Bitmap,
        location: Location,
    ) {
        val manager = kakaoMap.labelManager ?: return
        val labelLayer = manager.layer ?: return
        val styles = myLocationStyles(manager, markerBitmap) ?: return

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

    private fun myLocationStyles(
        manager: LabelManager,
        markerBitmap: Bitmap,
    ): LabelStyles? =
        manager.getLabelStyles(MY_LOCATION_STYLE_ID)
            ?: manager.addLabelStyles(
                LabelStyles.from(
                    MY_LOCATION_STYLE_ID,
                    LabelStyle
                        .from(markerBitmap)
                        .setAnchorPoint(0.5f, 0.5f),
                ),
            )

    private companion object {
        private const val MY_LOCATION_STYLE_ID = "my-location-marker-style"
        private const val MY_LOCATION_LABEL_ID = "my-location-marker"
    }
}

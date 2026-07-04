package com.peto.ramap.ui.map

import androidx.lifecycle.Lifecycle
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.domain.model.MapBounds
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android `MapView`와 KakaoMap SDK의 생명주기를 연결한다.
 *
 * 지도 시작, Compose 생명주기에 따른 resume/pause/finish, 지도 준비 이후 bounds 알림을 관리한다.
 */
internal class KakaoMapLifecycleController(
    private val mapView: MapView,
    private val boundsCalculator: MapBoundsCalculator,
) {
    private val isMapStarted = AtomicBoolean(false)

    fun startMap(
        lifecycle: Lifecycle,
        onMapReady: (KakaoMap) -> Unit,
        onBoundsChanged: (MapBounds) -> Unit,
    ) {
        if (!isMapStarted.compareAndSet(false, true)) return

        startMap(onBoundsChanged, onMapReady)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.resume()
        }
    }

    private fun startMap(
        onBoundsChanged: (MapBounds) -> Unit,
        onMapReady: (KakaoMap) -> Unit,
    ) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() = Unit

                override fun onMapError(error: Exception) = Unit
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(kakaoMap: KakaoMap) {
                    kakaoMap.setCameraMinLevel(MapInteractionConfig.MAX_ZOOM_OUT_LEVEL)
                    onMapReady(kakaoMap)
                    bindBoundsChangedListener(kakaoMap, onBoundsChanged)

                    mapView.post {
                        notifyCurrentBounds(kakaoMap, onBoundsChanged)
                    }
                }
            },
        )
    }

    fun resume() {
        if (isMapStarted.get()) {
            mapView.resume()
        }
    }

    fun pause() {
        if (isMapStarted.get()) {
            mapView.pause()
        }
    }

    fun finish() {
        isMapStarted.set(false)
        mapView.finish()
    }

    private fun bindBoundsChangedListener(
        kakaoMap: KakaoMap,
        onBoundsChanged: (MapBounds) -> Unit,
    ) {
        kakaoMap.setOnCameraMoveEndListener { map, _, _ ->
            notifyCurrentBounds(map, onBoundsChanged)
        }
    }

    private fun notifyCurrentBounds(
        kakaoMap: KakaoMap,
        onBoundsChanged: (MapBounds) -> Unit,
    ) {
        boundsCalculator
            .currentBounds(
                kakaoMap = kakaoMap,
                width = mapView.width,
                height = mapView.height,
            )?.let(onBoundsChanged)
    }
}

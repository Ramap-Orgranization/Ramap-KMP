@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.peto.ramap.ui.map

import cocoapods.KakaoMapsSDK.KMController
import cocoapods.KakaoMapsSDK.KMViewContainer
import cocoapods.KakaoMapsSDK.KakaoMap
import cocoapods.KakaoMapsSDK.KakaoMapEventDelegateProtocol
import cocoapods.KakaoMapsSDK.MapControllerDelegateProtocol
import cocoapods.KakaoMapsSDK.MapPoint
import cocoapods.KakaoMapsSDK.MapviewInfo
import cocoapods.KakaoMapsSDK.MoveBy
import cocoapods.KakaoMapsSDK.create
import com.peto.ramap.core.config.DefaultMapConfig
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.Marker
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectZero
import platform.darwin.NSObject

private const val DEFAULT_APP_NAME = "openmap"
private const val DEFAULT_VIEW_INFO_NAME = "map"

/**
 * iOS Kakao Maps SDK 생명주기와 delegate를 Compose 상태에 연결한다.
 */
class IosKakaoMapController(
    private val onBoundsChanged: (MapBounds) -> Unit,
    private val onShopClick: (RamenShop) -> Unit,
    private val onMyLocationChanged: (Location) -> Unit,
) : NSObject(),
    MapControllerDelegateProtocol,
    KakaoMapEventDelegateProtocol {
    private val mapViewContainer = KMViewContainer(frame = CGRectZero.readValue())
    val view =
        IosKakaoMapContainer(
            mapViewContainer = mapViewContainer,
            onMeasured = { start() },
            onTap = ::selectNearestShopAt,
        )

    private val controller = KMController(viewContainer = mapViewContainer)
    private val cameraController = KakaoCameraController()
    private val markerRenderer = KakaoMarkerRenderer()
    private val mapViewName = "ramap"
    private var pendingMarkers: List<Marker>? = null
    private var pendingMyLocationCoordinate: IosMapCoordinate? = null
    private var isStarted = false
    private var isMapViewAdded = false

    fun start() {
        if (isStarted || !hasMeasuredSize()) return

        controller.delegate = this
        if (!controller.prepareEngine()) return

        isStarted = true
        controller.activateEngine()
    }

    private fun hasMeasuredSize(): Boolean =
        mapViewContainer.bounds.useContents {
            size.width > 0.0 && size.height > 0.0
        }

    override fun addViews() {
        controller.addView(createDefaultMapViewInfo())
    }

    private fun createDefaultMapViewInfo(): MapviewInfo =
        MapviewInfo.create(
            mapViewName,
            DEFAULT_APP_NAME,
            DEFAULT_VIEW_INFO_NAME,
            MapPoint(
                longitude = DefaultMapConfig.location.lng,
                latitude = DefaultMapConfig.location.lat,
            ),
            DefaultMapConfig.ZOOM_LEVEL.toLong(),
            true,
        )

    override fun addViewSucceeded(
        viewName: String,
        viewInfoName: String,
    ) {
        val kakaoMap = getKakaoMap() ?: return

        isMapViewAdded = true
        kakaoMap.cameraMinLevel = MapInteractionConfig.MAX_ZOOM_OUT_LEVEL.toLong()
        kakaoMap.eventDelegate = this
        notifyCurrentBounds(kakaoMap)
        pendingMarkers?.let(::renderMarkers)
        pendingMyLocationCoordinate?.let(::moveToCoordinate)
        pendingMyLocationCoordinate = null
    }

    override fun cameraDidStoppedWithKakaoMap(
        kakaoMap: KakaoMap,
        by: MoveBy,
    ) {
        notifyCurrentBounds(kakaoMap)
    }

    private fun notifyCurrentBounds(kakaoMap: KakaoMap) {
        val width = mapViewContainer.bounds.useContents { size.width }
        val height = mapViewContainer.bounds.useContents { size.height }

        val cornerLocations =
            kakaoMap.visibleCornerLocations(
                width = width,
                height = height,
            ) ?: return

        val bounds = MapBounds.fromLocations(cornerLocations) ?: return

        onBoundsChanged(bounds)
    }

    fun updateMarkers(markers: List<Marker>) {
        pendingMarkers = markers
        renderMarkers(markers)
    }

    private fun renderMarkers(markers: List<Marker>) {
        if (!isMapViewAdded) return

        val kakaoMap = getKakaoMap() ?: return
        markerRenderer.render(
            kakaoMap = kakaoMap,
            markers = markers,
            onShopClick = onShopClick,
            onClusterClick = { cluster ->
                cameraController.focusRamenShops(
                    kakaoMap = kakaoMap,
                    shops = cluster.shops,
                )
            },
        )
    }

    fun updateFocusShops(
        shops: List<RamenShop>,
        currentLocation: Location?,
    ) {
        if (!isMapViewAdded) return

        val kakaoMap = getKakaoMap() ?: return
        cameraController.focusRamenShops(
            kakaoMap = kakaoMap,
            shops = shops,
            currentLocation = currentLocation,
        )
    }

    fun moveToLocation(location: Location) {
        onMyLocationChanged(location)
        moveToCoordinate(
            IosMapCoordinate(
                latitude = location.lat,
                longitude = location.lng,
            ),
        )
    }

    private fun moveToCoordinate(coordinate: IosMapCoordinate) {
        val kakaoMap = getKakaoMap()

        if (kakaoMap == null || !isMapViewAdded) {
            pendingMyLocationCoordinate = coordinate
            return
        }

        markerRenderer.renderMyLocation(kakaoMap, coordinate)
        cameraController.moveToCoordinate(kakaoMap, coordinate)
    }

    override fun poiDidTappedWithKakaoMap(
        kakaoMap: KakaoMap,
        layerID: String,
        poiID: String,
        position: MapPoint,
    ) {
        markerRenderer.handlePoiTap(
            poiId = poiID,
            onShopClick = onShopClick,
            onClusterClick = { cluster ->
                cameraController.focusRamenShops(
                    kakaoMap = kakaoMap,
                    shops = cluster.shops,
                )
            },
        )
    }

    private fun selectNearestShopAt(point: CValue<CGPoint>) {
        val kakaoMap = getKakaoMap() ?: return
        markerRenderer.selectNearestShopAt(
            kakaoMap = kakaoMap,
            point = point,
            onShopClick = onShopClick,
            onClusterClick = { cluster ->
                cameraController.focusRamenShops(
                    kakaoMap = kakaoMap,
                    shops = cluster.shops,
                )
            },
        )
    }

    private fun getKakaoMap(): KakaoMap? = controller.getView(mapViewName) as? KakaoMap

    fun dispose() {
        markerRenderer.clear()
        controller.pauseEngine()
        controller.resetEngine()
        controller.delegate = null
        isStarted = false
        isMapViewAdded = false
    }
}

private fun KakaoMap.visibleCornerLocations(
    width: Double,
    height: Double,
): List<Location>? {
    if (width <= 0.0 || height <= 0.0) return null

    return listOf(
        coordinateAt(CGPointMake(0.0, 0.0)),
        coordinateAt(CGPointMake(width, 0.0)),
        coordinateAt(CGPointMake(0.0, height)),
        coordinateAt(CGPointMake(width, height)),
    )
}

private fun KakaoMap.coordinateAt(point: CValue<CGPoint>): Location =
    getPosition(point)
        .wgsCoord
        .useContents {
            Location(
                lat = latitude,
                lng = longitude,
            )
        }

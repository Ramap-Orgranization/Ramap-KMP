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
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject

private const val DEFAULT_APP_NAME = "openmap"
private const val DEFAULT_VIEW_INFO_NAME = "map"

/**
 * iOS Kakao Maps SDK 생명주기와 delegate를 Compose 상태에 연결한다.
 *
 * 실제 카메라 이동, 마커 렌더링, 화면 영역 계산은 Android와 같은 이름의 actual 객체에 위임한다.
 */
class IosKakaoMapController(
    private val onBoundsChanged: (MapBounds) -> Unit,
    private val onShopClick: (RamenShop) -> Unit,
    private val onMyLocationChanged: (Location) -> Unit,
    private val onLocationPermissionBlocked: () -> Unit,
) : NSObject(),
    MapControllerDelegateProtocol,
    KakaoMapEventDelegateProtocol,
    CLLocationManagerDelegateProtocol {
    private val mapViewContainer = KMViewContainer(frame = CGRectZero.readValue())
    val view =
        IosKakaoMapContainer(
            mapViewContainer = mapViewContainer,
            onMeasured = { start() },
            onTap = ::selectNearestShopAt,
        )

    private val controller = KMController(viewContainer = mapViewContainer)
    private val locationManager =
        CLLocationManager().apply {
            delegate = this@IosKakaoMapController
            desiredAccuracy = kCLLocationAccuracyHundredMeters
        }
    private val boundsCalculator = MapBoundsCalculator()
    private val cameraController = KakaoCameraController()
    private val markerRenderer = KakaoMarkerRenderer()
    private val mapViewName = "ramap"
    private var pendingShops: RamenShops? = null
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
        pendingShops?.let(::renderRamenShopMarkers)
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

        boundsCalculator
            .currentBounds(
                kakaoMap = kakaoMap,
                width = width,
                height = height,
            )?.let(onBoundsChanged)
    }

    fun updateShops(shops: RamenShops) {
        pendingShops = shops
        renderRamenShopMarkers(shops)
    }

    private fun renderRamenShopMarkers(shops: RamenShops) {
        if (!isMapViewAdded) return

        val kakaoMap = getKakaoMap() ?: return
        markerRenderer.render(
            kakaoMap = kakaoMap,
            shops = shops,
            onShopClick = onShopClick,
        )
    }

    fun updateFocusShops(
        shops: List<RamenShop>,
        focusNearestToCurrentLocation: Boolean,
    ) {
        if (!isMapViewAdded) return

        val kakaoMap = getKakaoMap() ?: return
        cameraController.focusRamenShops(
            kakaoMap = kakaoMap,
            shops = shops,
            currentLocation = locationManager.location.takeIf { focusNearestToCurrentLocation },
        )
    }

    fun moveToMyLocation() {
        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> moveToKnownOrRequestedLocation()

            kCLAuthorizationStatusNotDetermined -> locationManager.requestWhenInUseAuthorization()

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> onLocationPermissionBlocked()

            else -> locationManager.requestWhenInUseAuthorization()
        }
    }

    private fun moveToKnownOrRequestedLocation() {
        val location = locationManager.location

        if (location != null) {
            moveToLocation(location)
        } else {
            locationManager.requestLocation()
        }
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when {
            manager.isAuthorized() -> moveToKnownOrRequestedLocation()
            manager.isBlocked() -> onLocationPermissionBlocked()
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus,
    ) {
        when {
            didChangeAuthorizationStatus.isAuthorized() -> moveToKnownOrRequestedLocation()
            didChangeAuthorizationStatus.isBlocked() -> onLocationPermissionBlocked()
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        moveToLocation(location)
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) = Unit

    private fun moveToLocation(location: CLLocation) {
        val coordinate = location.toCoordinate()
        onMyLocationChanged(
            Location(
                lat = coordinate.latitude,
                lng = coordinate.longitude,
            ),
        )
        moveToCoordinate(coordinate)
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
        )
    }

    private fun selectNearestShopAt(point: CValue<CGPoint>) {
        val kakaoMap = getKakaoMap() ?: return
        markerRenderer.selectNearestShopAt(
            kakaoMap = kakaoMap,
            point = point,
            onShopClick = onShopClick,
        )
    }

    private fun getKakaoMap(): KakaoMap? = controller.getView(mapViewName) as? KakaoMap

    private fun CLLocationManager.isAuthorized(): Boolean = authorizationStatus.isAuthorized()

    private fun CLLocationManager.isBlocked(): Boolean = authorizationStatus.isBlocked()

    private fun CLAuthorizationStatus.isAuthorized(): Boolean =
        this == kCLAuthorizationStatusAuthorizedWhenInUse ||
            this == kCLAuthorizationStatusAuthorizedAlways

    private fun CLAuthorizationStatus.isBlocked(): Boolean =
        this == kCLAuthorizationStatusDenied ||
            this == kCLAuthorizationStatusRestricted

    private fun CLLocation.toCoordinate(): IosMapCoordinate =
        coordinate.useContents {
            IosMapCoordinate(
                latitude = latitude,
                longitude = longitude,
            )
        }

    fun dispose() {
        markerRenderer.clear()
        controller.pauseEngine()
        controller.resetEngine()
        controller.delegate = null
        isStarted = false
        isMapViewAdded = false
    }
}

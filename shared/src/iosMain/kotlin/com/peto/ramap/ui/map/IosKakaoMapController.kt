@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.peto.ramap.ui.map

import cocoapods.KakaoMapsSDK.AreaRect
import cocoapods.KakaoMapsSDK.CameraUpdate
import cocoapods.KakaoMapsSDK.CompetitionTypeNone
import cocoapods.KakaoMapsSDK.CompetitionUnitPoi
import cocoapods.KakaoMapsSDK.KMController
import cocoapods.KakaoMapsSDK.KMViewContainer
import cocoapods.KakaoMapsSDK.KakaoMap
import cocoapods.KakaoMapsSDK.KakaoMapEventDelegateProtocol
import cocoapods.KakaoMapsSDK.LabelLayer
import cocoapods.KakaoMapsSDK.LabelLayerOptions
import cocoapods.KakaoMapsSDK.LabelManager
import cocoapods.KakaoMapsSDK.MapControllerDelegateProtocol
import cocoapods.KakaoMapsSDK.MapPoint
import cocoapods.KakaoMapsSDK.MapviewInfo
import cocoapods.KakaoMapsSDK.MoveBy
import cocoapods.KakaoMapsSDK.OrderingTypeRank
import cocoapods.KakaoMapsSDK.PerLevelPoiStyle
import cocoapods.KakaoMapsSDK.PoiIconStyle
import cocoapods.KakaoMapsSDK.PoiOptions
import cocoapods.KakaoMapsSDK.PoiStyle
import cocoapods.KakaoMapsSDK.PoiText
import cocoapods.KakaoMapsSDK.PoiTextLineStyle
import cocoapods.KakaoMapsSDK.PoiTextStyle
import cocoapods.KakaoMapsSDK.PoiTransition
import cocoapods.KakaoMapsSDK.TextStyle
import cocoapods.KakaoMapsSDK.TransitionTypeNone
import cocoapods.KakaoMapsSDK.create
import com.peto.ramap.core.config.DefaultMapConfig
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.core.config.MarkerConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.domain.model.nearestTo
import com.peto.ramap.ui.extension.alphaComponent
import com.peto.ramap.ui.extension.blueComponent
import com.peto.ramap.ui.extension.greenComponent
import com.peto.ramap.ui.extension.redComponent
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGBlendMode
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
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
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.darwin.NSObject

private const val DEFAULT_APP_NAME = "openmap"
private const val DEFAULT_VIEW_INFO_NAME = "map"
private const val MARKER_IMAGE_NAME = "marker_ramen"
private const val MARKER_LAYER_Z_ORDER = 10L
private const val MY_LOCATION_LAYER_Z_ORDER = 20L
private const val MY_LOCATION_STYLE_ID = "my-location-marker-style"
private const val MY_LOCATION_POI_ID = "my-location-marker"
private const val MY_LOCATION_IMAGE_SIZE = 34.0
private const val MY_LOCATION_OUTER_INSET = 2.0
private const val MY_LOCATION_OUTER_SIZE = 30.0
private const val MY_LOCATION_INNER_INSET = 7.0
private const val MY_LOCATION_INNER_SIZE = 20.0
private const val MARKER_TAP_RADIUS_METERS = 80.0
private const val VISIBLE_MARKER_ALPHA = 1.0
private const val EMPTY_FOCUS_KEY = ""

/**
 * iOS Kakao Maps SDK의 생명주기, 마커 렌더링, 카메라 이동을 Compose 상태와 연결하는 컨트롤러
 *
 * [KakaoMapView]의 `UIKitView`에서 생성되어 SDK 엔진을 준비하고 공통 UI 상태에서 내려오는
 * 매장 목록을 마커로 렌더링한다.
 *
 * 지도 카메라가 멈출 때는 현재 화면 영역을 [MapBounds]로
 * 계산해 ViewModel에 전달하고 마커 또는 지도 탭으로 선택된 매장은 [onShopClick]으로
 * 다시 올려 보낸다.
 *
 * 검색 결과나 상세 선택으로 전달되는 focus 대상은 iOS SDK의 [CameraUpdate]로 변환해 단일
 * 매장은 중심 이동, 여러 매장은 [AreaRect] 기반 영역 맞춤 이동으로 처리한다.
 *
 * @param onBoundsChanged 현재 지도 화면 영역이 바뀌었을 때 호출되는 콜백.
 * @param onShopClick 지도 위 매장 마커가 선택되었을 때 호출되는 콜백.
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
    private val mapViewName = "ramap"
    private val markerLayerId = "ramen-shop-marker-layer"
    private val myLocationLayerId = "my-location-marker-layer"
    private val renderedShopIds = mutableSetOf<String>()
    private val shopsByPoiId = mutableMapOf<String, RamenShop>()
    private var pendingShops: RamenShops? = null
    private var pendingMyLocationCoordinate: IosMapCoordinate? = null
    private var isStarted = false
    private var isMapViewAdded = false
    private var lastFocusKey = EMPTY_FOCUS_KEY

    /**
     * Kakao 지도 엔진을 준비하고 활성화한다.
     *
     * SDK가 0 크기 컨테이너에서 준비되지 않도록 [IosKakaoMapContainer]의 측정 완료 이후에만
     * 호출되며, 이미 시작된 엔진은 다시 준비하지 않는다.
     */
    fun start() {
        if (isStarted || !hasMeasuredSize()) return

        startEngine()
    }

    /**
     * Kakao 지도 엔진을 시작할 수 있을 만큼 컨테이너가 실제 크기를 가졌는지 확인한다.
     */
    private fun hasMeasuredSize(): Boolean =
        mapViewContainer.bounds.useContents {
            size.width > 0.0 && size.height > 0.0
        }

    /**
     * Kakao Maps SDK 엔진을 준비하고 활성화 상태로 전환한다.
     */
    private fun startEngine() {
        controller.delegate = this
        if (!controller.prepareEngine()) return

        isStarted = true
        controller.activateEngine()
    }

    /**
     * Kakao Maps SDK 엔진 준비 후 기본 지도 뷰를 추가한다.
     */
    override fun addViews() {
        controller.addView(createDefaultMapViewInfo())
    }

    /**
     * 기본 시작 위치와 줌 레벨을 가진 지도 뷰 정보를 만든다.
     */
    private fun createDefaultMapViewInfo(): MapviewInfo =
        MapviewInfo.create(
            mapViewName,
            DEFAULT_APP_NAME,
            DEFAULT_VIEW_INFO_NAME,
            defaultMapPoint(),
            DefaultMapConfig.ZOOM_LEVEL.toLong(),
            true,
        )

    /**
     * 앱 최초 진입 시 사용할 기본 지도 중심 좌표를 만든다.
     */
    private fun defaultMapPoint(): MapPoint =
        MapPoint(
            longitude = DefaultMapConfig.location.lng,
            latitude = DefaultMapConfig.location.lat,
        )

    /**
     * 지도 뷰 추가 완료 후 이벤트 delegate를 연결하고 지연된 마커 렌더링을 실행한다.
     */
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

    /**
     * 사용자의 이동이나 프로그램 이동으로 카메라가 멈췄을 때 현재 화면 영역을 알린다.
     */
    override fun cameraDidStoppedWithKakaoMap(
        kakaoMap: KakaoMap,
        by: MoveBy,
    ) {
        notifyCurrentBounds(kakaoMap)
    }

    /**
     * 현재 지도 화면의 네 모서리를 위경도로 변환해 ViewModel에 전달할 bounds를 계산한다.
     */
    private fun notifyCurrentBounds(kakaoMap: KakaoMap) {
        val width = mapViewContainer.bounds.useContents { size.width }
        val height = mapViewContainer.bounds.useContents { size.height }
        if (width <= 0.0 || height <= 0.0) return

        onBoundsChanged(kakaoMap.visibleBounds(width, height))
    }

    /**
     * 현재 지도 화면의 네 모서리 좌표를 포함하는 bounds를 만든다.
     */
    private fun KakaoMap.visibleBounds(
        width: Double,
        height: Double,
    ): MapBounds {
        val points =
            listOf(
                coordinateAt(CGPointMake(0.0, 0.0)),
                coordinateAt(CGPointMake(width, 0.0)),
                coordinateAt(CGPointMake(0.0, height)),
                coordinateAt(CGPointMake(width, height)),
            )

        return points.toMapBounds()
    }

    /**
     * 화면 탭 좌표를 위경도 좌표로 변환한다.
     */
    private fun KakaoMap.coordinateAt(point: CValue<CGPoint>): IosMapCoordinate =
        getPosition(point)
            .wgsCoord
            .useContents {
                IosMapCoordinate(
                    latitude = latitude,
                    longitude = longitude,
                )
            }

    /**
     * 좌표 목록의 최소/최대 위경도로 [MapBounds]를 만든다.
     */
    private fun List<IosMapCoordinate>.toMapBounds(): MapBounds =
        MapBounds(
            minLat = minOf { it.latitude },
            maxLat = maxOf { it.latitude },
            minLng = minOf { it.longitude },
            maxLng = maxOf { it.longitude },
        )

    /**
     * 현재 지도에 표시해야 할 매장 목록을 갱신한다.
     *
     * 지도 뷰가 아직 추가되지 않은 경우에는 [pendingShops]에 보관했다가 [addViewSucceeded]
     * 이후 마커를 렌더링한다.
     */
    fun updateShops(shops: RamenShops) {
        pendingShops = shops
        renderRamenShopMarkers(shops)
    }

    /**
     * 현재 매장 목록과 기존 마커 상태를 비교해 iOS 지도 마커를 동기화한다.
     *
     * 지도와 레이어가 준비되면 사라진 매장 마커를 먼저 제거하고
     * 아직 렌더링되지 않은 매장만 새 POI로 추가한다.
     */
    private fun renderRamenShopMarkers(shops: RamenShops) {
        if (!isMapViewAdded) return

        val kakaoMap = getKakaoMap() ?: return
        val layer = prepareMarkerLayer(kakaoMap) ?: return

        removeStaleMarkers(
            layer = layer,
            currentShopIds = shops.keys,
        )
        removeChangedMarkers(
            layer = layer,
            shops = shops.values.toList(),
        )
        renderNewMarkers(
            layer = layer,
            shops = shops.values.toList(),
        )
    }

    /**
     * 마커 스타일을 등록하고 라벨 레이어를 가져오거나 새로 만든다.
     */
    private fun prepareMarkerLayer(kakaoMap: KakaoMap): LabelLayer? {
        val labelManager = kakaoMap.getLabelManager()
        ensureMarkerStyles(labelManager)

        return (
            labelManager.getLabelLayerWithLayerID(markerLayerId)
                ?: labelManager.addLabelLayerWithOption(createMarkerLayerOptions())
        )?.apply {
            visible = true
            setClickable(true)
        }
    }

    /**
     * 라멘 매장 마커 아이콘과 텍스트 스타일을 Kakao Maps SDK label manager에 등록한다.
     */
    private fun ensureMarkerStyles(labelManager: LabelManager) {
        addMarkerStyle(
            labelManager = labelManager,
            styleId = MarkerConfig.Single.STYLE_ID,
            alpha = VISIBLE_MARKER_ALPHA,
        )
        addMarkerStyle(
            labelManager = labelManager,
            styleId = MarkerConfig.Single.HIDDEN_STYLE_ID,
            alpha = MapInteractionConfig.HIDDEN_SHOP_ALPHA.toDouble(),
        )
    }

    private fun addMarkerStyle(
        labelManager: LabelManager,
        styleId: String,
        alpha: Double,
    ) {
        val poiStyle =
            PoiStyle(
                styleId,
                listOf(
                    PerLevelPoiStyle(
                        createMarkerIconStyle(alpha) ?: return,
                        createMarkerTextStyle(alpha),
                        0.0f,
                        0,
                    ),
                ),
            )

        labelManager.addPoiStyle(poiStyle)
    }

    /**
     * 마커 이미지 리소스로 POI 아이콘 스타일을 만든다.
     */
    private fun createMarkerIconStyle(alpha: Double): PoiIconStyle? {
        val image = UIImage.imageNamed(MARKER_IMAGE_NAME) ?: return null

        return PoiIconStyle(
            image.withAlpha(alpha),
            CGPointMake(0.5, 1.0),
            poiTransition(),
            true,
            true,
            null,
        )
    }

    /**
     * 마커와 라벨의 등장/퇴장 애니메이션을 비활성화한 전환 값을 만든다.
     */
    private fun poiTransition(): CValue<PoiTransition> =
        cValue {
            entrance = TransitionTypeNone
            exit = TransitionTypeNone
        }

    /**
     * 마커 라벨 텍스트 스타일을 만든다.
     */
    private fun createMarkerTextStyle(alpha: Double): PoiTextStyle =
        PoiTextStyle(
            poiTransition(),
            true,
            true,
            listOf(createMarkerTextLineStyle(alpha)),
        )

    /**
     * 마커 라벨 한 줄에 적용할 글자 크기, 색상, 외곽선 스타일을 만든다.
     */
    private fun createMarkerTextLineStyle(alpha: Double): PoiTextLineStyle =
        PoiTextLineStyle()
            .apply {
                textStyle =
                    TextStyle(
                        MarkerConfig.Single.LABEL_TEXT_SIZE.toULong(),
                        markerTextColor(alpha),
                        MarkerConfig.Single.LABEL_STROKE_WIDTH.toULong(),
                        UIColor.whiteColor,
                        "",
                        0,
                        1.0f,
                        1.0f,
                    )
            }

    /**
     * 매장 마커 라벨에 사용할 텍스트 색상을 UIKit 색상으로 만든다.
     */
    private fun markerTextColor(alpha: Double): UIColor =
        UIColor.colorWithRed(
            red = MarkerConfig.Single.LABEL_TEXT_COLOR.redComponent(),
            green = MarkerConfig.Single.LABEL_TEXT_COLOR.greenComponent(),
            blue = MarkerConfig.Single.LABEL_TEXT_COLOR.blueComponent(),
            alpha = MarkerConfig.Single.LABEL_TEXT_COLOR.alphaComponent() * alpha,
        )

    /**
     * 매장 마커 전용 label layer 생성 옵션을 만든다.
     */
    private fun createMarkerLayerOptions(): LabelLayerOptions =
        LabelLayerOptions(
            markerLayerId,
            CompetitionTypeNone,
            CompetitionUnitPoi,
            OrderingTypeRank,
            MARKER_LAYER_Z_ORDER,
        )

    /**
     * 매장 id를 Kakao Maps SDK POI id 네임스페이스로 변환한다.
     */
    private fun String.toMarkerPoiId(): String = "ramen-shop-$this"

    /**
     * 현재 매장 목록에 더 이상 포함되지 않는 기존 POI 마커를 제거한다.
     */
    private fun removeStaleMarkers(
        layer: LabelLayer,
        currentShopIds: Set<String>,
    ) {
        val staleShopIds = renderedShopIds - currentShopIds
        if (staleShopIds.isEmpty()) return

        val stalePoiIds = staleShopIds.map { shopId -> shopId.toMarkerPoiId() }
        layer.removePoisWithPoiIDs(stalePoiIds, callback = null)
        stalePoiIds.forEach(shopsByPoiId::remove)
        renderedShopIds.removeAll(staleShopIds)
    }

    private fun removeChangedMarkers(
        layer: LabelLayer,
        shops: List<RamenShop>,
    ) {
        val changedShopIds =
            shops
                .filter { shop ->
                    val renderedShop = shopsByPoiId[shop.id.toMarkerPoiId()]
                    renderedShop != null && renderedShop != shop
                }.map { shop -> shop.id }

        if (changedShopIds.isEmpty()) return

        val changedPoiIds = changedShopIds.map { shopId -> shopId.toMarkerPoiId() }
        layer.removePoisWithPoiIDs(changedPoiIds, callback = null)
        changedPoiIds.forEach(shopsByPoiId::remove)
        renderedShopIds.removeAll(changedShopIds)
    }

    /**
     * 아직 지도에 추가되지 않은 매장을 새 POI 마커로 렌더링한다.
     */
    private fun renderNewMarkers(
        layer: LabelLayer,
        shops: List<RamenShop>,
    ) {
        val newShops = shops.filter { shop -> shop.id !in renderedShopIds }
        if (newShops.isEmpty()) return

        newShops.forEach { shop -> addMarkerPoi(layer, shop) }
    }

    /**
     * 매장 하나를 Kakao Maps SDK POI로 추가하고 탭 처리를 위한 매핑을 저장한다.
     */
    private fun addMarkerPoi(
        layer: LabelLayer,
        shop: RamenShop,
    ) {
        val poiId = shop.id.toMarkerPoiId()
        val poi =
            layer.addPoiWithOption(
                option = createMarkerPoiOptions(shop, poiId),
                at = shop.toMapPoint(),
                callback = null,
            )

        poi?.show()
        poi?.clickable = true

        shopsByPoiId[poiId] = shop
        renderedShopIds += shop.id
    }

    /**
     * 매장 마커 POI에 사용할 옵션과 라벨 텍스트를 만든다.
     */
    private fun createMarkerPoiOptions(
        shop: RamenShop,
        poiId: String,
    ): PoiOptions =
        PoiOptions(
            styleID = if (shop.isVisible) MarkerConfig.Single.STYLE_ID else MarkerConfig.Single.HIDDEN_STYLE_ID,
            poiID = poiId,
        ).apply {
            clickable = true
            addText(
                PoiText(
                    text = shop.name,
                    styleIndex = 0u,
                ),
            )
        }

    /**
     * 검색 결과 또는 선택 매장을 지도 카메라의 포커스 대상으로 반영한다.
     *
     * 빈 목록과 동일한 포커스 요청은 무시한다. 대상이 하나면 해당 매장 중심으로 이동하고,
     * 둘 이상이면 모든 대상이 보이도록 Kakao Maps SDK의 영역 맞춤 카메라 업데이트를 적용한다.
     */
    fun updateFocusShops(
        shops: List<RamenShop>,
        focusNearestToCurrentLocation: Boolean,
    ) {
        if (!isMapViewAdded) return

        if (!shouldUpdateFocus(shops, focusNearestToCurrentLocation)) return

        val kakaoMap = getKakaoMap() ?: return
        lastFocusKey = shops.focusKey(focusNearestToCurrentLocation)
        focusShops(
            kakaoMap = kakaoMap,
            shops = shops,
            focusNearestToCurrentLocation = focusNearestToCurrentLocation,
        )
    }

    /**
     * 위치 권한 상태를 확인한 뒤 권한이 있으면 현재 위치로 이동하고, 없으면 iOS 권한 요청을 시작한다.
     */
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

    /**
     * 권한 허용 이후 마지막 위치가 있으면 즉시 이동하고, 없으면 단발 위치 조회를 요청한다.
     */
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

    private fun CLLocationManager.isAuthorized(): Boolean = authorizationStatus.isAuthorized()

    private fun CLLocationManager.isBlocked(): Boolean = authorizationStatus.isBlocked()

    private fun CLAuthorizationStatus.isAuthorized(): Boolean =
        this == kCLAuthorizationStatusAuthorizedWhenInUse ||
            this == kCLAuthorizationStatusAuthorizedAlways

    private fun CLAuthorizationStatus.isBlocked(): Boolean =
        this == kCLAuthorizationStatusDenied ||
            this == kCLAuthorizationStatusRestricted

    /**
     * CoreLocation 좌표를 Kakao Maps SDK 좌표로 변환해 지도 카메라를 이동한다.
     */
    private fun moveToLocation(location: CLLocation) {
        onMyLocationChanged(location.toDomainLocation())
        location.coordinate.useContents {
            moveToCoordinate(
                IosMapCoordinate(
                    latitude = latitude,
                    longitude = longitude,
                ),
            )
        }
    }

    private fun moveToCoordinate(coordinate: IosMapCoordinate) {
        val kakaoMap = getKakaoMap()

        if (kakaoMap == null || !isMapViewAdded) {
            pendingMyLocationCoordinate = coordinate
            return
        }

        renderMyLocationMarker(kakaoMap, coordinate)
        moveCamera(
            kakaoMap = kakaoMap,
            cameraUpdate =
                CameraUpdate.makeWithTarget(
                    target =
                        MapPoint(
                            longitude = coordinate.longitude,
                            latitude = coordinate.latitude,
                        ),
                    mapView = kakaoMap,
                ),
        )
    }

    /**
     * 현재 위치를 매장 마커와 분리된 POI로 표시한다.
     */
    private fun renderMyLocationMarker(
        kakaoMap: KakaoMap,
        coordinate: IosMapCoordinate,
    ) {
        val labelManager = kakaoMap.getLabelManager()
        val layer = prepareMyLocationLayer(labelManager) ?: return

        layer.removePoisWithPoiIDs(listOf(MY_LOCATION_POI_ID), callback = null)
        layer
            .addPoiWithOption(
                option =
                    PoiOptions(
                        styleID = MY_LOCATION_STYLE_ID,
                        poiID = MY_LOCATION_POI_ID,
                    ).apply {
                        clickable = false
                    },
                at =
                    MapPoint(
                        longitude = coordinate.longitude,
                        latitude = coordinate.latitude,
                    ),
                callback = null,
            )?.show()
    }

    private fun prepareMyLocationLayer(labelManager: LabelManager): LabelLayer? {
        ensureMyLocationStyle(labelManager)

        return (
            labelManager.getLabelLayerWithLayerID(myLocationLayerId)
                ?: labelManager.addLabelLayerWithOption(createMyLocationLayerOptions())
        )?.apply {
            visible = true
            setClickable(false)
        }
    }

    private fun ensureMyLocationStyle(labelManager: LabelManager) {
        val poiStyle =
            PoiStyle(
                MY_LOCATION_STYLE_ID,
                listOf(
                    PerLevelPoiStyle(
                        createMyLocationIconStyle() ?: return,
                        0.0f,
                        0,
                    ),
                ),
            )

        labelManager.addPoiStyle(poiStyle)
    }

    private fun createMyLocationIconStyle(): PoiIconStyle? =
        createMyLocationImage()?.let { image ->
            PoiIconStyle(
                image,
                CGPointMake(0.5, 0.5),
                poiTransition(),
                true,
                true,
                null,
            )
        }

    private fun createMyLocationImage(): UIImage? {
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(MY_LOCATION_IMAGE_SIZE, MY_LOCATION_IMAGE_SIZE),
            false,
            0.0,
        )
        UIColor.whiteColor.setFill()
        UIBezierPath
            .bezierPathWithOvalInRect(
                CGRectMake(
                    MY_LOCATION_OUTER_INSET,
                    MY_LOCATION_OUTER_INSET,
                    MY_LOCATION_OUTER_SIZE,
                    MY_LOCATION_OUTER_SIZE,
                ),
            ).fill()
        UIColor.colorWithRed(0.184, 0.502, 0.929, 1.0).setFill()
        UIBezierPath
            .bezierPathWithOvalInRect(
                CGRectMake(
                    MY_LOCATION_INNER_INSET,
                    MY_LOCATION_INNER_INSET,
                    MY_LOCATION_INNER_SIZE,
                    MY_LOCATION_INNER_SIZE,
                ),
            ).fill()
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return image
    }

    private fun createMyLocationLayerOptions(): LabelLayerOptions =
        LabelLayerOptions(
            myLocationLayerId,
            CompetitionTypeNone,
            CompetitionUnitPoi,
            OrderingTypeRank,
            MY_LOCATION_LAYER_Z_ORDER,
        )

    /**
     * 전달된 매장 목록이 새 카메라 포커스 요청인지 확인한다.
     */
    private fun shouldUpdateFocus(
        shops: List<RamenShop>,
        focusNearestToCurrentLocation: Boolean,
    ): Boolean {
        val focusKey = shops.focusKey(focusNearestToCurrentLocation)

        return focusKey.isNotBlank() && lastFocusKey != focusKey
    }

    /**
     * 동일한 포커스 요청을 식별하기 위한 안정적인 key를 만든다.
     */
    private fun List<RamenShop>.focusKey(focusNearestToCurrentLocation: Boolean): String {
        if (isEmpty()) return EMPTY_FOCUS_KEY

        return joinToString(separator = "|") { shop ->
            "${shop.id}:${shop.location.lat}:${shop.location.lng}"
        } + ":$focusNearestToCurrentLocation"
    }

    /**
     * 매장 수에 따라 단일 중심 이동 또는 다중 영역 맞춤 이동을 선택한다.
     */
    private fun focusShops(
        kakaoMap: KakaoMap,
        shops: List<RamenShop>,
        focusNearestToCurrentLocation: Boolean,
    ) {
        when (shops.size) {
            1 -> moveToShop(kakaoMap, shops.first())
            else -> {
                val nearestShop =
                    if (focusNearestToCurrentLocation) {
                        shops.nearestTo(locationManager.location?.toDomainLocation())
                    } else {
                        null
                    }

                if (nearestShop != null) {
                    moveToShop(kakaoMap, nearestShop)
                } else {
                    fitShops(kakaoMap, shops)
                }
            }
        }
    }

    /**
     * 단일 매장을 지도 중심으로 이동한다.
     */
    private fun moveToShop(
        kakaoMap: KakaoMap,
        shop: RamenShop,
    ) {
        val target = shop.toMapPoint()
        moveCamera(
            kakaoMap = kakaoMap,
            cameraUpdate =
                if (kakaoMap.zoomLevel < MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL) {
                    CameraUpdate.makeWithTarget(
                        target = target,
                        zoomLevel = MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL.toLong(),
                        mapView = kakaoMap,
                    )
                } else {
                    CameraUpdate.makeWithTarget(
                        target = target,
                        mapView = kakaoMap,
                    )
                },
        )
    }

    /**
     * 도메인 매장 좌표를 Kakao Maps SDK의 [MapPoint]로 변환한다.
     */
    private fun RamenShop.toMapPoint(): MapPoint =
        MapPoint(
            longitude = location.lng,
            latitude = location.lat,
        )

    /**
     * 여러 매장이 모두 보이도록 지도 카메라 영역을 맞춘다.
     */
    private fun fitShops(
        kakaoMap: KakaoMap,
        shops: List<RamenShop>,
    ) {
        moveCamera(
            kakaoMap = kakaoMap,
            cameraUpdate =
                CameraUpdate.makeWithArea(
                    area = AreaRect(points = shops.map { shop -> shop.toMapPoint() }),
                    levelLimit = -1,
                ),
        )
    }

    /**
     * Kakao Maps SDK 카메라 업데이트를 콜백 없이 적용한다.
     */
    private fun moveCamera(
        kakaoMap: KakaoMap,
        cameraUpdate: CameraUpdate,
    ) {
        kakaoMap.moveCamera(
            cameraUpdate,
            callback = null,
        )
    }

    /**
     * Kakao Maps SDK의 POI 탭 콜백을 공통 매장 선택 콜백으로 변환한다.
     */
    override fun poiDidTappedWithKakaoMap(
        kakaoMap: KakaoMap,
        layerID: String,
        poiID: String,
        position: MapPoint,
    ) {
        val shop = shopsByPoiId[poiID]
        shop?.let {
            moveToShop(kakaoMap, it)
            onShopClick(it)
        }
    }

    /**
     * UIKit 단일 탭 위치에서 가장 가까운 매장 마커를 찾아 선택한다.
     *
     * iOS interop 환경에서 POI 탭 콜백이 누락될 수 있어, 지도 좌표로 변환한 탭 위치와
     * 렌더링된 매장 좌표의 거리를 비교해 보조 선택 경로를 제공한다.
     */
    private fun selectNearestShopAt(point: CValue<CGPoint>) {
        val kakaoMap = getKakaoMap() ?: return
        val tappedCoordinate = kakaoMap.coordinateAt(point)
        val shop = findNearestShop(tappedCoordinate) ?: return

        moveToShop(kakaoMap, shop)
        onShopClick(shop)
    }

    /**
     * 현재 컨트롤러에 등록된 Kakao 지도 뷰를 가져온다.
     */
    private fun getKakaoMap(): KakaoMap? = controller.getView(mapViewName) as? KakaoMap

    private fun UIImage.withAlpha(alpha: Double): UIImage {
        UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
        drawAtPoint(
            point = CGPointMake(0.0, 0.0),
            blendMode = CGBlendMode.kCGBlendModeNormal,
            alpha = alpha,
        )
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return image ?: this
    }

    private fun CLLocation.toDomainLocation(): Location =
        coordinate.useContents {
            Location(
                lat = latitude,
                lng = longitude,
            )
        }

    /**
     * 탭 좌표에서 선택 가능한 거리 안에 있는 가장 가까운 매장을 찾는다.
     */
    private fun findNearestShop(tappedCoordinate: IosMapCoordinate): RamenShop? =
        shopsByPoiId
            .values
            .map { shop ->
                shop to
                    tappedCoordinate.distanceTo(
                        shop.location.lat,
                        shop.location.lng,
                    )
            }.minByOrNull { (_, distance) -> distance }
            ?.takeIf { (_, distance) -> distance <= MARKER_TAP_RADIUS_METERS }
            ?.first

    /**
     * 지도 엔진과 delegate 연결을 정리한다.
     *
     * Compose에서 `UIKitView`가 dispose될 때 호출되어 SDK 리소스가 화면 생명주기 밖에
     * 남지 않도록 한다.
     */
    fun dispose() {
        controller.pauseEngine()
        controller.resetEngine()
        controller.delegate = null
        isStarted = false
        isMapViewAdded = false
    }
}

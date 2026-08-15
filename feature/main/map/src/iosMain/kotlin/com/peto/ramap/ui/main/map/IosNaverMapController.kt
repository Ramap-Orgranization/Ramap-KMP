@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.peto.ramap.ui.main.map

import cocoapods.NMapsMap.NMCBuilder
import cocoapods.NMapsMap.NMFCameraUpdate
import cocoapods.NMapsMap.NMFLocationManager
import cocoapods.NMapsMap.NMFLocationManagerDelegateProtocol
import cocoapods.NMapsMap.NMFMapView
import cocoapods.NMapsMap.NMFMapViewCameraDelegateProtocol
import cocoapods.NMapsMap.NMFMapViewLoadDelegateProtocol
import cocoapods.NMapsMap.NMFMyPositionDirection
import cocoapods.NMapsMap.NMFMyPositionNormal
import cocoapods.NMapsMap.NMFNaverMapView
import cocoapods.NMapsMap.NMGLatLng
import cocoapods.NMapsMap.NMGLatLngBounds
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.main.ShopLeafMarkerUpdater
import com.peto.ramap.ui.main.map.config.CurrentLocationConfig
import com.peto.ramap.ui.main.map.config.DefaultMapConfig
import com.peto.ramap.ui.main.map.config.MapInteractionConfig
import com.peto.ramap.ui.main.map.model.CameraPosition
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectZero
import platform.CoreLocation.CLLocation
import platform.UIKit.UIEdgeInsetsMake
import platform.darwin.NSObject

private const val FOCUS_PADDING = 120.0

internal class IosNaverMapController(
    private val onMapMoveStarted: () -> Unit,
    private val onBoundsChanged: (MapBounds) -> Unit,
    private val onCameraPositionChanged: (CameraPosition) -> Unit,
    private val onShopClick: (RamenShop) -> Unit,
    private val onMyLocationChanged: (Location) -> Unit,
    private val onCurrentLocationFocused: () -> Unit,
) : NSObject(),
    NMFMapViewCameraDelegateProtocol,
    NMFMapViewLoadDelegateProtocol,
    NMFLocationManagerDelegateProtocol {
    val view = NMFNaverMapView(frame = CGRectZero.readValue())
    val mapView = view.mapView
    var viewportHeight: Int = 0
    private val leafUpdater = ShopLeafMarkerUpdater(onShopClick)
    private var clusterer = newClusterer()
    private var lastShopsKey = ""
    private var lastFocusKey = ""
    private var hasFocusedCurrentLocation = false
    private var hasRestoredCameraPosition = false
    private var currentLocation: Location? = null
    private var shouldMoveToCurrentLocation = false
    private var isCameraMoving = false
    private var shopKeys = emptyList<ShopClusteringKey>()
    private var hasMapLoaded = false
    private var isRenderingShops = false
    private var isDisposed = false
    private var shopRenderGeneration = 0L
    private var lastRenderedShopGeneration = -1L
    private var lastNotifiedBounds: MapBounds? = null
    private var lastNotifiedCameraPosition: CameraPosition? = null
    private val locationManager = NMFLocationManager.sharedInstance()

    init {
        view.showCompass = false
        view.showScaleBar = false
        view.showZoomControls = false
        view.showLocationButton = false
        mapView.minZoomLevel = MapInteractionConfig.MAX_ZOOM_OUT_LEVEL.toDouble()
        mapView.addCameraDelegate(this)
        mapView.addLoadDelegate(this)
        locationManager?.addDelegate(this)
        moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(DefaultMapConfig.LATITUDE, DefaultMapConfig.LONGITUDE),
                zoomTo = DefaultMapConfig.ZOOM_LEVEL.toDouble(),
            ),
        )
    }

    fun updateShops(shops: RamenShops) {
        val key = shops.values.joinToString("|") { shopRenderKey(it) }
        if (key == lastShopsKey) {
            renderPendingShopKeys()
            return
        }
        lastShopsKey = key
        shopKeys = shops.values.map(::ShopClusteringKey)
        shopRenderGeneration += 1
        renderPendingShopKeys()
    }

    fun restoreCameraPosition(position: CameraPosition?) {
        if (position == null || hasRestoredCameraPosition) return
        hasRestoredCameraPosition = true
        moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(position.center.lat, position.center.lng),
                zoomTo = position.zoom,
            ),
        )
    }

    fun updateFocus(
        shops: RamenShops,
        focusNearestToCurrentLocation: Boolean,
        focusRequestKey: Long,
        selectedShopId: String?,
    ): Boolean {
        mapView.contentInset =
            UIEdgeInsetsMake(
                top = 0.0,
                left = 0.0,
                bottom =
                    if (selectedShopId != null && shops.size == 1) {
                        viewportHeight * MapInteractionConfig.SELECTED_MARKER_BOTTOM_PADDING_RATIO.toDouble()
                    } else {
                        0.0
                    },
                right = 0.0,
            )
        val location = currentLocation.takeIf { focusNearestToCurrentLocation }
        val key =
            shops.values.joinToString("|") { "${it.id}:${it.location.lat}:${it.location.lng}" } +
                ":${location != null}:$focusRequestKey"
        if (shops.isEmpty()) return false
        if (key == lastFocusKey) return true
        lastFocusKey = key
        mapView.positionMode = NMFMyPositionNormal

        val nearest = shops.nearestTo(location)
        when {
            shops.size == 1 -> moveToShop(shops.values.first())
            nearest != null -> moveToShop(nearest)
            else -> {
                val points = shops.values.map { NMGLatLng.latLngWithLat(it.location.lat, it.location.lng) }
                val bounds = NMGLatLngBounds.latLngBoundsWithLatLngs(points)
                moveCamera(NMFCameraUpdate.cameraUpdateWithFitBounds(bounds, FOCUS_PADDING))
            }
        }
        return true
    }

    fun updateInitialLocationFocus(location: Location?) {
        if (location == null || hasFocusedCurrentLocation) return
        hasFocusedCurrentLocation = true
        moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(location.lat, location.lng),
            ),
        )
        mapView.positionMode = NMFMyPositionNormal
    }

    fun bootstrapInitialLocationFocus() {
        mapView.positionMode = NMFMyPositionDirection
    }

    fun requestCurrentLocation() {
        shouldMoveToCurrentLocation = true
        mapView.positionMode = NMFMyPositionDirection
        currentLocation?.let(::moveToCurrentLocation)
    }

    fun cancelCurrentLocationRequest() {
        shouldMoveToCurrentLocation = false
    }

    override fun locationManager(
        locationManager: NMFLocationManager?,
        didUpdateLocations: List<*>?,
    ) {
        val location = didUpdateLocations?.lastOrNull() as? CLLocation ?: return
        Location(
            lat = location.coordinate.useContents { latitude },
            lng = location.coordinate.useContents { longitude },
        ).let { current ->
            currentLocation = current
            onMyLocationChanged(current)
            if (shouldMoveToCurrentLocation) {
                moveToCurrentLocation(current)
            }
        }
    }

    override fun mapViewCameraIdle(mapView: NMFMapView) {
        isCameraMoving = false
        renderPendingShopKeys()
        notifyViewportIfReady()
    }

    override fun mapView(
        mapView: NMFMapView,
        cameraIsChangingByReason: Long,
    ) {
        if (isCameraMoving) return
        isCameraMoving = true
        onMapMoveStarted()
    }

    override fun mapViewDidFinishLoadingMap(mapView: NMFMapView) {
        hasMapLoaded = true
        renderPendingShopKeys()
        notifyViewportIfReady()
    }

    fun dispose() {
        isDisposed = true
        shopRenderGeneration += 1
        isRenderingShops = false
        clusterer.mapView = null
        clusterer.clear()
        shopKeys = emptyList()
        mapView.removeCameraDelegate(this)
        mapView.removeLoadDelegate(this)
        locationManager?.removeDelegate(this)
    }

    private fun moveToShop(shop: RamenShop) {
        moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(shop.location.lat, shop.location.lng),
                zoomTo = MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL.toDouble(),
            ),
        )
    }

    private fun moveToCurrentLocation(location: Location) {
        shouldMoveToCurrentLocation = false
        moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(location.lat, location.lng),
                zoomTo = CurrentLocationConfig.zoomForCurrentLocation(mapView.cameraPosition.zoom),
            ),
        )
        mapView.positionMode = NMFMyPositionNormal
        onCurrentLocationFocused()
    }

    private fun renderPendingShopKeys() {
        if (
            !canRenderShopKeys() ||
            isRenderingShops ||
            shopRenderGeneration == lastRenderedShopGeneration
        ) {
            return
        }

        val renderGeneration = shopRenderGeneration
        val keysForRender = shopKeys
        isRenderingShops = true

        // NMCClusterer can retain its previous camera index when data is replaced in place.
        // Populate a fresh instance before attachment so it renders against the current viewport.
        val replacementClusterer = newClusterer()
        val keyTagMap: Map<Any?, Any> = keysForRender.associate { key -> key to ShopMarkerTag(key.shop) }
        if (keyTagMap.isNotEmpty()) {
            replacementClusterer.addAll(keyTagMap)
        }
        if (!isCurrentShopRender(renderGeneration)) {
            replacementClusterer.clear()
            isRenderingShops = false
            renderPendingShopKeys()
            return
        }

        clusterer.mapView = null
        clusterer.clear()
        clusterer = replacementClusterer
        clusterer.mapView = mapView
        lastRenderedShopGeneration = renderGeneration
        isRenderingShops = false
    }

    private fun newClusterer() =
        NMCBuilder()
            .apply {
                maxZoom = MapInteractionConfig.CLUSTER_MAX_ZOOM_LEVEL.toLong()
                animate = false
                leafMarkerUpdater = leafUpdater
            }.build()

    private fun canRenderShopKeys(): Boolean =
        !isDisposed &&
            hasMapLoaded &&
            mapView.contentWidth > 0.0 &&
            mapView.contentHeight > 0.0

    private fun isCurrentShopRender(renderGeneration: Long): Boolean = canRenderShopKeys() && renderGeneration == shopRenderGeneration

    private fun moveCamera(cameraUpdate: NMFCameraUpdate) {
        mapView.moveCamera(cameraUpdate) { isCancelled ->
            if (!isCancelled) {
                notifyViewportIfReady()
            }
        }
    }

    private fun notifyViewportIfReady() {
        if (!canNotifyViewport()) return

        val bounds = currentMapBounds()
        val cameraPosition = currentCameraPosition()
        if (bounds == lastNotifiedBounds && cameraPosition == lastNotifiedCameraPosition) return

        lastNotifiedBounds = bounds
        lastNotifiedCameraPosition = cameraPosition
        onBoundsChanged(bounds)
        onCameraPositionChanged(cameraPosition)
    }

    private fun canNotifyViewport(): Boolean =
        !isDisposed &&
            hasMapLoaded &&
            mapView.contentWidth > 0.0 &&
            mapView.contentHeight > 0.0

    private fun currentMapBounds(): MapBounds {
        val bounds = mapView.contentBounds
        return MapBounds(
            minLat = bounds.southWestLat(),
            maxLat = bounds.northEastLat(),
            minLng = bounds.southWestLng(),
            maxLng = bounds.northEastLng(),
        )
    }

    private fun currentCameraPosition(): CameraPosition {
        val cameraPosition = mapView.cameraPosition
        return CameraPosition(
            center =
                Location(
                    lat = cameraPosition.target.lat(),
                    lng = cameraPosition.target.lng(),
                ),
            zoom = cameraPosition.zoom,
        )
    }

    private fun shopRenderKey(shop: RamenShop): String =
        "${shop.id}:${shop.location.lat}:${shop.location.lng}:${shop.name}:${shop.isVisible}"
}

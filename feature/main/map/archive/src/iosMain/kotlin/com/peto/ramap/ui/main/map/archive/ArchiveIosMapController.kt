@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.main.map.archive.model.ArchiveCurrentLocationConfig
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapInteractionConfig
import com.peto.ramap.ui.main.map.archive.model.CameraPosition
import com.peto.ramap.ui.main.map.archive.model.InitialMapRenderState
import com.peto.ramap.ui.main.map.archive.model.MapClusteringOptions
import com.peto.ramap.ui.main.map.archive.model.MapMarkerImage
import com.peto.ramap.ui.main.map.archive.model.overlappingMarkerShops
import com.peto.ramap.ui.main.map.archive.nmaps.NMCComplexBuilder
import com.peto.ramap.ui.main.map.archive.nmaps.NMCDefaultThresholdStrategy
import com.peto.ramap.ui.main.map.archive.nmaps.NMFCameraUpdate
import com.peto.ramap.ui.main.map.archive.nmaps.NMFLocationManager
import com.peto.ramap.ui.main.map.archive.nmaps.NMFLocationManagerDelegateProtocol
import com.peto.ramap.ui.main.map.archive.nmaps.NMFMapView
import com.peto.ramap.ui.main.map.archive.nmaps.NMFMapViewCameraDelegateProtocol
import com.peto.ramap.ui.main.map.archive.nmaps.NMFMapViewLoadDelegateProtocol
import com.peto.ramap.ui.main.map.archive.nmaps.NMFMapViewRenderDelegateProtocol
import com.peto.ramap.ui.main.map.archive.nmaps.NMFMyPositionDirection
import com.peto.ramap.ui.main.map.archive.nmaps.NMFMyPositionNormal
import com.peto.ramap.ui.main.map.archive.nmaps.NMFNaverMapView
import com.peto.ramap.ui.main.map.archive.nmaps.NMGLatLng
import com.peto.ramap.ui.main.map.archive.nmaps.NMGLatLngBounds
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectZero
import platform.CoreLocation.CLLocation
import platform.UIKit.UIEdgeInsetsMake
import platform.darwin.NSObject

private const val FOCUS_PADDING = 120.0

internal class ArchiveIosMapController(
    private val clusteringOptions: MapClusteringOptions,
    private val onMapMoveStarted: () -> Unit,
    private val onCameraPositionChanged: (CameraPosition) -> Unit,
    private val onShopClick: (RamenShop) -> Unit,
    private val onClusterClick: (List<RamenShop>, Double, Double) -> Unit,
    private val onCurrentLocationFocused: () -> Unit,
    private val onMapReady: () -> Unit,
) : NSObject(),
    NMFMapViewCameraDelegateProtocol,
    NMFMapViewLoadDelegateProtocol,
    NMFMapViewRenderDelegateProtocol,
    NMFLocationManagerDelegateProtocol {
    val view = NMFNaverMapView(frame = CGRectZero.readValue())
    val mapView = view.mapView
    private var viewportHeightPixels = 0
    private var viewportDensity = 1f
    private val leafUpdater = ArchiveShopLeafMarkerUpdater(::onLeafMarkerClick)
    private val clusterUpdater =
        ArchiveShopClusterMarkerUpdater { shops, position ->
            mapView.projection.pointFromLatLng(position).useContents {
                onClusterClick(shops, x, y)
            }
        }
    private var clusterer = newClusterer()
    private var lastShopsKey = ""
    private var lastFocusKey = ""
    private var hasRestoredCameraPosition = false
    private var currentLocation: Location? = null
    private var shouldMoveToCurrentLocation = false
    private var isCameraMoving = false
    private var shopKeys = emptyList<ArchiveShopClusteringKey>()
    private var hasMapLoaded = false
    private var isRenderingShops = false
    private var isDisposed = false
    private var shopRenderGeneration = 0L
    private var lastRenderedShopGeneration = -1L
    private var lastNotifiedCameraPosition: CameraPosition? = null
    private val initialRenderState = InitialMapRenderState()
    private val locationManager = NMFLocationManager.sharedInstance()

    init {
        view.showCompass = false
        view.showScaleBar = false
        view.showZoomControls = false
        view.showLocationButton = false
        mapView.minZoomLevel = ArchiveMapInteractionConfig.MAX_ZOOM_OUT_LEVEL.toDouble()
        mapView.maxZoomLevel = ArchiveMapInteractionConfig.MAP_MAX_ZOOM_LEVEL.toDouble()
        mapView.addCameraDelegate(this)
        mapView.addLoadDelegate(this)
        mapView.addRenderDelegate(this)
        locationManager?.addDelegate(this)
    }

    fun updateViewportHeight(
        heightPixels: Int,
        density: Float,
    ) {
        viewportHeightPixels = heightPixels
        viewportDensity = density
    }

    fun updateShops(
        shops: RamenShops,
        markerImages: Map<String, MapMarkerImage>,
    ) {
        leafUpdater.updateImages(markerImages)
        val key = shops.values.joinToString("|") { shopRenderKey(it) + ":" + markerImages[it.id]?.key }
        if (key == lastShopsKey) {
            renderPendingShopKeys()
            return
        }
        lastShopsKey = key
        shopKeys = shops.values.map(::ArchiveShopClusteringKey)
        shopRenderGeneration += 1
        renderPendingShopKeys()
    }

    private fun onLeafMarkerClick(shop: RamenShop) {
        val shops = overlappingMarkerShops(shop, shopKeys.map { it.shop })
        if (shops.size < 2) {
            onShopClick(shop)
            return
        }
        mapView.projection.pointFromLatLng(NMGLatLng.latLngWithLat(shop.location.lat, shop.location.lng)).useContents {
            onClusterClick(shops, x, y)
        }
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
        selectedShopId: String?,
    ): Boolean {
        mapView.contentInset =
            UIEdgeInsetsMake(
                top = 0.0,
                left = 0.0,
                bottom =
                    selectedMarkerInsetPoints(
                        heightPixels = viewportHeightPixels,
                        density = viewportDensity,
                        hasSelection = selectedShopId != null,
                        shopCount = shops.size,
                    ),
                right = 0.0,
            )
        val key =
            shops.values.joinToString("|") { "${it.id}:${it.location.lat}:${it.location.lng}" }
        if (shops.isEmpty()) return false
        if (key == lastFocusKey) return true
        lastFocusKey = key
        mapView.positionMode = NMFMyPositionNormal

        when {
            shops.size == 1 -> moveToShop(shops.values.first())
            else -> {
                val points = shops.values.map { NMGLatLng.latLngWithLat(it.location.lat, it.location.lng) }
                val bounds = NMGLatLngBounds.latLngBoundsWithLatLngs(points)
                moveCamera(NMFCameraUpdate.cameraUpdateWithFitBounds(bounds, FOCUS_PADDING))
            }
        }
        return true
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

    fun initialCameraConfigured() {
        initialRenderState.cameraConfigured()
    }

    override fun mapViewDidFinishRenderingFrame(
        mapView: NMFMapView,
        fully: Boolean,
        stable: Boolean,
    ) {
        if (!isDisposed && initialRenderState.onFrameRendered(fully, stable)) onMapReady()
    }

    fun dispose() {
        isDisposed = true
        shopRenderGeneration += 1
        isRenderingShops = false
        clusterer.mapView = null
        clusterer.clear()
        shopKeys = emptyList()
        leafUpdater.updateImages(emptyMap())
        mapView.removeCameraDelegate(this)
        mapView.removeLoadDelegate(this)
        mapView.removeRenderDelegate(this)
        locationManager?.removeDelegate(this)
    }

    private fun moveToShop(shop: RamenShop) {
        moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(shop.location.lat, shop.location.lng),
                zoomTo = ArchiveMapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL.toDouble(),
            ),
        )
    }

    private fun moveToCurrentLocation(location: Location) {
        shouldMoveToCurrentLocation = false
        moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(location.lat, location.lng),
                zoomTo = ArchiveCurrentLocationConfig.zoomForCurrentLocation(mapView.cameraPosition.zoom),
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
        val keyTagMap: Map<Any?, Any> = keysForRender.associate { key -> key to ArchiveShopMarkerTag(key.shop) }
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
        NMCComplexBuilder()
            .apply {
                maxClusteringZoom = clusteringOptions.maxClusteringZoomLevel.toLong()
                maxIndexingZoom = clusteringOptions.maxClusteringZoomLevel.toLong()
                thresholdStrategy = NMCDefaultThresholdStrategy(threshold = clusteringOptions.screenDistanceDp)
                maxScreenDistance = clusteringOptions.screenDistanceDp
                animationDuration = 0.0
                leafMarkerUpdater = leafUpdater
                clusterMarkerUpdater = clusterUpdater
                tagMergeStrategy = ArchiveShopTagMergeStrategy()
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

        val cameraPosition = currentCameraPosition()
        if (cameraPosition == lastNotifiedCameraPosition) return

        lastNotifiedCameraPosition = cameraPosition
        onCameraPositionChanged(cameraPosition)
    }

    private fun canNotifyViewport(): Boolean =
        !isDisposed &&
            hasMapLoaded &&
            mapView.contentWidth > 0.0 &&
            mapView.contentHeight > 0.0

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

    private fun shopRenderKey(shop: RamenShop): String = "${shop.id}:${shop.location.lat}:${shop.location.lng}:${shop.name}:${shop.isVisible}"
}

internal fun selectedMarkerInsetPoints(
    heightPixels: Int,
    density: Float,
    hasSelection: Boolean,
    shopCount: Int,
): Double {
    if (!hasSelection || shopCount != 1) return 0.0
    return heightPixels.toDouble() / density.toDouble() * ArchiveMapInteractionConfig.SELECTED_MARKER_BOTTOM_PADDING_RATIO.toDouble()
}

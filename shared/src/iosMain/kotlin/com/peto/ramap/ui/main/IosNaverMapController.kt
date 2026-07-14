@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.peto.ramap.ui.main.map

import co.touchlab.kermit.Logger
import cocoapods.NMapsMap.NMCBuilder
import cocoapods.NMapsMap.NMFCameraUpdate
import cocoapods.NMapsMap.NMFLocationManager
import cocoapods.NMapsMap.NMFLocationManagerDelegateProtocol
import cocoapods.NMapsMap.NMFMapView
import cocoapods.NMapsMap.NMFMapViewCameraDelegateProtocol
import cocoapods.NMapsMap.NMFMyPositionDirection
import cocoapods.NMapsMap.NMFMyPositionNormal
import cocoapods.NMapsMap.NMFNaverMapView
import cocoapods.NMapsMap.NMGLatLng
import cocoapods.NMapsMap.NMGLatLngBounds
import com.peto.ramap.core.config.DefaultMapConfig
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.nearestTo
import com.peto.ramap.ui.main.ShopLeafMarkerUpdater
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectZero
import platform.CoreLocation.CLLocation
import platform.UIKit.UIEdgeInsetsMake
import platform.darwin.NSObject

private const val FOCUS_PADDING = 120.0
private const val DEBUG_SAMPLE_SIZE = 5
private val mapLogger = Logger.withTag("RamapIosMap")

internal class IosNaverMapController(
    private val onMapMoveStarted: () -> Unit,
    private val onBoundsChanged: (MapBounds) -> Unit,
    private val onShopClick: (RamenShop) -> Unit,
    private val onMyLocationChanged: (Location) -> Unit,
) : NSObject(),
    NMFMapViewCameraDelegateProtocol,
    NMFLocationManagerDelegateProtocol {
    val view = NMFNaverMapView(frame = CGRectZero.readValue())
    val mapView = view.mapView
    var viewportHeight: Int = 0
    private val leafUpdater = ShopLeafMarkerUpdater(onShopClick)
    private val clusterer =
        NMCBuilder()
            .apply {
                maxZoom = MapInteractionConfig.CLUSTER_MAX_ZOOM_LEVEL.toLong()
                leafMarkerUpdater = leafUpdater
            }.build()
    private var lastShopsKey = ""
    private var lastFocusKey = ""
    private var lastCurrentLocationFocusKey = 0L
    private var currentLocation: Location? = null
    private var isCameraMoving = false
    private var shopKeys = emptyList<ShopClusteringKey>()
    private val locationManager = NMFLocationManager.sharedInstance()

    init {
        view.showCompass = false
        view.showScaleBar = false
        view.showZoomControls = false
        view.showLocationButton = true
        mapView.minZoomLevel = MapInteractionConfig.MAX_ZOOM_OUT_LEVEL.toDouble()
        mapView.addCameraDelegate(this)
        locationManager?.addDelegate(this)
        mapLogger.d {
            "init: minZoom=${mapView.minZoomLevel}, clusterMaxZoom=${MapInteractionConfig.CLUSTER_MAX_ZOOM_LEVEL}"
        }
        mapView.moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(DefaultMapConfig.LATITUDE, DefaultMapConfig.LONGITUDE),
                zoomTo = DefaultMapConfig.ZOOM_LEVEL.toDouble(),
            ),
        )
        clusterer.mapView = mapView
    }

    fun updateShops(shops: List<RamenShop>) {
        val key = shops.joinToString("|") { "${it.id}:${it.location.lat}:${it.location.lng}" }
        if (key == lastShopsKey && !clusterer.empty) {
            mapLogger.d {
                "updateShops: skip sameKey shops=${shops.size}, clustererEmpty=${clusterer.empty}, zoom=${mapView.cameraPosition.zoom}"
            }
            return
        }
        val sample = shops.take(DEBUG_SAMPLE_SIZE).joinToString { it.debugLabel() }
        mapLogger.d {
            "updateShops: shops=${shops.size}, sameKey=${key == lastShopsKey}, " +
                "clustererEmpty=${clusterer.empty}, zoom=${mapView.cameraPosition.zoom}, sample=$sample"
        }
        lastShopsKey = key
        shopKeys = shops.map(::ShopClusteringKey)
        renderShopKeys()
    }

    fun updateFocus(
        shops: List<RamenShop>,
        focusNearestToCurrentLocation: Boolean,
        focusRequestKey: Long,
        selectedShopId: String?,
    ) {
        mapLogger.d {
            "updateFocus: focusShops=${shops.size}, nearest=$focusNearestToCurrentLocation, selected=$selectedShopId, request=$focusRequestKey, zoom=${mapView.cameraPosition.zoom}"
        }
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
            shops.joinToString("|") { "${it.id}:${it.location.lat}:${it.location.lng}" } +
                ":${location != null}:$focusRequestKey"
        if (shops.isEmpty() || key == lastFocusKey) return
        lastFocusKey = key
        mapView.positionMode = NMFMyPositionNormal

        val nearest = shops.nearestTo(location)
        when {
            shops.size == 1 -> moveToShop(shops.first())
            nearest != null -> moveToShop(nearest)
            else -> {
                val points = shops.map { NMGLatLng.latLngWithLat(it.location.lat, it.location.lng) }
                val bounds = NMGLatLngBounds.latLngBoundsWithLatLngs(points)
                mapView.moveCamera(NMFCameraUpdate.cameraUpdateWithFitBounds(bounds, FOCUS_PADDING))
            }
        }
    }

    fun updateInitialLocationFocus(
        location: Location?,
        requestKey: Long,
    ) {
        if (location == null || requestKey == 0L || requestKey == lastCurrentLocationFocusKey) return
        lastCurrentLocationFocusKey = requestKey
        mapView.moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(location.lat, location.lng),
            ),
        )
        mapView.positionMode = NMFMyPositionNormal
    }

    fun bootstrapInitialLocationFocus() {
        mapView.positionMode = NMFMyPositionDirection
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
        }
    }

    override fun mapViewCameraIdle(mapView: NMFMapView) {
        isCameraMoving = false
        mapLogger.d {
            "cameraIdle: zoom=${mapView.cameraPosition.zoom}, shopKeys=${shopKeys.size}, clustererEmpty=${clusterer.empty}"
        }

        val bounds = mapView.contentBounds
        mapLogger.d {
            "cameraIdle bounds: lat=${bounds.southWestLat()}..${bounds.northEastLat()}, lng=${bounds.southWestLng()}..${bounds.northEastLng()}"
        }
        onBoundsChanged(
            MapBounds(
                minLat = bounds.southWestLat(),
                maxLat = bounds.northEastLat(),
                minLng = bounds.southWestLng(),
                maxLng = bounds.northEastLng(),
            ),
        )
    }

    override fun mapView(
        mapView: NMFMapView,
        cameraIsChangingByReason: Long,
    ) {
        if (isCameraMoving) return
        isCameraMoving = true
        onMapMoveStarted()
    }

    fun dispose() {
        mapLogger.d {
            "dispose: shopKeys=${shopKeys.size}, clustererEmpty=${clusterer.empty}, zoom=${mapView.cameraPosition.zoom}"
        }
        clusterer.mapView = null
        clusterer.clear()
        shopKeys = emptyList()
        mapView.removeCameraDelegate(this)
        locationManager?.removeDelegate(this)
    }

    private fun moveToShop(shop: RamenShop) {
        mapLogger.d {
            "moveToShop: ${shop.debugLabel()}, zoom=${MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL}"
        }
        mapView.moveCamera(
            NMFCameraUpdate.cameraUpdateWithScrollTo(
                NMGLatLng.latLngWithLat(shop.location.lat, shop.location.lng),
                zoomTo = MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL.toDouble(),
            ),
        )
    }

    private fun renderShopKeys() {
        val sample = shopKeys.take(DEBUG_SAMPLE_SIZE).joinToString { it.shop.debugLabel() }
        mapLogger.d {
            "renderShopKeys start: keys=${shopKeys.size}, clustererEmpty=${clusterer.empty}, " +
                "zoom=${mapView.cameraPosition.zoom}, sample=$sample"
        }
        clusterer.clear()
        val keyTagMap: Map<Any?, Any> = shopKeys.associate { key -> key to ShopMarkerTag(key.shop) }
        clusterer.addAll(keyTagMap)
        mapLogger.d {
            "renderShopKeys end: keys=${shopKeys.size}, clustererEmpty=${clusterer.empty}, zoom=${mapView.cameraPosition.zoom}"
        }
    }

    private fun RamenShop.debugLabel(): String = "$id(${location.lat},${location.lng},visible=$isVisible)"
}

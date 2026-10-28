package com.peto.ramap.ui.main.map.archive.model

import com.peto.ramap.domain.model.shop.RamenShop

private const val OVERLAPPING_MARKER_MAX_DISTANCE_METERS = 10.0

internal fun overlappingMarkerShops(
    selectedShop: RamenShop,
    shops: Collection<RamenShop>,
): List<RamenShop> =
    shops.filter {
        selectedShop.location.distanceMetersTo(it.location) <= OVERLAPPING_MARKER_MAX_DISTANCE_METERS
    }

internal fun hasOnlyOverlappingMarkers(shops: List<RamenShop>): Boolean =
    shops.size >= 2 &&
        shops.indices.all { firstIndex ->
            (firstIndex + 1 until shops.size).all { secondIndex ->
                shops[firstIndex].location.distanceMetersTo(shops[secondIndex].location) <=
                    OVERLAPPING_MARKER_MAX_DISTANCE_METERS
            }
        }

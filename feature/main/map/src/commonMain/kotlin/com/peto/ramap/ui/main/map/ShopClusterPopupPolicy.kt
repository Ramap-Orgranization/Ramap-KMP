package com.peto.ramap.ui.main.map

import com.peto.ramap.domain.model.shop.RamenShop

private const val OVERLAPPING_MARKER_MAX_DISTANCE_METERS = 1.0

internal fun hasOnlyOverlappingMarkers(shops: List<RamenShop>): Boolean =
    shops.size >= 2 &&
        shops.indices.all { firstIndex ->
            (firstIndex + 1 until shops.size).all { secondIndex ->
                shops[firstIndex].location.distanceMetersTo(shops[secondIndex].location) <=
                    OVERLAPPING_MARKER_MAX_DISTANCE_METERS
            }
        }

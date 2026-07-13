package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.Location

data class InitialLocationFocus(
    val location: Location? = null,
    val requestKey: Long = 0,
    val hasRequested: Boolean = false,
) {
    fun request(location: Location): InitialLocationFocus {
        if (hasRequested) return this
        return copy(
            location = location,
            requestKey = requestKey + 1,
            hasRequested = true,
        )
    }

    fun consume(): InitialLocationFocus = copy(location = null)
}

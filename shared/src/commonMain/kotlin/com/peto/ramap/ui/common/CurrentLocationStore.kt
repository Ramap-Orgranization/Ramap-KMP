package com.peto.ramap.ui.common

import com.peto.ramap.domain.model.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrentLocationStore {
    private val mutableLocation = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = mutableLocation.asStateFlow()

    fun update(location: Location) {
        mutableLocation.value = location
    }
}

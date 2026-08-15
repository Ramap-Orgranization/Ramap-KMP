package com.peto.ramap.ui.main.map.model.location

internal enum class CurrentLocationRequestState {
    Idle,
    Loading,
    ;

    val isLoading: Boolean
        get() = this == Loading

    fun start(): CurrentLocationRequestState = Loading

    fun finish(): CurrentLocationRequestState = Idle

    fun timeout(): CurrentLocationRequestState = Idle
}

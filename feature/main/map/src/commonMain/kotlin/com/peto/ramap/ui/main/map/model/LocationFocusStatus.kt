package com.peto.ramap.ui.main.map.model

import com.peto.ramap.domain.model.shop.Location

/**
 * 지도 진입 후 최초 현재 위치로 카메라가 한 번 이동하는 과정을 나타내는 상태.
 *
 * 위치 수신을 기다리는 [AwaitingLocationStatus]에서 시작해 최초 위치를 받으면 [Pending]이 되고,
 * 플랫폼 지도가 카메라 이동을 처리하면 [Consumed]로 전환된다.
 */
sealed interface LocationFocusStatus {
    /** 아직 최초 현재 위치를 수신하지 않은 상태. */
    data object AwaitingLocationStatus : LocationFocusStatus

    /**
     * 최초 현재 위치를 수신해 플랫폼 지도의 카메라 이동을 기다리는 상태.
     *
     * @property location 카메라가 이동할 최초 현재 위치
     */
    data class Pending(
        val location: Location,
    ) : LocationFocusStatus

    /** 최초 현재 위치로의 카메라 이동 요청이 처리된 상태. */
    data object Consumed : LocationFocusStatus

    /**
     * 최초 현재 위치를 카메라 포커스 대상으로 등록한다.
     *
     * [AwaitingLocationStatus]에서만 [Pending]으로 전환하며
     * 이미 요청했거나 처리된 상태에서는 기존 상태를 유지한다.
     */
    fun request(location: Location): LocationFocusStatus =
        when (this) {
            AwaitingLocationStatus -> Pending(location)
            is Pending,
            Consumed,
            -> this
        }
}

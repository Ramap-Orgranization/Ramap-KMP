package com.peto.ramap.data.model

import com.peto.ramap.domain.model.UnregisteredPlaceReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnregisteredPlaceReportRequest(
    @SerialName("place_url")
    val placeUrl: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
) {
    companion object {
        fun from(report: UnregisteredPlaceReport): UnregisteredPlaceReportRequest =
            UnregisteredPlaceReportRequest(
                placeUrl = report.placeUrl,
                lat = report.location?.lat,
                lng = report.location?.lng,
            )
    }
}

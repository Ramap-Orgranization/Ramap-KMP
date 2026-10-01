package com.peto.ramap.data.model

import com.peto.ramap.domain.model.report.UnregisteredPlaceReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UnregisteredPlaceReportRequest(
    @SerialName("place_url")
    val placeUrl: String,
) {
    companion object {
        fun from(report: UnregisteredPlaceReport): UnregisteredPlaceReportRequest =
            UnregisteredPlaceReportRequest(
                placeUrl = report.placeUrl,
            )
    }
}

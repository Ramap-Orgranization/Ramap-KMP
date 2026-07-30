package com.peto.ramap.data.model

import com.peto.ramap.domain.model.report.ShopInformationReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShopInformationReportRequest(
    @SerialName("shop_id")
    val shopId: String,
    @SerialName("shop_name")
    val shopName: String,
    @SerialName("wrong_fields")
    val wrongFields: List<String>,
    val description: String,
) {
    companion object {
        fun from(report: ShopInformationReport): ShopInformationReportRequest =
            ShopInformationReportRequest(
                shopId = report.shopId,
                shopName = report.shopName,
                wrongFields = report.wrongFields.map { it.key },
                description = report.description,
            )
    }
}

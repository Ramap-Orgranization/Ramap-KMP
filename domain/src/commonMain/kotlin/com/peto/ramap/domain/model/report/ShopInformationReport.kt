package com.peto.ramap.domain.model.report

data class ShopInformationReport(
    val shopId: String,
    val shopName: String,
    val wrongFields: Set<ShopInformationField>,
    val description: String,
)

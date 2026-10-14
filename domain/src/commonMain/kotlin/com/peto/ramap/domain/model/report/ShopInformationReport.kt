package com.peto.ramap.domain.model.report

import com.peto.ramap.domain.model.shop.RamenShop

data class ShopInformationReport(
    val shopId: String,
    val shopName: String,
    val wrongFields: Set<ShopInformationField>,
    val description: String,
) {
    companion object {
        fun create(
            shop: RamenShop,
            wrongFields: Set<ShopInformationField>,
            description: String,
        ): ShopInformationReport? {
            if (wrongFields.isEmpty() && description.isBlank()) return null
            return ShopInformationReport(
                shopId = shop.id,
                shopName = shop.name,
                wrongFields = wrongFields,
                description = description.trim(),
            )
        }
    }
}

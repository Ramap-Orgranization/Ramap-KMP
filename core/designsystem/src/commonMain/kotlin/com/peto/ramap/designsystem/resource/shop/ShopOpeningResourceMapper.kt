package com.peto.ramap.designsystem.resource.shop

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.shop.ShopOpening
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.new_shop_opening_date
import ramap.shared.generated.resources.new_shop_opening_date_official
import ramap.shared.generated.resources.new_shop_opening_date_soft

object ShopOpeningResourceMapper {
    fun openingDate(opening: ShopOpening): UiText {
        val resource =
            when (opening.kind) {
                ShopOpening.Kind.OPENING -> Res.string.new_shop_opening_date
                ShopOpening.Kind.OFFICIAL -> Res.string.new_shop_opening_date_official
                ShopOpening.Kind.SOFT -> Res.string.new_shop_opening_date_soft
            }
        return UiText(
            resource = resource,
            arguments = listOf(opening.date.month.ordinal + 1, opening.date.day),
        )
    }
}

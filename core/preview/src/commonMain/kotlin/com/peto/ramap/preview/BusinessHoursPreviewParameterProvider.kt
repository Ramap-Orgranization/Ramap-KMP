package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.shop.BusinessHours
import com.peto.ramap.domain.model.shop.BusinessHoursDay

class BusinessHoursPreviewParameterProvider : PreviewParameterProvider<BusinessHours> {
    override val values: Sequence<BusinessHours> =
        sequenceOf(
            BusinessHours(
                weekly =
                    mapOf(
                        "mon" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                        "sun" to BusinessHoursDay(true, null, null, false, null),
                    ),
                breakTimes = emptyMap(),
                lastOrders = emptyMap(),
                notice = "재료 소진 시 조기 마감될 수 있습니다.",
            ),
        )
}

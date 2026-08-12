package com.peto.ramap.designsystem.resource.businesshours

import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_fri
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_mon
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_sat
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_sun
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_thu
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_tue
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_wed

enum class BusinessHoursWeekday(
    val resource: StringResource,
) {
    MON(Res.string.shop_detail_business_hours_weekday_mon),
    TUE(Res.string.shop_detail_business_hours_weekday_tue),
    WED(Res.string.shop_detail_business_hours_weekday_wed),
    THU(Res.string.shop_detail_business_hours_weekday_thu),
    FRI(Res.string.shop_detail_business_hours_weekday_fri),
    SAT(Res.string.shop_detail_business_hours_weekday_sat),
    SUN(Res.string.shop_detail_business_hours_weekday_sun),
    ;

    val key: String
        get() = name.lowercase()
}

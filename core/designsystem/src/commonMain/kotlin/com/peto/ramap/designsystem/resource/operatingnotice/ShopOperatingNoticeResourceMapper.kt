package com.peto.ramap.designsystem.resource.operatingnotice

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.operating_notice_type_early_closing
import ramap.shared.generated.resources.operating_notice_type_late_opening
import ramap.shared.generated.resources.operating_notice_type_operating_notice
import ramap.shared.generated.resources.operating_notice_type_temporary_closure

object ShopOperatingNoticeResourceMapper {
    fun typeLabel(type: OperatingNoticeType): StringResource =
        when (type) {
            OperatingNoticeType.OPERATING_NOTICE -> Res.string.operating_notice_type_operating_notice
            OperatingNoticeType.TEMPORARY_CLOSURE -> Res.string.operating_notice_type_temporary_closure
            OperatingNoticeType.EARLY_CLOSING -> Res.string.operating_notice_type_early_closing
            OperatingNoticeType.LATE_OPENING -> Res.string.operating_notice_type_late_opening
        }

    fun notice(notice: OperatingNotice): UiText = UiText(typeLabel(notice.type))
}

package com.peto.ramap.designsystem.resource

import com.peto.ramap.domain.model.community.ReportReason
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_reason_harassment
import ramap.shared.generated.resources.review_reason_hate
import ramap.shared.generated.resources.review_reason_other
import ramap.shared.generated.resources.review_reason_privacy
import ramap.shared.generated.resources.review_reason_sexual
import ramap.shared.generated.resources.review_reason_spam
import ramap.shared.generated.resources.review_reason_violence

internal fun reportReasonResource(reason: ReportReason): StringResource =
    when (reason) {
        ReportReason.SPAM -> Res.string.review_reason_spam
        ReportReason.HARASSMENT -> Res.string.review_reason_harassment
        ReportReason.HATE -> Res.string.review_reason_hate
        ReportReason.SEXUAL_CONTENT -> Res.string.review_reason_sexual
        ReportReason.VIOLENCE -> Res.string.review_reason_violence
        ReportReason.PRIVACY -> Res.string.review_reason_privacy
        ReportReason.OTHER -> Res.string.review_reason_other
    }

package com.peto.ramap.domain.model.community

enum class ReportReason {
    SPAM,
    HARASSMENT,
    HATE,
    SEXUAL_CONTENT,
    VIOLENCE,
    PRIVACY,
    OTHER,
    ;

    companion object {
        const val MAX_DETAILS_LENGTH = 1000
    }
}

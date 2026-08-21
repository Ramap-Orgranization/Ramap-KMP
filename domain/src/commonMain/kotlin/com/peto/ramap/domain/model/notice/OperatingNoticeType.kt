package com.peto.ramap.domain.model.notice

enum class OperatingNoticeType {
    OPERATING_NOTICE,
    TEMPORARY_CLOSURE,
    EARLY_CLOSING,
    LATE_OPENING,
    ;

    companion object {
        fun from(id: String): OperatingNoticeType =
            when (id.lowercase()) {
                "operating_notice" -> OPERATING_NOTICE
                "full_close", "temporary_closure" -> TEMPORARY_CLOSURE
                "early_close", "early_closing" -> EARLY_CLOSING
                "late_opening" -> LATE_OPENING
                else -> error("Invalid notice type: $id")
            }
    }
}

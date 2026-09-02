package com.peto.ramap.domain.model.event

/** 7일 이상 한정 메뉴 이벤트는 메뉴판 노출형 중장기 메뉴로 분류한다. */
enum class LimitedMenuDuration {
    ONE_DAY,
    SHORT_TERM,
    LONG_TERM,
}

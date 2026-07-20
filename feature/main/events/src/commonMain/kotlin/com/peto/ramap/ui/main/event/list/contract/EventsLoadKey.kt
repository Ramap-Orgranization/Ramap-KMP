package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.ui.loading.LoadKey

/** 이벤트 목록 화면에서 독립적으로 표시하는 로딩 종류. */
enum class EventsLoadKey : LoadKey {
    /** 기존 목록을 유지한 채 새 데이터를 요청하는 pull-to-refresh 로딩. */
    Refresh,
}

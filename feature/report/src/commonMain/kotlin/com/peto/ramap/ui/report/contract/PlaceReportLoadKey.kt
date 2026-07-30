package com.peto.ramap.ui.report.contract

import com.peto.ramap.ui.loading.LoadKey

enum class PlaceReportLoadKey : LoadKey {
    /** URL 제보와 현재 위치 제보가 공유하는 저장 로딩. */
    Submit,

    /** 플랫폼에서 현재 위치를 가져오는 로딩. */
    CurrentLocation,

    /** 현재 위치를 주소로 변환하는 로딩. */
    Address,
}

package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.ui.loading.LoadKey

/** 지도 화면에서 부분 UI로 표시하는 로딩 종류. */
enum class MapLoadKey : LoadKey {
    /** 선택한 매장의 상세·웨이팅·이벤트 정보를 조회하는 로딩. */
    ShopDetail,
}

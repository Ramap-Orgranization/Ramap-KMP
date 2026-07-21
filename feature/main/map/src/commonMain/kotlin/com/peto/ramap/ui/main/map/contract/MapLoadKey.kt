package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.ui.loading.LoadKey

/** 지도 화면에서 부분 UI로 표시하는 로딩 종류. */
enum class MapLoadKey : LoadKey {
    /** 외부 요청으로 전달된 매장 id를 실제 매장으로 해석하는 로딩. */
    RequestedShop,

    /** 선택한 매장의 상세·웨이팅·이벤트 정보를 조회하는 로딩. */
    ShopDetail,
}

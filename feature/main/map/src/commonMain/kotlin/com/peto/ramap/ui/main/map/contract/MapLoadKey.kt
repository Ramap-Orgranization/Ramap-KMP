package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.ui.loading.LoadKey

/** 지도 화면에서 부분 UI로 표시하는 로딩 종류. */
enum class MapLoadKey : LoadKey {
    /** 검색어에 맞는 등록 매장 또는 검증된 지도 이동 장소를 조회하는 로딩. */
    Search,

    /** 선택한 매장의 상세·웨이팅·이벤트 정보를 조회하는 로딩. */
    ShopDetail,
}

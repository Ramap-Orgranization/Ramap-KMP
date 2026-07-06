package com.peto.ramap.platform

import com.peto.ramap.domain.model.Location

/**
 * 플랫폼별 위치 API에서 현재 위치를 조회해 앱의 공통 [Location] 모델로 반환한다.
 *
 * 권한 요청과 권한 상태 판단은 호출자가 담당한다. 이 provider는 권한이 허용된 뒤
 * 위치 값을 가져오는 책임만 가진다.
 */
internal expect class LocationProvider {
    /**
     * 현재 사용할 수 있는 위치를 반환한다.
     *
     * 플랫폼 위치 서비스가 위치를 제공하지 못하거나 요청이 실패하면 `null`을 반환한다.
     */
    suspend fun position(): Location?
}

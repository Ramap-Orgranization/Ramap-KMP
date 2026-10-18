package com.peto.ramap.designsystem.place

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.place.PlaceSearchErrorCode
import com.peto.ramap.domain.model.place.PlaceSearchException
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.place_search_daily_limited
import ramap.shared.generated.resources.place_search_daily_limited_hours
import ramap.shared.generated.resources.place_search_rate_limited
import ramap.shared.generated.resources.place_search_rate_limited_minutes
import ramap.shared.generated.resources.place_search_rate_limited_seconds
import ramap.shared.generated.resources.place_search_unavailable
import ramap.shared.generated.resources.place_search_unavailable_seconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaceSearchErrorToastTest {
    @Test
    fun `분 제한 안내는 초 단위 대기 시간을 전달한다`() {
        val toast = PlaceSearchErrorToast.resolve(RamapError.Unknown(PlaceSearchException(PlaceSearchErrorCode.REQUEST_LIMIT, 25)))
        assertEquals(Res.string.place_search_rate_limited_seconds, toast?.message)
        assertEquals(listOf("25"), toast?.formatArguments)
        assertEquals(ToastType.ERROR, toast?.type)
        assertEquals(6000L, toast?.durationMills)
    }

    @Test
    fun `분과 시간 표시는 재시도 시간보다 짧게 반올림하지 않는다`() {
        val minutes = PlaceSearchErrorToast.resolve(RamapError.Unknown(PlaceSearchException(PlaceSearchErrorCode.REQUEST_LIMIT, 61)))
        val hours = PlaceSearchErrorToast.resolve(RamapError.Unknown(PlaceSearchException(PlaceSearchErrorCode.DAILY_LIMIT, 3601)))
        assertEquals(Res.string.place_search_rate_limited_minutes, minutes?.message)
        assertEquals(listOf("2"), minutes?.formatArguments)
        assertEquals(Res.string.place_search_daily_limited_hours, hours?.message)
        assertEquals(listOf("2"), hours?.formatArguments)
    }

    @Test
    fun `하루 제한의 대기 시간이 없으면 한국 자정 안내를 선택한다`() {
        val toast = PlaceSearchErrorToast.resolve(RamapError.Unknown(PlaceSearchException(PlaceSearchErrorCode.DAILY_LIMIT)))
        assertEquals(Res.string.place_search_daily_limited, toast?.message)
        assertEquals(emptyList(), toast?.formatArguments)
    }

    @Test
    fun `예산 확인 실패는 서비스 일시 중단 안내로 표시한다`() {
        val toast = PlaceSearchErrorToast.resolve(RamapError.Unknown(PlaceSearchException(PlaceSearchErrorCode.UNAVAILABLE, 3)))
        assertEquals(Res.string.place_search_unavailable_seconds, toast?.message)
        assertEquals(listOf("3"), toast?.formatArguments)
    }

    @Test
    fun `다른 오류는 기존 화면의 오류 처리에 맡긴다`() {
        assertNull(PlaceSearchErrorToast.resolve(RamapError.Http(400)))
        assertNull(PlaceSearchErrorToast.resolve(RamapError.Unknown(IllegalStateException())))
    }

    @Test
    fun `기존 DB 검색의 429와 503은 시간을 추측하지 않고 안내한다`() {
        assertEquals(Res.string.place_search_rate_limited, PlaceSearchErrorToast.resolve(RamapError.Http(429))?.message)
        assertEquals(Res.string.place_search_unavailable, PlaceSearchErrorToast.resolve(RamapError.Http(503))?.message)
        assertEquals(emptyList(), PlaceSearchErrorToast.resolve(RamapError.Http(429))?.formatArguments)
    }
}

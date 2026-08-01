package com.peto.ramap.network

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SupabaseReverseGeocoderTest {
    @Test
    fun `Edge Function 응답에서 주소를 역직렬화한다`() {
        val response = Json.decodeFromString<ReverseGeocodeResponse>("""{"address":"서울특별시 강남구 역삼동 12-3"}""")

        assertEquals("서울특별시 강남구 역삼동 12-3", response.address)
    }

    @Test
    fun `Edge Function 응답에서 주소 없음 값을 역직렬화한다`() {
        val response = Json.decodeFromString<ReverseGeocodeResponse>("""{"address":null}""")

        assertEquals(null, response.address)
    }
}

package com.peto.ramap.network

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class NaverReverseGeocoderTest {
    private val geocoder = NaverReverseGeocoder(HttpClient())

    @Test
    fun `도로명이 없으면 지역과 번지만 조합한다`() {
        val root = response(land = """"number1": "12", "number2": "3"""")

        assertEquals("서울특별시 강남구 역삼동 12-3", geocoder.parseAddress(root))
    }

    @Test
    fun `번지가 없으면 지역과 도로명만 조합한다`() {
        val root = response(land = """"name": "테헤란로"""")

        assertEquals("서울특별시 강남구 역삼동 테헤란로", geocoder.parseAddress(root))
    }

    @Test
    fun `land가 없으면 지역명만 조합한다`() {
        val root = response()

        assertEquals("서울특별시 강남구 역삼동", geocoder.parseAddress(root))
    }

    private fun response(land: String? = null) =
        Json
            .parseToJsonElement(
                """
                {
                    "results": [
                        {
                            "region": {
                                "area1": { "name": "서울특별시" },
                                "area2": { "name": "강남구" },
                                "area3": { "name": "역삼동" },
                                "area4": { "name": "" }
                            }
                            ${land?.let { """, "land": { $it }""" }.orEmpty()}
                        }
                    ]
                }
                """.trimIndent(),
            ).jsonObject
}

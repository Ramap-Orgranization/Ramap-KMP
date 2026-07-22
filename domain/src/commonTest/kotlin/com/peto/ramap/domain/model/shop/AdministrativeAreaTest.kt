package com.peto.ramap.domain.model.shop

import kotlin.test.Test
import kotlin.test.assertEquals

class AdministrativeAreaTest {
    @Test
    fun `17개 광역 지역의 canonical 주소 토큰을 정규화한다`() {
        val addressTokensByArea =
            mapOf(
                AdministrativeArea.SEOUL to listOf("서울", "서울특별시"),
                AdministrativeArea.BUSAN to listOf("부산", "부산광역시"),
                AdministrativeArea.DAEGU to listOf("대구", "대구광역시"),
                AdministrativeArea.INCHEON to listOf("인천", "인천광역시"),
                AdministrativeArea.GWANGJU to listOf("광주", "광주광역시"),
                AdministrativeArea.DAEJEON to listOf("대전", "대전광역시"),
                AdministrativeArea.ULSAN to listOf("울산", "울산광역시"),
                AdministrativeArea.SEJONG to listOf("세종", "세종특별자치시"),
                AdministrativeArea.GYEONGGI to listOf("경기", "경기도"),
                AdministrativeArea.CHUNGBUK to listOf("충북", "충청북도"),
                AdministrativeArea.CHUNGNAM to listOf("충남", "충청남도"),
                AdministrativeArea.JEONNAM to listOf("전남", "전라남도"),
                AdministrativeArea.GYEONGBUK to listOf("경북", "경상북도"),
                AdministrativeArea.GYEONGNAM to listOf("경남", "경상남도"),
                AdministrativeArea.GANGWON to listOf("강원", "강원도", "강원특별자치도"),
                AdministrativeArea.JEONBUK to listOf("전북", "전라북도", "전북특별자치도"),
                AdministrativeArea.JEJU to listOf("제주", "제주도", "제주특별자치도"),
            )

        addressTokensByArea.forEach { (area, addressTokens) ->
            addressTokens.forEach { token ->
                assertEquals(area, AdministrativeAreaParser.fromAddress("$token 테스트구"))
            }
        }
    }

    @Test
    fun `알 수 없거나 비어 있는 주소는 지역으로 해석하지 않는다`() {
        assertEquals(null, AdministrativeAreaParser.fromAddress("해외 테스트구"))
        assertEquals(null, AdministrativeAreaParser.fromAddress(""))
    }
}

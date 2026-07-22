package com.peto.ramap.domain.model.shop

/** 저장된 한국 주소 원문의 첫 토큰을 광역 행정 구역 식별자로 변환한다. */
object AdministrativeAreaParser {
    /**
     * 주소 첫 토큰과 일치하는 광역 행정 구역을 반환한다.
     *
     * 여기서 사용하는 canonical 토큰은 UI 번역 문자열이 아니라 데이터에 저장된 한국 주소
     * 원문이므로 현재 로케일과 무관하게 해석한다.
     */
    fun fromAddress(address: String): AdministrativeArea? = AREA_BY_CANONICAL_ADDRESS_TOKEN[address.canonicalAreaToken()]

    /** 저장된 한국 주소 원문이 [area]에 속하는지 확인한다. */
    fun matches(
        area: AdministrativeArea,
        address: String,
    ): Boolean = fromAddress(address) == area

    private val AREA_BY_CANONICAL_ADDRESS_TOKEN =
        mapOf(
            "서울" to AdministrativeArea.SEOUL,
            "서울특별시" to AdministrativeArea.SEOUL,
            "부산" to AdministrativeArea.BUSAN,
            "부산광역시" to AdministrativeArea.BUSAN,
            "대구" to AdministrativeArea.DAEGU,
            "대구광역시" to AdministrativeArea.DAEGU,
            "인천" to AdministrativeArea.INCHEON,
            "인천광역시" to AdministrativeArea.INCHEON,
            "광주" to AdministrativeArea.GWANGJU,
            "광주광역시" to AdministrativeArea.GWANGJU,
            "대전" to AdministrativeArea.DAEJEON,
            "대전광역시" to AdministrativeArea.DAEJEON,
            "울산" to AdministrativeArea.ULSAN,
            "울산광역시" to AdministrativeArea.ULSAN,
            "세종" to AdministrativeArea.SEJONG,
            "세종특별자치시" to AdministrativeArea.SEJONG,
            "경기" to AdministrativeArea.GYEONGGI,
            "경기도" to AdministrativeArea.GYEONGGI,
            "충북" to AdministrativeArea.CHUNGBUK,
            "충청북도" to AdministrativeArea.CHUNGBUK,
            "충남" to AdministrativeArea.CHUNGNAM,
            "충청남도" to AdministrativeArea.CHUNGNAM,
            "전남" to AdministrativeArea.JEONNAM,
            "전라남도" to AdministrativeArea.JEONNAM,
            "경북" to AdministrativeArea.GYEONGBUK,
            "경상북도" to AdministrativeArea.GYEONGBUK,
            "경남" to AdministrativeArea.GYEONGNAM,
            "경상남도" to AdministrativeArea.GYEONGNAM,
            "강원" to AdministrativeArea.GANGWON,
            "강원도" to AdministrativeArea.GANGWON,
            "강원특별자치도" to AdministrativeArea.GANGWON,
            "전북" to AdministrativeArea.JEONBUK,
            "전라북도" to AdministrativeArea.JEONBUK,
            "전북특별자치도" to AdministrativeArea.JEONBUK,
            "제주" to AdministrativeArea.JEJU,
            "제주도" to AdministrativeArea.JEJU,
            "제주특별자치도" to AdministrativeArea.JEJU,
        )

    private fun String.canonicalAreaToken(): String = trimStart().substringBefore(' ')
}

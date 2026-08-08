package com.peto.ramap.data.datasource.importation

import com.peto.ramap.data.model.ImportationRequest
import com.peto.ramap.network.client.importation.ImportationResponse
import io.ktor.http.Url

internal class RemoteImportationDataSource(
    private val naverDataSource: NaverImportationDataSource,
    private val kakaoDataSource: KakaoImportationDataSource,
) : ImportationDataSource {
    /** 네이버 URL은 네이버 데이터소스로, 그 외 URL은 카카오 데이터소스로 전달한다. */
    override suspend fun analyze(request: ImportationRequest): ImportationResponse =
        if (isNaverUrl(request.url)) {
            naverDataSource.analyze(request.url)
        } else {
            kakaoDataSource.analyze(request.url)
        }

    /** 지원하는 네이버 호스트인지 확인한다. */
    private fun isNaverUrl(value: String): Boolean = runCatching { Url(value.trim()).host in NAVER_HOSTS }.getOrDefault(false)

    private companion object {
        val NAVER_HOSTS = setOf("naver.me", "map.naver.com", "m.map.naver.com")
    }
}

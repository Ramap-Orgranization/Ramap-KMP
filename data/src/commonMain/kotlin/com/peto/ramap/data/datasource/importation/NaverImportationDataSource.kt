package com.peto.ramap.data.datasource.importation

import com.peto.ramap.data.datasource.extension.asDouble
import com.peto.ramap.data.datasource.extension.asText
import com.peto.ramap.data.model.NaverBookmarkResponse
import com.peto.ramap.data.model.NaverBookmarksResponse
import com.peto.ramap.domain.repository.ImportationErrorCode
import com.peto.ramap.domain.repository.ImportationException
import com.peto.ramap.network.client.importation.ImportationFunctionClient
import com.peto.ramap.network.client.importation.ImportationMatchRequest
import com.peto.ramap.network.client.importation.ImportationPlaceRequest
import com.peto.ramap.network.client.importation.ImportationResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.json.Json

/** 네이버 지도 공유 목록을 조회하고 매칭 Function에 전달한다. */
internal class NaverImportationDataSource(
    private val functionClient: ImportationFunctionClient,
    private val httpClient: HttpClient,
) {
    /** 네이버 공유 URL에서 장소를 조회해 매칭 결과를 반환한다. */
    suspend fun analyze(value: String): ImportationResponse {
        val shareId = resolveShareId(value)
        val response =
            httpClient.get(naverBookmarksUrl(shareId)) {
                headers {
                    append(HttpHeaders.Accept, JSON_CONTENT_TYPE)
                    append(HttpHeaders.UserAgent, USER_AGENT)
                    append(HttpHeaders.AcceptLanguage, KOREAN_LANGUAGE)
                    append(REFERER_HEADER, "$NAVER_DETAIL_URL/$shareId")
                }
            }
        if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Forbidden) {
            throw ImportationException(ImportationErrorCode.UNAVAILABLE_LIST)
        }
        if (response.status.value !in 200..299) {
            throw ImportationException(ImportationErrorCode.PROVIDER_FAILURE)
        }

        val places =
            response
                .body<NaverBookmarksResponse>()
                .bookmarkList
                .mapNotNull(::toPlaceRequest)
                .take(MAX_PLACES)
        if (places.isEmpty()) {
            throw ImportationException(ImportationErrorCode.UNAVAILABLE_LIST)
        }
        return functionClient.invoke(
            MATCH_FUNCTION_NAME,
            Json.encodeToJsonElement(
                ImportationMatchRequest.serializer(),
                ImportationMatchRequest(provider = NAVER_PROVIDER, places = places),
            ),
        )
    }

    /** 단축 URL을 최종 네이버 지도 URL로 해석하고 공유 폴더 ID를 반환한다. */
    private suspend fun resolveShareId(value: String): String {
        val input =
            runCatching { Url(value.trim()) }
                .getOrElse { throw ImportationException(ImportationErrorCode.UNSUPPORTED_URL) }
        var finalUrl = input
        repeat(MAX_REDIRECTS) {
            if (finalUrl.host != NAVER_SHORT_HOST) return extractShareId(finalUrl)
            val response = httpClient.get(finalUrl)
            val location =
                response.headers[HttpHeaders.Location]
                    ?: throw ImportationException(ImportationErrorCode.UNAVAILABLE_LIST)
            finalUrl =
                runCatching { Url(location) }
                    .getOrElse { throw ImportationException(ImportationErrorCode.UNSUPPORTED_URL) }
        }
        throw ImportationException(ImportationErrorCode.UNSUPPORTED_URL)
    }

    /** 네이버 지도 공유 폴더 경로에서 공유 ID를 추출한다. */
    private fun extractShareId(url: Url): String {
        if (url.host !in NAVER_MAP_HOSTS) {
            throw ImportationException(ImportationErrorCode.UNSUPPORTED_URL)
        }
        return NAVER_FOLDER_PATTERN
            .find(url.encodedPath)
            ?.groupValues
            ?.get(1)
            ?: throw ImportationException(ImportationErrorCode.UNAVAILABLE_LIST)
    }

    /** 네이버 지도 웹 북마크 API URL을 생성한다. */
    private fun naverBookmarksUrl(shareId: String): String = "$NAVER_BOOKMARKS_API_URL/$shareId/bookmarks$NAVER_BOOKMARKS_QUERY"

    /** 네이버 북마크 응답을 매칭 요청 장소로 변환한다. */
    private fun toPlaceRequest(bookmark: NaverBookmarkResponse): ImportationPlaceRequest? {
        if (bookmark.type != NAVER_PLACE_TYPE || bookmark.available == false || bookmark.matched == false) {
            return null
        }
        val name = bookmark.name?.trim().orEmpty()
        if (name.isEmpty()) return null
        return ImportationPlaceRequest(
            sourceId = bookmark.sid.asText(),
            name = name,
            address = bookmark.address?.trim()?.takeIf(String::isNotEmpty),
            lat = bookmark.py.asDouble(),
            lng = bookmark.px.asDouble(),
        )
    }

    private companion object {
        const val MATCH_FUNCTION_NAME = "importation-match"
        const val NAVER_PROVIDER = "naver"
        const val NAVER_SHORT_HOST = "naver.me"
        const val NAVER_PLACE_TYPE = "place"
        const val USER_AGENT = "Mozilla/5.0"
        const val JSON_CONTENT_TYPE = "application/json"
        const val KOREAN_LANGUAGE = "ko"
        const val REFERER_HEADER = "Referer"
        const val NAVER_DETAIL_URL = "https://pages.map.naver.com/save-pages/pc/detail-list"
        const val NAVER_BOOKMARKS_API_URL =
            "https://pages.map.naver.com/save-pages/api/maps-bookmark/v3/shares"
        const val NAVER_BOOKMARKS_QUERY = "?start=0&limit=5000&sort=lastUseTime&createIdNo=false"
        const val MAX_PLACES = 100
        const val MAX_REDIRECTS = 3
        val NAVER_MAP_HOSTS = setOf("map.naver.com", "m.map.naver.com")
        val NAVER_FOLDER_PATTERN = Regex("/(?:sharedPlace|myPlace)/folder/([a-zA-Z0-9]+)")
    }
}

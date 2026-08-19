package com.peto.ramap.data.datasource.report

import com.peto.ramap.domain.model.report.PlaceLinkProvider
import com.peto.ramap.domain.model.report.ResolvedPlaceLink
import com.peto.ramap.domain.repository.PlaceLinkResolver
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.Url

internal class RemotePlaceLinkResolver(
    private val httpClient: HttpClient,
) : PlaceLinkResolver {
    override suspend fun resolve(url: String): ResolvedPlaceLink? {
        var currentUrl = runCatching { Url(url.trim()) }.getOrNull() ?: return null

        repeat(MAX_REDIRECTS + 1) {
            parse(currentUrl)?.let { return it }
            if (currentUrl.host !in SHORT_HOSTS) return null

            val response = runCatching { httpClient.get(currentUrl) }.getOrNull() ?: return null
            val resolvedUrl = response.call.request.url
            val location = response.headers[HttpHeaders.Location]
            parse(resolvedUrl)?.let { return it }
            currentUrl = runCatching { Url(location.orEmpty()) }.getOrNull() ?: return null
        }
        return null
    }

    private fun parse(url: Url): ResolvedPlaceLink? {
        if (url.host == KAKAO_PLACE_HOST) {
            val placeId =
                url.encodedPath
                    .trim('/')
                    .substringBefore('/')
                    .takeIf(String::isNotEmpty)
            return placeId?.let { ResolvedPlaceLink(PlaceLinkProvider.KAKAO, placeId = it) }
        }
        if (url.host == KAKAO_APP_LINK_HOST) {
            return url.parameters["id"]?.let { ResolvedPlaceLink(PlaceLinkProvider.KAKAO, placeId = it) }
        }
        if (url.host != NAVER_MAP_HOST) return null

        val placeId =
            NAVER_ENTRY_PLACE_PATTERN
                .find(url.encodedPath)
                ?.groupValues
                ?.get(1)
                ?: url.parameters["pinId"]
        val name = url.parameters["title"]?.trim()?.takeIf(String::isNotEmpty)
        return if (placeId == null && name == null) null else ResolvedPlaceLink(PlaceLinkProvider.NAVER, placeId, name)
    }

    private companion object {
        const val KAKAO_APP_LINK_HOST = "applink.map.kakao.com"
        const val KAKAO_PLACE_HOST = "place.map.kakao.com"
        const val NAVER_MAP_HOST = "map.naver.com"
        const val MAX_REDIRECTS = 3
        val SHORT_HOSTS = setOf("kko.to", "naver.me")
        val NAVER_ENTRY_PLACE_PATTERN = Regex("/entry/place/(\\d+)")
    }
}

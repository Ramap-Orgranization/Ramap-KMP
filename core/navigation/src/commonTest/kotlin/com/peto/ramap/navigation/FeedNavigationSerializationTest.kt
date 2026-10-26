package com.peto.ramap.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedNavigationSerializationTest {
    private val json = Json { serializersModule = navKeySerializersModule() }
    private val navKeySerializer = PolymorphicSerializer(NavKey::class)

    @Test
    fun `기존 커뮤니티 선택 탭을 피드로 복원하고 같은 저장 이름을 유지한다`() {
        val savedTab = "\"COMMUNITY\""

        assertEquals(TabStatus.FEED, json.decodeFromString(TabStatus.serializer(), savedTab))
        assertEquals(savedTab, json.encodeToString(TabStatus.serializer(), TabStatus.FEED))
    }

    @Test
    fun `기존 커뮤니티 경로를 다형 내비게이션 키로 복원한다`() {
        val savedRoute = """{"type":"com.peto.ramap.navigation.ScreenRoutes.CommunityTabRoutes"}"""

        assertEquals(ScreenRoutes.FeedTabRoutes, json.decodeFromString(navKeySerializer, savedRoute))
        assertEquals(savedRoute, json.encodeToString(navKeySerializer, ScreenRoutes.FeedTabRoutes))
    }

    @Test
    fun `지도 상세에서 기존 커뮤니티 복귀 탭을 피드로 복원한다`() {
        val savedRoute = """{"type":"com.peto.ramap.navigation.ScreenRoutes.MapRoutes","shopId":"shop-id","returnTab":"COMMUNITY"}"""
        val route = ScreenRoutes.MapRoutes(shopId = "shop-id", returnTab = TabStatus.FEED)

        assertEquals(route, json.decodeFromString(navKeySerializer, savedRoute))
        assertEquals(savedRoute, json.encodeToString(navKeySerializer, route))
    }
}

package com.peto.ramap.navigation

import com.peto.ramap.navigation.deeplink.ShopDeepLink
import com.peto.ramap.navigation.deeplink.ShopDeepLinkParser
import com.peto.ramap.navigation.deeplink.ShopLinkConfig
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShopDeepLinkParserTest {
    private val config =
        ShopLinkConfig(
            baseUrl = "https://ramap-link.vercel.app",
            webHost = "ramap-link.vercel.app",
        )
    private val parser = ShopDeepLinkParser(config)

    @Test
    fun parsesWebAndCustomShopLinks() {
        assertEquals(
            ShopDeepLink.Shop("shop-id"),
            parser.parse("https://ramap-link.vercel.app/shops/shop-id?source=test#detail"),
        )
        assertEquals(
            ShopDeepLink.Shop("shop-id"),
            parser.parse("ramap://shop/shop-id"),
        )
    }

    @Test
    fun preservesSingleReviewIdInShareAndAppLinks() {
        val reviewId = "123e4567-e89b-42d3-a456-426614174000"
        val shareLink = ShopShareLinkFactory(config).createReview("shop/id", reviewId)

        assertEquals(ShopDeepLink.Shop("shop/id", reviewId), parser.parse(shareLink))
        assertEquals(ShopDeepLink.Shop("shop-id", reviewId), parser.parse("ramap://shop/shop-id?review=$reviewId"))
        assertEquals(ShopDeepLink.Shop("shop-id"), parser.parse("ramap://shop/shop-id?review=$reviewId&review=$reviewId"))
        assertEquals(ShopDeepLink.Shop("shop-id"), parser.parse("ramap://shop/shop-id?review=invalid"))
    }

    @Test
    fun rejectsUnsupportedOrMalformedLinks() {
        listOf(
            null,
            "",
            "http://ramap-link.vercel.app/shops/shop-id",
            "https://other.example/shops/shop-id",
            "https://ramap-link.vercel.app/shops/",
            "https://ramap-link.vercel.app/shops/shop-id/extra",
            "ramap://auth",
            "ramap://other/shop-id",
        ).forEach { assertNull(parser.parse(it)) }
    }

    @Test
    fun factoryEncodesShopIdAsPathSegment() {
        assertEquals(
            "https://ramap-link.vercel.app/shops/shop%2Fid",
            ShopShareLinkFactory(config).create("shop/id"),
        )
    }
}

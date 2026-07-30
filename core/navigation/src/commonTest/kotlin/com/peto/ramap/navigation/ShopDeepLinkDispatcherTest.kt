package com.peto.ramap.navigation

import com.peto.ramap.navigation.deeplink.ShopDeepLinkDispatcher
import com.peto.ramap.navigation.deeplink.ShopDeepLinkParser
import com.peto.ramap.navigation.deeplink.ShopLinkConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopDeepLinkDispatcherTest {
    private val dispatcher =
        ShopDeepLinkDispatcher(
            ShopDeepLinkParser(
                ShopLinkConfig(
                    baseUrl = "https://ramap-link.vercel.app",
                    webHost = "ramap-link.vercel.app",
                ),
            ),
        )

    @Test
    fun consumingOldLinkDoesNotRemoveNewLink() {
        val oldLink = "ramap://shop/old"
        val newLink = "ramap://shop/new"

        dispatcher.dispatch(oldLink)
        dispatcher.dispatch(newLink)
        dispatcher.consume(oldLink)

        assertEquals(newLink, dispatcher.pendingDeepLink.value)
    }
}

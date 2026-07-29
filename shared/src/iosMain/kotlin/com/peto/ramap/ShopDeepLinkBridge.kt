package com.peto.ramap

import com.peto.ramap.navigation.deeplink.ShopDeepLinkDispatcher
import org.koin.mp.KoinPlatformTools

fun dispatchShopDeepLink(rawUrl: String?): Boolean {
    val dispatcher = KoinPlatformTools.defaultContext().get().get<ShopDeepLinkDispatcher>()
    return dispatcher.dispatch(rawUrl)
}

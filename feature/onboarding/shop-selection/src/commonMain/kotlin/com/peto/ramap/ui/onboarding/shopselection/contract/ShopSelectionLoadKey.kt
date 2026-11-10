package com.peto.ramap.ui.onboarding.shopselection.contract

import com.peto.ramap.ui.loading.LoadKey

public sealed interface ShopSelectionLoadKey : LoadKey {
    data object Personalization : ShopSelectionLoadKey

    data object Suggestions : ShopSelectionLoadKey

    data object NextPage : ShopSelectionLoadKey

    data object Search : ShopSelectionLoadKey

    data object Complete : ShopSelectionLoadKey

    data object BookmarkUpdates : ShopSelectionLoadKey
}

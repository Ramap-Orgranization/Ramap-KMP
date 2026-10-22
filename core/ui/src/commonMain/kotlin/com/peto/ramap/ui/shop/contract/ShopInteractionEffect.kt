package com.peto.ramap.ui.shop.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface ShopInteractionEffect : SideEffect {
    data class ShareShop(
        val shopId: String,
        val shopName: String,
    ) : ShopInteractionEffect

    data class ShowLoginGuide(
        val description: StringResource,
    ) : ShopInteractionEffect

    data class ShowToast(
        val data: ToastData,
    ) : ShopInteractionEffect
}

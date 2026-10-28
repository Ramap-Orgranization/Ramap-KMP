package com.peto.ramap.ui.main.feed.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

sealed interface FeedSideEffect : SideEffect {
    data class LoginRequired(
        val description: StringResource,
    ) : FeedSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : FeedSideEffect
}

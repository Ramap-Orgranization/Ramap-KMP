package com.peto.ramap.ui.review.detail.contract

import com.peto.ramap.ui.base.SideEffect
import org.jetbrains.compose.resources.StringResource

internal sealed interface ReviewDetailEffect : SideEffect {
    data class RequestLogin(
        val description: StringResource,
    ) : ReviewDetailEffect

    data class ShowError(
        val message: StringResource,
    ) : ReviewDetailEffect

    data object Deleted : ReviewDetailEffect

    data object Reported : ReviewDetailEffect

    data object CommentDeleted : ReviewDetailEffect
}

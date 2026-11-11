package com.peto.ramap.ui.review.detail.contract

import com.peto.ramap.ui.loading.LoadKey

internal sealed interface ReviewDetailLoadKey : LoadKey {
    data object Detail : ReviewDetailLoadKey

    data object SessionRefresh : ReviewDetailLoadKey

    data object Comments : ReviewDetailLoadKey

    data class Replies(
        val parentId: String,
    ) : ReviewDetailLoadKey

    data object Submit : ReviewDetailLoadKey

    data object CommentMutation : ReviewDetailLoadKey

    data object Like : ReviewDetailLoadKey

    data object Delete : ReviewDetailLoadKey

    data object Report : ReviewDetailLoadKey

    data object Notifications : ReviewDetailLoadKey
}

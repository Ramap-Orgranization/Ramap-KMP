package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.feed.ReviewFeedFilter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_empty_title
import ramap.shared.generated.resources.feed_filter_all
import ramap.shared.generated.resources.feed_filter_following
import ramap.shared.generated.resources.feed_filter_saved_shops
import ramap.shared.generated.resources.feed_following_reviews_empty
import ramap.shared.generated.resources.feed_saved_shops_reviews_empty
import ramap.shared.generated.resources.follow_empty_illustration
import ramap.shared.generated.resources.follow_following_empty
import ramap.shared.generated.resources.review_empty_illustration
import ramap.shared.generated.resources.search_empty_illustration
import ramap.shared.generated.resources.shop_review_empty_title

val ReviewFeedFilter.label: StringResource
    get() =
        when (this) {
            ReviewFeedFilter.ALL -> Res.string.feed_filter_all
            ReviewFeedFilter.SAVED_SHOPS -> Res.string.feed_filter_saved_shops
            ReviewFeedFilter.FOLLOWING -> Res.string.feed_filter_following
        }

fun ReviewFeedFilter.emptyImage(hasFilterTargets: Boolean?): DrawableResource =
    when {
        hasFilterTargets != false -> Res.drawable.review_empty_illustration
        this == ReviewFeedFilter.SAVED_SHOPS -> Res.drawable.search_empty_illustration
        this == ReviewFeedFilter.FOLLOWING -> Res.drawable.follow_empty_illustration
        else -> Res.drawable.review_empty_illustration
    }

fun ReviewFeedFilter.emptyMessage(hasFilterTargets: Boolean?): StringResource =
    when (this) {
        ReviewFeedFilter.ALL -> Res.string.shop_review_empty_title
        ReviewFeedFilter.SAVED_SHOPS ->
            when (hasFilterTargets) {
                false -> Res.string.bookmarked_shops_empty_title
                true -> Res.string.feed_saved_shops_reviews_empty
                null -> Res.string.shop_review_empty_title
            }
        ReviewFeedFilter.FOLLOWING ->
            when (hasFilterTargets) {
                false -> Res.string.follow_following_empty
                true -> Res.string.feed_following_reviews_empty
                null -> Res.string.shop_review_empty_title
            }
    }

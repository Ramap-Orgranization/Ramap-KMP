package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.feed.ReviewFeedFilter
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.feed_filter_all
import ramap.shared.generated.resources.feed_filter_following
import ramap.shared.generated.resources.feed_filter_saved_shops

val ReviewFeedFilter.label: StringResource
    get() =
        when (this) {
            ReviewFeedFilter.ALL -> Res.string.feed_filter_all
            ReviewFeedFilter.SAVED_SHOPS -> Res.string.feed_filter_saved_shops
            ReviewFeedFilter.FOLLOWING -> Res.string.feed_filter_following
        }

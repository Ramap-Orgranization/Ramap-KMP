package com.peto.ramap.designsystem.shop.model

import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.usecase.ShopDetail
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

sealed interface ShopDetailSheetUiState {
    data object Closed : ShopDetailSheetUiState

    data class Loading(
        val shopId: String,
        val shop: RamenShop?,
    ) : ShopDetailSheetUiState

    data class Content(
        val detail: ShopDetail,
    ) : ShopDetailSheetUiState

    data class Error(
        val shopId: String,
        val shop: RamenShop?,
    ) : ShopDetailSheetUiState
}

val ShopDetailSheetUiState.businessHoursNoticeStatus: BusinessHoursStatus?
    get() =
        (this as? ShopDetailSheetUiState.Content)
            ?.detail
            ?.shop
            ?.businessHoursStatus(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
            ?.takeIf { it is BusinessHoursStatus.BreakTime || it is BusinessHoursStatus.Closed }

package com.peto.ramap.designsystem.shop.model

import com.peto.ramap.domain.model.businesshour.BusinessHoursStatus
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.usecase.ShopDetail
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

sealed interface ShopDetailSheetUiState {
    fun businessHoursNoticeStatus(): BusinessHoursStatus? {
        val contentState = (this as? Content) ?: return null
        val currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val businessHoursStatus = contentState.detail.shop.businessHoursStatus(currentDateTime)

        return businessHoursStatus?.takeIf { it.isNotOpening }
    }

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

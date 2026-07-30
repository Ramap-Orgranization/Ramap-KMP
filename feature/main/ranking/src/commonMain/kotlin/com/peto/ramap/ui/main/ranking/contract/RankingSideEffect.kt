package com.peto.ramap.ui.main.ranking.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface RankingSideEffect : SideEffect {
    data object ShowLoginGuide : RankingSideEffect

    data class ShowToast(
        val data: ToastData,
    ) : RankingSideEffect
}

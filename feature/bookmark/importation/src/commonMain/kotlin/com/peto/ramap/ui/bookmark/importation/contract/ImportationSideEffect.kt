package com.peto.ramap.ui.bookmark.importation.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface ImportationSideEffect : SideEffect {
    data class showToast(
        val toast: ToastData,
    ) : ImportationSideEffect

    data class ImportCompleted(
        val toast: ToastData,
    ) : ImportationSideEffect
}

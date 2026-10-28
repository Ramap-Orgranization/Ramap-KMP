package com.peto.ramap.ui.main.map.archive.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface ArchiveMapSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : ArchiveMapSideEffect
}

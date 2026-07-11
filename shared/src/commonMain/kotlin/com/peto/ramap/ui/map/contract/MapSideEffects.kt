package com.peto.ramap.ui.map.contract

import com.peto.ramap.designsystem.toast.model.ToastData

data object ShowLoginGuide : MapSideEffect

data class ShowToast(
    val data: ToastData,
) : MapSideEffect

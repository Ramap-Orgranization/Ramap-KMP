package com.peto.ramap.ui.main.map.model

import org.jetbrains.compose.resources.DrawableResource

data class WaitingProviderLink(
    val label: String,
    val icon: DrawableResource,
    val providerUrl: String,
)

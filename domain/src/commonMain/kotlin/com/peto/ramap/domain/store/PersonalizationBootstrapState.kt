package com.peto.ramap.domain.store

sealed interface PersonalizationBootstrapState {
    data object Loading : PersonalizationBootstrapState

    data object Ready : PersonalizationBootstrapState

    data object Error : PersonalizationBootstrapState
}

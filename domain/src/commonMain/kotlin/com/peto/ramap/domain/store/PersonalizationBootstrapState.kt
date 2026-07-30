package com.peto.ramap.domain.store

import com.peto.ramap.domain.model.personalization.ShopPersonalization

sealed interface PersonalizationBootstrapState {
    data object Loading : PersonalizationBootstrapState

    data class Success(
        val value: ShopPersonalization,
    ) : PersonalizationBootstrapState

    data object Error : PersonalizationBootstrapState
}

package com.peto.ramap.domain.store

import com.peto.ramap.domain.model.personalization.Personalization

sealed interface PersonalizationBootstrapState {
    data object Loading : PersonalizationBootstrapState

    data class Success(
        val value: Personalization,
    ) : PersonalizationBootstrapState

    data object Error : PersonalizationBootstrapState
}

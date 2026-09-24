package com.peto.ramap.ui.main.my

import com.peto.ramap.domain.model.profile.AccountProfile

data class MyTabUiState(
    val userId: String? = null,
    val profile: AccountProfile? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val bookmarkedCount: Int? = null,
    val notificationCount: Int? = null,
    val hiddenCount: Int? = null,
)

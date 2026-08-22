package com.peto.ramap.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface EventReadRepository {
    val readEventIds: StateFlow<Set<String>?>

    suspend fun markAsRead(eventId: String)
}

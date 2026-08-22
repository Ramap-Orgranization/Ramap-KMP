package com.peto.ramap.data.datasource.event

import kotlinx.coroutines.flow.StateFlow

interface EventReadDataSource {
    val readEventIds: StateFlow<Set<String>?>

    suspend fun markAsRead(eventId: String)
}

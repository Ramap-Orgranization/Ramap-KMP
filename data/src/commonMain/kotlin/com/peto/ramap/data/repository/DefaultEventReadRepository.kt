package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.event.EventReadDataSource
import com.peto.ramap.domain.repository.EventReadRepository
import kotlinx.coroutines.flow.StateFlow

internal class DefaultEventReadRepository(
    private val dataSource: EventReadDataSource,
) : EventReadRepository {
    override val readEventIds: StateFlow<Set<String>?> = dataSource.readEventIds

    override suspend fun markAsRead(eventId: String) {
        dataSource.markAsRead(eventId)
    }
}

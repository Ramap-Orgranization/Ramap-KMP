package com.peto.ramap.fake

import com.peto.ramap.domain.repository.EventReadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeEventReadRepository(
    initialReadEventIds: Set<String>? = emptySet(),
) : EventReadRepository {
    private val eventIds = MutableStateFlow(initialReadEventIds)

    override val readEventIds: StateFlow<Set<String>?> = eventIds

    override suspend fun markAsRead(eventId: String) {
        if (eventId.isNotBlank()) eventIds.value = eventIds.value.orEmpty() + eventId
    }

    fun updateReadEventIds(readEventIds: Set<String>?) {
        eventIds.value = readEventIds
    }
}

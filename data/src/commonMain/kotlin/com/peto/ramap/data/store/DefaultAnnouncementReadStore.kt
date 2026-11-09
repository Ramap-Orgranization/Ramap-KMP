package com.peto.ramap.data.store

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.announcement.AnnouncementReadDataSource
import com.peto.ramap.domain.model.announcement.AnnouncementReadState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.AnnouncementReadStore
import com.peto.ramap.network.execute.invokeRequest
import com.peto.ramap.platform.storage.announcement.AnnouncementReadStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultAnnouncementReadStore(
    private val dataSource: AnnouncementReadDataSource,
    private val loginRepository: LoginRepository,
    private val guestStorage: AnnouncementReadStorage,
) : AnnouncementReadStore {
    private val accountReads = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    override val state =
        flow {
            loginRepository.awaitInitialization()
            emitAll(
                loginRepository.accountId
                    .distinctUntilChanged()
                    .flatMapLatest { accountId ->
                        if (accountId == null) {
                            guestStorage.readIds.map { AnnouncementReadState(readIds = it) }
                        } else {
                            accountReads
                                .map { AnnouncementReadState(accountId, it[accountId].orEmpty()) }
                                .onStart {
                                    emit(AnnouncementReadState(accountId, accountReads.value[accountId].orEmpty()))
                                    refreshAccount(accountId)
                                }
                        }
                    }.distinctUntilChanged(),
            )
        }

    override suspend fun refresh(): RamapResult<Unit> {
        loginRepository.awaitInitialization()
        val accountId = loginRepository.currentAccountId() ?: return RamapResult.Success(Unit)
        return refreshAccount(accountId)
    }

    override suspend fun markRead(
        announcementId: String,
        accountId: String?,
    ): RamapResult<Unit> {
        loginRepository.awaitInitialization()
        if (loginRepository.currentAccountId() != accountId) return RamapResult.Error(RamapError.Http(401))
        return invokeRequest {
            if (accountId == null) {
                guestStorage.markRead(announcementId)
            } else {
                dataSource.markRead(announcementId, accountId)
                mergeReads(accountId, setOf(announcementId))
            }
        }
    }

    private suspend fun refreshAccount(accountId: String): RamapResult<Unit> =
        invokeRequest {
            val readIds = dataSource.fetchReadIds(accountId)
            mergeReads(accountId, readIds)
        }

    private fun mergeReads(
        accountId: String,
        readIds: Set<String>,
    ) {
        accountReads.update { current -> current + (accountId to (current[accountId].orEmpty() + readIds)) }
    }
}

package com.peto.ramap.ui.task

import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadKey
import kotlinx.coroutines.Job

/** 작업별 generation과 종료 시 한 번만 정리할 상태를 보관한다. */
internal data class TaskEntry<S : State>(
    val generation: Long,
    val job: Job,
    val loadKey: LoadKey?,
    val onFinish: S.() -> S,
)

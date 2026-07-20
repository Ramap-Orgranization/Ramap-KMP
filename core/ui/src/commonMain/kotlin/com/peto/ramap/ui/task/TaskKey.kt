package com.peto.ramap.ui.task

import kotlin.jvm.JvmInline

/**
 * 한 ViewModel 안에서 실행 작업을 식별하는 키.
 *
 * 같은 [TaskKey]를 사용하는 작업끼리만 [TaskPolicy] 정책이 적용된다.
 *
 * @property value ViewModel 내부에서 유일한 작업 식별 문자열
 */
@JvmInline
value class TaskKey(
    val value: String,
)

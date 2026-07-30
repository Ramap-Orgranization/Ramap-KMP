package com.peto.ramap.ui.task

/** 같은 작업이 이미 실행 중일 때 적용할 정책. */
enum class TaskPolicy {
    /** 기존 작업을 취소하고 새 작업으로 교체한다. */
    CancelPrevious,

    /** 기존 작업을 유지하고 새 실행 요청을 무시한다. */
    IgnoreNew,
}

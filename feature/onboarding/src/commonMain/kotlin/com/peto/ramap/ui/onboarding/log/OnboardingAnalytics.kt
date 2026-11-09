package com.peto.ramap.ui.onboarding.log

import co.touchlab.kermit.Logger
import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.platform.permission.NotificationPermissionOutcome
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.ui.onboarding.contract.OnboardingPermission
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

internal class OnboardingAnalytics(
    private val tracker: AnalyticsTracker,
) {
    private val attemptId = Uuid.random().toString()
    private var startedAt: TimeMark? = null
    private var stageShownAt: TimeMark? = null
    private var visibleStage: OnboardingAnalyticsStage? = null
    private val viewedGuides = mutableSetOf<OnboardingAnalyticsStage>()
    private var pendingPermission: OnboardingPermission? = null
    private var eventIndex = 0L
    private var isBackgrounded = false
    private var hasReachedShopGoal = false
    private var hasCompleted = false

    internal fun start(resumesAfterPermissions: Boolean) {
        if (startedAt != null) return
        startedAt = TimeSource.Monotonic.markNow()
        val entry = if (resumesAfterPermissions) "permissions_confirmed" else "permission_guide"
        log("onboarding_started", "entry" to entry)
    }

    internal fun viewStage(stage: OnboardingAnalyticsStage) {
        val previous = visibleStage
        if (previous == stage || startedAt == null || hasCompleted) return
        val previousElapsed = stageShownAt?.elapsedNow()?.inWholeMilliseconds
        visibleStage = stage
        stageShownAt = TimeSource.Monotonic.markNow()
        if (stage != OnboardingAnalyticsStage.PermissionGuide && stage != OnboardingAnalyticsStage.ShopSelection) {
            viewedGuides += stage
        }
        val previousStageParams =
            if (previous != null && previousElapsed != null) {
                arrayOf("from_stage" to previous.key, "previous_stage_ms" to previousElapsed)
            } else {
                emptyArray()
            }
        log("onboarding_stage_viewed", *previousStageParams)
    }

    internal fun permissionGuideConfirmed() = log("onboarding_permission_guide_next")

    internal fun permissionRequested(permission: OnboardingPermission) {
        pendingPermission = permission
        log("onboarding_permission_requested", "permission" to permission.name.lowercase())
    }

    internal fun locationResult(status: PermissionStatus) {
        val result =
            when (status) {
                PermissionStatus.Granted -> "granted"
                PermissionStatus.Denied -> "not_granted"
                PermissionStatus.Blocked -> "blocked"
            }
        permissionResult(OnboardingPermission.Location, result, status == PermissionStatus.Granted)
    }

    internal fun notificationResult(outcome: NotificationPermissionOutcome) {
        val result =
            when (outcome) {
                NotificationPermissionOutcome.Granted -> "newly_granted"
                NotificationPermissionOutcome.AlreadyGranted -> "already_granted"
                NotificationPermissionOutcome.EnabledWithoutRuntimeRequest -> "enabled_without_runtime_request"
                NotificationPermissionOutcome.NotGranted -> "not_granted"
                NotificationPermissionOutcome.DisabledInSettings -> "disabled_in_settings"
                NotificationPermissionOutcome.Failed -> "failed"
            }
        permissionResult(OnboardingPermission.Notification, result, outcome.isGranted.takeUnless { outcome == NotificationPermissionOutcome.Failed })
    }

    private fun permissionResult(
        permission: OnboardingPermission,
        result: String,
        granted: Boolean?,
    ) {
        pendingPermission = null
        val grantParams = if (granted == null) emptyArray() else arrayOf("granted" to if (granted) 1L else 0L)
        log(
            "onboarding_permission_result",
            "permission" to permission.name.lowercase(),
            "result" to result,
            *grantParams,
        )
    }

    internal fun guideAction(
        stage: OnboardingAnalyticsStage,
        action: String,
    ) = log("onboarding_guide_action", "action_stage" to stage.key, "action" to action)

    internal fun shopLoadResult(
        operation: String,
        resultCount: Int,
    ) = log(
        "onboarding_shop_load_result",
        "operation" to operation,
        "result" to if (resultCount == 0) "empty" else "success",
        "result_count" to resultCount,
    )

    internal fun shopLoadFailed(
        operation: String,
        error: RamapError,
    ) = log("onboarding_shop_load_result", "operation" to operation, "result" to "failed", "error_type" to error.analyticsType)

    internal fun bookmarkResult(
        enabled: Boolean,
        savedCount: Int,
        error: RamapError? = null,
    ) {
        val resultParams = if (error == null) emptyArray() else arrayOf("error_type" to error.analyticsType)
        log(
            "onboarding_bookmark_result",
            "action" to if (enabled) "save" else "remove",
            "result" to if (error == null) "success" else "failed",
            "saved_count" to savedCount,
            *resultParams,
        )
    }

    internal fun shopGoalReached(savedCount: Int) {
        if (hasReachedShopGoal) return
        hasReachedShopGoal = true
        log("onboarding_shop_goal_reached", "saved_count" to savedCount)
    }

    internal fun finishRequested(savedCount: Int) = log("onboarding_finish_requested", "saved_count" to savedCount)

    internal fun finishFailed(savedCount: Int) = log("onboarding_finish_failed", "saved_count" to savedCount)

    internal fun completed(savedCount: Int) {
        log("onboarding_completed", "saved_count" to savedCount)
        hasCompleted = true
    }

    internal fun backgrounded() {
        if (visibleStage == null || isBackgrounded || hasCompleted) return
        isBackgrounded = true
        log("onboarding_backgrounded", "pending_permission" to (pendingPermission?.name?.lowercase() ?: "none"))
    }

    internal fun foregrounded() {
        if (!isBackgrounded) return
        isBackgrounded = false
        log("onboarding_foregrounded")
    }

    private fun log(
        eventName: String,
        vararg attributes: Pair<String, Any>,
    ) {
        val elapsed = startedAt?.elapsedNow() ?: return
        if (hasCompleted) return
        val eventParams =
            buildMap<String, Any> {
                put("attempt_id", attemptId)
                put("schema_version", 1)
                put("event_index", ++eventIndex)
                put("elapsed_ms", elapsed.inWholeMilliseconds)
                put("guide_pages_viewed", viewedGuides.size)
                visibleStage?.let { put("stage", it.key) }
                putAll(attributes)
            }
        try {
            tracker.logEvent(
                object : AnalyticsEvent {
                    override val name = eventName

                    override fun params(): Map<String, Any> = eventParams
                },
            )
        } catch (_: Exception) {
            Logger.w("온보딩 이벤트 기록 실패: $eventName")
        }
    }

    private val RamapError.analyticsType: String
        get() =
            when (this) {
                is RamapError.Network -> "network"
                is RamapError.Timeout -> "timeout"
                is RamapError.Http -> "http"
                is RamapError.Serialization -> "serialization"
                is RamapError.Unknown -> "unknown"
            }
}

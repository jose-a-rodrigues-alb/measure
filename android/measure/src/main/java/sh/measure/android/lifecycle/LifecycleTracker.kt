package sh.measure.android.lifecycle

import android.app.Activity
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.Application
import android.os.Bundle
import curtains.onNextDraw
import sh.measure.android.applaunch.LaunchState
import sh.measure.android.attributes.SpanConstant
import sh.measure.android.gestures.ClickData
import sh.measure.android.gestures.GestureListener
import sh.measure.android.gestures.LongClickData
import sh.measure.android.gestures.ScrollData
import sh.measure.android.mainHandler
import sh.measure.android.postAtFrontOfQueueAsync
import sh.measure.android.tracing.InternalTracer
import sh.measure.android.tracing.Scope
import sh.measure.android.tracing.Span
import sh.measure.android.utils.TimeProvider

internal class LifecycleTracker(
    private val application: Application,
    private val timeProvider: TimeProvider,
    private val internalTracer: InternalTracer,
) : ActivityLifecycleAdapter, GestureListener {
    private val createdActivities = mutableMapOf<String, ActivityInfo>()
    private val startedActivities = mutableListOf<String>()
    private val resumedActivities = mutableListOf<String>()
    private var launchInProgress = false
    private var lastAppVisibleTime: Long? = null
    private var coldLaunchComplete = false
    private var transitionSpan: Span? = null
    private var appStartupSpan: Span? = null
    private var appStartupSpanScope: Scope? = null

    private data class ActivityInfo(
        val activityName: String,
        val firstFrameDrawnTimestamp: Long? = null,
        val firstInteraction: Interaction? = null,
        val sameMessage: Boolean,
        val hasSavedState: Boolean,
        val currentActivitySpanScope: Scope? = null,
        val launchType: String? = null,
        val currentActivitySpan: Span? = null,
    )

    private data class Interaction(
        val target: String,
        val targetId: String?,
        val timestamp: Long,
        val type: Type,
    ) {
        enum class Type {
            Click, LongClick, Scroll,
        }
    }

    fun register() {
        application.registerActivityLifecycleCallbacks(this)
        getAppStartTime()?.let { startTime ->
            appStartupSpan = internalTracer.startSpan(
                SpanConstant.APP_STARTUP,
                startTime = convertToEpochTime(startTime),
                setNoParent = true,
            ).apply {
                appStartupSpanScope = makeCurrent()
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val identityHash = Integer.toHexString(System.identityHashCode(activity))
        val hasSavedState = savedInstanceState != null
        createdActivities[identityHash] = ActivityInfo(
            sameMessage = true,
            hasSavedState = hasSavedState,
            activityName = activity.javaClass.name
        )

        // Helps differentiating between warm and hot launches.
        // OnCreateRecord.sameMessage remains true for warm launch. While it gets set to
        // false for hot launch. This is because hot launch does not trigger an onCreate.
        // The handler processes this message after the activity is resumed.
        mainHandler.post {
            if (identityHash in createdActivities) {
                createdActivities[identityHash]?.copy(sameMessage = false)?.let {
                    createdActivities[identityHash] = it
                }
            }
        }

        val appWasInvisible = startedActivities.isEmpty()
        if (appWasInvisible) {
            if (!launchInProgress) {
                launchInProgress = true
                appMightBecomeVisible()
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        val currentActivitySpan = internalTracer.startSpan(SpanConstant.CURRENT_ACTIVITY)
            .setAttribute(SpanConstant.CURRENT_SCREEN_NAME, activity.javaClass.name)
        val currentActivityScope = if (appStartupSpanScope == null || !launchInProgress) {
            currentActivitySpan.makeCurrent()
        } else {
            null
        }
        val appWasInvisible = startedActivities.isEmpty()
        if (appWasInvisible) {
            if (!launchInProgress) {
                launchInProgress = true
                appMightBecomeVisible()
            }
        } else {
            val previousActivity = startedActivities.lastOrNull()
            if (previousActivity != null) {
                createdActivities[previousActivity]?.activityName?.let { activityName ->
                    transitionSpan = internalTracer.startSpan(
                        SpanConstant.ACTIVITY_TRANSITION, setNoParent = true
                    ).setAttribute(SpanConstant.PREVIOUS_SCREEN_NAME, activityName.javaClass.name)
                }
            }
        }
        val identityHash = Integer.toHexString(System.identityHashCode(activity))
        startedActivities += identityHash

        createdActivities[identityHash]?.copy(
            currentActivitySpanScope = currentActivityScope,
            currentActivitySpan = currentActivitySpan,
        )?.let {
            createdActivities[identityHash] = it
        }
    }

    override fun onActivityResumed(activity: Activity) {
        val identityHash = Integer.toHexString(System.identityHashCode(activity))
        resumedActivities += identityHash
        var launchType: String? = null

        val activityInfo = createdActivities[identityHash]
        if (activityInfo == null) {
            launchInProgress = false
            return
        }

        if (launchInProgress) {
            launchInProgress = false
            launchType = computeLaunchType(activityInfo)
        }
        registerFirstFrameDrawn(activity, identityHash, launchType)
    }

    private fun registerFirstFrameDrawn(
        activity: Activity,
        identityHash: String,
        launchType: String?,
    ) {
        activity.window.onNextDraw {
            mainHandler.postAtFrontOfQueueAsync {
                val firstFrameDrawnTime = timeProvider.now()
                updateFirstFrameDrawnTime(identityHash, firstFrameDrawnTime)
                when (launchType) {
                    "Cold" -> {
                        coldLaunchComplete = true
                        val resumedActivity = getResumedActivity()
                        resumedActivity?.let {
                            createdActivities[resumedActivity]?.copy(launchType = "cold")?.let {
                                createdActivities[resumedActivity] = it
                            }
                        }
                        getAppStartTime()?.let { startTime ->
                            appStartupSpan?.let { startupSpan ->
                                internalTracer.startSpan(
                                    SpanConstant.COLD_LAUNCH_TTID,
                                    startTime = convertToEpochTime(startTime),
                                ).setParent(startupSpan)
                                    .setAttribute(SpanConstant.LAUNCH_TYPE, launchType.lowercase())
                                    .setAttribute(
                                        SpanConstant.CURRENT_SCREEN_NAME, activity.javaClass.name
                                    ).end()
                            }
                        }
                    }

                    "Lukewarm" -> {
                        val resumedActivity = getResumedActivity()
                        resumedActivity?.let {
                            createdActivities[resumedActivity]?.copy(launchType = "warm")?.let {
                                createdActivities[resumedActivity] = it
                            }
                        }
                        getAppStartTime()?.let { startTime ->
                            appStartupSpan?.let { startupSpan ->
                                internalTracer.startSpan(
                                    SpanConstant.WARM_LAUNCH_TTID, startTime = startTime
                                ).setAttribute(SpanConstant.LAUNCH_TYPE, launchType.lowercase())
                                    .setAttribute(
                                        SpanConstant.CURRENT_SCREEN_NAME, activity.javaClass.name
                                    ).setParent(startupSpan).end()
                            }
                        }
                    }

                    "Warm" -> {
                        val resumedActivity = getResumedActivity()
                        resumedActivity?.let {
                            createdActivities[resumedActivity]?.copy(launchType = "warm")?.let {
                                createdActivities[resumedActivity] = it
                            }
                        }
                        lastAppVisibleTime?.let {
                            internalTracer.startSpan(
                                SpanConstant.WARM_LAUNCH_TTID,
                                startTime = it,
                                setNoParent = true,
                            ).setAttribute(SpanConstant.LAUNCH_TYPE, launchType.lowercase())
                                .setAttribute(
                                    SpanConstant.CURRENT_SCREEN_NAME, activity.javaClass.name
                                ).end()
                        }
                    }

                    "Hot" -> {
                        val resumedActivity = getResumedActivity()
                        resumedActivity?.let {
                            createdActivities[resumedActivity]?.copy(launchType = "hot")?.let {
                                createdActivities[resumedActivity] = it
                            }
                        }
                        lastAppVisibleTime?.let {
                            internalTracer.startSpan(
                                SpanConstant.HOT_LAUNCH_TTID,
                                startTime = it,
                                setNoParent = true,
                            )
                        }?.setAttribute(SpanConstant.LAUNCH_TYPE, launchType.lowercase())
                            ?.setAttribute(
                                SpanConstant.CURRENT_SCREEN_NAME, activity.javaClass.name
                            )?.end()
                    }

                    else -> {
                        transitionSpan?.setAttribute(
                            SpanConstant.CURRENT_SCREEN_NAME, activity.javaClass.name
                        )?.end()
                        transitionSpan = null
                    }
                }
            }
        }
    }

    private fun updateFirstFrameDrawnTime(identityHash: String, timestamp: Long) {
        createdActivities[identityHash]?.copy(firstFrameDrawnTimestamp = timestamp)
            ?.let { updatedInfo ->
                createdActivities[identityHash] = updatedInfo
            }
    }

    override fun onActivityPaused(activity: Activity) {
        val identityHash = Integer.toHexString(System.identityHashCode(activity))
        resumedActivities.remove(identityHash)
        createdActivities[identityHash]?.currentActivitySpan?.end()
        createdActivities[identityHash]?.currentActivitySpanScope?.close()
    }

    override fun onActivityStopped(activity: Activity) {
        val identityHash = Integer.toHexString(System.identityHashCode(activity))
        startedActivities.remove(identityHash)
    }

    override fun onActivityDestroyed(activity: Activity) {
        val identityHash = Integer.toHexString(System.identityHashCode(activity))
        createdActivities.remove(identityHash)
    }

    private fun appMightBecomeVisible() {
        lastAppVisibleTime = timeProvider.now()
    }

    private fun computeLaunchType(activityInfo: ActivityInfo): String {
        return when {
            coldLaunchComplete -> {
                if (activityInfo.sameMessage) {
                    "Warm"
                } else {
                    "Hot"
                }
            }

            // This could have been a cold launch, but the activity was created with a saved state.
            // Which reflects that the app was previously alive but the system evicted it from
            // memory, but still kept the saved state. This is a "lukewarm" launch as the activity
            // will still be created from scratch. It's not a cold launch as the system can benefit
            // from the saved state.
            LaunchState.processImportanceOnInit == IMPORTANCE_FOREGROUND && activityInfo.hasSavedState -> "Lukewarm"

            // This is clearly a cold launch as the process was started with a foreground importance
            // and does not have a saved state.
            LaunchState.processImportanceOnInit == IMPORTANCE_FOREGROUND -> "Cold"

            // This is a case where activity was created and resumed, but the app was
            // not launched with a foreground importance. The system started the app without
            // foreground importance but decided to change it's mind later. We track this as a
            // lukewarm launch as the system got a chance to warm up before deciding to bring the
            // activity to the foreground. Sadly we do not know when the system changed it's mind, so
            // we just use the same launch time as a cold launch. We cannot rely on
            // app_visible_uptime as it won't be set in this case.
            else -> "Lukewarm"
        }
    }

    private fun getAppStartTime(): Long? {
        return listOfNotNull(
            LaunchState.processStartUptime,
            LaunchState.processStartRequestedUptime,
            LaunchState.contentLoaderAttachUptime,
        ).minOrNull()
    }

    private fun trackTtfi(interaction: Interaction) {
        getResumedActivity()?.let { resumedActivity ->
            val activityInfo = createdActivities[resumedActivity]
            if (activityInfo?.firstInteraction == null) {
                val name = if (activityInfo?.launchType != null) {
                    when (activityInfo.launchType.lowercase()) {
                        "cold" -> SpanConstant.COLD_LAUNCH_TTFI
                        "warm" -> SpanConstant.WARM_LAUNCH_TTFI
                        "lukewarm" -> SpanConstant.WARM_LAUNCH_TTFI
                        "hot" -> SpanConstant.HOT_LAUNCH_TTFI
                        else -> throw IllegalArgumentException("No launch type named ${activityInfo.launchType}")
                    }
                } else {
                    endAppStartupSpan()
                    return
                }

                getAppStartTime()?.let { startTime ->
                    internalTracer.startSpan(
                        name,
                        startTime = convertToEpochTime(startTime),
                    ).setAttribute(SpanConstant.CURRENT_SCREEN_NAME, resumedActivity.javaClass.name)
                        .apply {
                            interaction.targetId?.let {
                                setAttribute(
                                    SpanConstant.GESTURE_TARGET_ID, it
                                )
                            }
                            setAttribute(SpanConstant.GESTURE_TARGET_NAME, interaction.target)
                            setAttribute(
                                SpanConstant.GESTURE_TYPE, interaction.type.name.lowercase()
                            )
                        }.end()
                }
                endAppStartupSpan()
                createdActivities[resumedActivity]?.copy(firstInteraction = interaction)
                    ?.let { createdActivities[resumedActivity] = it }
            }
        }
    }

    private fun endAppStartupSpan() {
        appStartupSpanScope?.close()
        appStartupSpan?.end()
        appStartupSpan = null
        appStartupSpanScope = null
    }

    override fun onClick(clickData: ClickData) {
        trackTtfi(clickData.toInteraction())
    }

    override fun onLongClick(longClickData: LongClickData) {
        trackTtfi(longClickData.toInteraction())
    }

    override fun onScroll(scrollData: ScrollData) {
        trackTtfi(scrollData.toInteraction())
    }

    private fun getResumedActivity(): String? {
        return resumedActivities.lastOrNull()
    }

    private fun ClickData.toInteraction(): Interaction {
        return Interaction(
            type = Interaction.Type.Click,
            targetId = target_id,
            target = target,
            timestamp = timeProvider.now(),
        )
    }

    private fun LongClickData.toInteraction(): Interaction {
        return Interaction(
            type = Interaction.Type.LongClick,
            targetId = target_id,
            target = target,
            timestamp = timeProvider.now(),
        )
    }

    private fun ScrollData.toInteraction(): Interaction {
        return Interaction(
            type = Interaction.Type.Scroll,
            targetId = target_id,
            target = target,
            timestamp = timeProvider.now(),
        )
    }

    private fun convertToEpochTime(elapsedRealtime: Long): Long {
        return timeProvider.now() - (timeProvider.millisTime - elapsedRealtime)
    }
}

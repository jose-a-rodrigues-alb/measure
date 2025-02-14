package sh.measure.android.gestures

import android.view.MotionEvent
import android.view.ViewGroup
import android.view.Window
import sh.measure.android.events.EventProcessor
import sh.measure.android.events.EventType
import sh.measure.android.logger.LogLevel
import sh.measure.android.logger.Logger
import sh.measure.android.tracing.InternalTrace
import sh.measure.android.utils.TimeProvider

internal class GestureCollector(
    private val logger: Logger,
    private val eventProcessor: EventProcessor,
    private val timeProvider: TimeProvider,
) {
    fun register() {
        logger.log(LogLevel.Debug, "Registering gesture collector")
        WindowInterceptor().apply {
            init()
            registerInterceptor(object : WindowTouchInterceptor {
                override fun intercept(motionEvent: MotionEvent, window: Window) {
                    trackGesture(motionEvent, window)
                }
            })
        }
    }

    private fun trackGesture(motionEvent: MotionEvent, window: Window) {
        val gesture = GestureDetector.detect(window.context, motionEvent, timeProvider)
        if (gesture == null || motionEvent.action != MotionEvent.ACTION_UP) {
            return
        }

        // Find the potential view on which the gesture ended on.
        val target = getTarget(gesture, window, motionEvent)
        if (target == null) {
            logger.log(
                LogLevel.Debug,
                "No target found for gesture ${gesture.javaClass.simpleName}",
            )
            return
        } else {
            logger.log(
                LogLevel.Debug,
                "Target found for gesture ${gesture.javaClass.simpleName}: ${target.className}:${target.id}",
            )
        }

        when (gesture) {
            is DetectedGesture.Click -> eventProcessor.track(
                timestamp = gesture.timestamp,
                type = EventType.CLICK,
                data = ClickData.fromDetectedGesture(gesture, target),
            )

            is DetectedGesture.LongClick -> eventProcessor.track(
                timestamp = gesture.timestamp,
                type = EventType.LONG_CLICK,
                data = LongClickData.fromDetectedGesture(gesture, target),
            )

            is DetectedGesture.Scroll -> eventProcessor.track(
                timestamp = gesture.timestamp,
                type = EventType.SCROLL,
                data = ScrollData.fromDetectedGesture(gesture, target),
            )
        }
    }

    private fun getTarget(
        gesture: DetectedGesture,
        window: Window,
        motionEvent: MotionEvent,
    ): Target? {
        return when (gesture) {
            is DetectedGesture.Scroll -> {
                InternalTrace.trace(
                    label = { "msr-scroll-getTarget" },
                    block = {
                        GestureTargetFinder.findScrollable(
                            window.decorView as ViewGroup,
                            motionEvent,
                        )
                    },
                )
            }

            else -> {
                InternalTrace.trace(
                    // Note that this label is also used in [ViewTargetFinderBenchmark].
                    label = { "msr-click-getTarget" },
                    block = {
                        GestureTargetFinder.findClickable(
                            window.decorView as ViewGroup,
                            motionEvent,
                        )
                    },
                )
            }
        }
    }
}

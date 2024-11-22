package sh.measure.android.config

import sh.measure.android.config.EventTrackingLevel.Basic
import sh.measure.android.config.EventTrackingLevel.Full

/**
 * Controls the events to be collected.
 *
 * - [Basic] tracks only important events like app crashes and launch events.
 * - [Full] enables all events to be tracked including custom events. Required for
 *  session replay to work.
 */
sealed interface EventTrackingLevel {
    /**
     * Minimal tracking level, enables collection of only crashes and launch events.
     */
    data object Basic : EventTrackingLevel

    /**
     * Full tracking level, enables collection of all events including custom events.
     *
     * @see [MeasureConfig.sessionSamplingRate] to control the sessions to be reported.
     */
    data object Full : EventTrackingLevel
}

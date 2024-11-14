package sh.measure.android.tracing

import sh.measure.android.Measure
import sh.measure.android.config.MeasureConfig

/**
 * Represents a span in a trace.
 */
interface Span {
    /**
     * Gets the unique identifier for the trace this span belongs to.
     *
     * @return A unique string identifier generated when the root span of this trace
     * was created. For example: "4bf92f3577b34da6a3ce929d0e0e4736"
     *
     * Note: All spans in the same trace share the same trace ID, allowing correlation of
     * related operations across a distributed system.
     */
    val traceId: String

    /**
     * Gets the unique identifier for this span.
     *
     * @return A unique string identifier generated when this span was created.
     * For example: "00f067aa0ba902b7"
     *
     * Note: Each span in a trace has its own unique span ID, while sharing the same trace ID.
     * This allows tracking of specific operations within the larger trace context.
     */
    val spanId: String

    /**
     * Gets the name identifying this span.
     *
     * @return The name assigned to this span when it was created.
     */
    val name: String

    /**
     * Gets the span ID of this span's parent, if one exists.
     *
     * @return The unique identifier of the parent span, or null if this is a root span.
     */
    val parentId: String?

    /**
     * Gets the session identifier associated with this span. A v4-UUID string.
     *
     * @return The unique identifier for the session this span belongs to
     */
    val sessionId: String

    /**
     * Gets the timestamp when this span was started.
     *
     * @return The start time in milliseconds since epoch, obtained via [Measure.getTimestamp].
     */
    val startTime: Long

    /**
     * Gets the list of time-based checkpoints added to this span.
     *
     * @return A mutable list of [Checkpoint] objects, each representing a significant
     * point in time during the span's lifecycle
     *
     * Note: Checkpoints can be added during the span's lifetime using [setCheckpoint] to mark
     * important events or transitions within the traced operation.
     */
    val checkpoints: MutableList<Checkpoint>

    /**
     * Gets the map of attributes attached to this span.
     *
     * @return The attributes added to the span.
     */
    val attributes: Map<String, Any?>

    /**
     * Indicates whether this span has been selected for collection and export.
     *
     * Sampling is performed using head-based sampling strategy - the decision is made at the root span
     * and applied consistently to all spans within the same trace. This ensures that traces are either
     * collected in their entirety or not at all.
     *
     * @return true if this span will be sent to the server for analysis,
     * false if it will be dropped
     *
     * Note: The sampling rate can be configured using [MeasureConfig.traceSamplingRate].
     */
    val isSampled: Boolean

    /**
     * Gets the current status of this span, indicating its outcome or error state.
     *
     * @return [SpanStatus] The status of the span.
     */
    fun getStatus(): SpanStatus

    /**
     * Updates the status of this span.
     *
     * @param status The [SpanStatus] to set for this span
     *
     * Note: This operation has no effect if called after the span has ended.
     */
    fun setStatus(status: SpanStatus): Span

    /**
     * Sets the parent span for this span, establishing a hierarchical relationship.
     *
     * @param parentSpan The span to set as the parent of this span
     *
     * Note: This operation has no effect if called after the span has ended.
     */
    fun setParent(parentSpan: Span): Span

    /**
     * Adds a checkpoint marking a significant moment during the span's lifetime.
     *
     * @param name A descriptive name for this checkpoint, indicating what it represents
     *
     * Note: This operation has no effect if called after the span has ended.
     */
    fun setCheckpoint(name: String): Span

    /**
     * Marks this span as completed, recording its end time.
     *
     * Note: This method can be called only once per span. Subsequent calls will have no effect.
     */
    fun end(): Span

    /**
     * Marks this span as completed using the specified end time.
     *
     * @param timestamp The end time in milliseconds since epoch, obtained via [Measure.getTimestamp]
     *
     * Note: This method can be called only once per span. Subsequent calls will have no effect.
     * Use this variant when you need to trace an operation that has already completed and you
     * have captured its end time using [Measure.getTimestamp].
     */
    fun end(timestamp: Long): Span

    /**
     * Checks if this span has been completed.
     *
     * @return true if [end] has been called on this span, false otherwise
     */
    fun hasEnded(): Boolean

    /**
     * Makes this span the active span in the current thread's context.
     *
     * @return A [Scope] object that must be closed to restore the previous context
     *
     * Example:
     * ```
     * span.makeCurrent().use { scope ->
     *     // Any spans created here will have 'span' as their parent
     *     startSpan("child_operation")
     * } // Previous context is restored when scope is closed
     * ```
     *
     * Note: Any new spans created in this thread will automatically have this span set as their
     * parent until the returned scope is closed. Always close the scope to prevent context leaks.
     */
    fun makeCurrent(): Scope

    /**
     * Executes the given code block with this span as the active span in the current thread's context.
     *
     * @param block The code to execute within this span's context
     * @return The result of executing the block
     * @param T The type of value returned by the block
     *
     * Note: Any spans created within the block will automatically have this span set as their parent.
     * The previous context is automatically restored after the block completes, even if an exception occurs.
     *
     * Example:
     * ```
     * parentSpan.withScope {
     *     // New spans created here will have parentSpan as their parent
     * }
     * ```
     */
    fun <T> withScope(block: () -> T): T

    /**
     * Gets the total duration of this span in milliseconds.
     *
     * @return The time elapsed between span start and end in milliseconds, or 0 if the span
     * hasn't ended yet
     *
     * Note: Duration is only available after calling [end] on the span. For ongoing spans,
     * this method returns 0.
     */
    fun getDuration(): Long

    companion object {

        /**
         * Returns the current span from thread local if any.
         */
        internal fun current(): Span? {
            return SpanStorage.instance.current()
        }

        internal fun invalid(): Span {
            return InvalidSpan()
        }
    }
}

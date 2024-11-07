package sh.measure.android.tracing

import sh.measure.android.SessionManager
import sh.measure.android.logger.LogLevel
import sh.measure.android.logger.Logger
import sh.measure.android.utils.IdProvider
import sh.measure.android.utils.TimeProvider

/**
 * A thread safe implementation of [Span].
 */
internal class MsrSpan(
    private val logger: Logger,
    private val timeProvider: TimeProvider,
    private val spanProcessor: SpanProcessor,
    override val name: String,
    override val spanId: String,
    override val traceId: String,
    override var parentId: String?,
    override val sessionId: String,
    override val startTime: Long,
) : Span, ReadableSpan {
    private val lock = Any()
    private var status = SpanStatus.Unset
    private var endTime = 0L
    private var hasEnded: EndState = EndState.NotEnded
    override val linkedEvents: MutableList<String> = mutableListOf()
    override val spanEvents: MutableList<SpanEvent> = mutableListOf()
    override val attributes: MutableMap<String, Any?> = mutableMapOf()

    companion object {
        fun startSpan(
            name: String,
            logger: Logger,
            timeProvider: TimeProvider,
            spanProcessor: SpanProcessor,
            sessionManager: SessionManager,
            idProvider: IdProvider,
            parentSpan: Span?,
            timestamp: Long? = null,
        ): Span {
            val startTime = timestamp ?: timeProvider.now()
            val spanId: String = idProvider.spanId()
            val traceId = parentSpan?.traceId ?: idProvider.traceId()
            val sessionId = sessionManager.getSessionId()
            val span = MsrSpan(
                logger = logger,
                timeProvider = timeProvider,
                spanProcessor = spanProcessor,
                name = name,
                spanId = spanId,
                traceId = traceId,
                parentId = parentSpan?.spanId,
                sessionId = sessionId,
                startTime = startTime,
            )
            spanProcessor.onStart(span)
            return span
        }
    }

    override fun getStatus(): SpanStatus {
        return this.status
    }

    override fun setStatus(status: SpanStatus): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.status = status
        }
        return this
    }

    override fun setParent(parentSpan: Span): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.parentId = parentSpan.spanId
        }
        return this
    }

    override fun setEvent(name: String, attributes: Map<String, Any?>): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.spanEvents.add(SpanEvent(name, timeProvider.now(), attributes))
        }
        return this
    }

    // TODO: this is not very elegant as it forces callers to cast to MsrSpan.
    internal fun setEventInternal(eventId: String): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.linkedEvents.add(eventId)
        }
        return this
    }

    override fun setAttribute(key: String, value: Boolean): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.attributes.put(key, value)
        }
        return this
    }

    override fun setAttribute(key: String, value: Double): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.attributes.put(key, value)
        }
        return this
    }

    override fun setAttribute(key: String, value: Float): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.attributes.put(key, value)
        }
        return this
    }

    override fun setAttribute(key: String, value: Int): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.attributes.put(key, value)
        }
        return this
    }

    override fun setAttribute(key: String, value: Long): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.attributes.put(key, value)
        }
        return this
    }

    override fun setAttribute(key: String, value: String): Span {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to set parent after span ended")
                return this
            }
            this.attributes.put(key, value)
        }
        return this
    }

    override fun end(): Span {
        endSpanInternal(timeProvider.now())
        return this
    }

    override fun end(timestamp: Long): Span {
        endSpanInternal(timestamp)
        return this
    }

    override fun hasEnded(): Boolean {
        synchronized(lock) {
            return hasEnded != EndState.NotEnded
        }
    }

    private fun endSpanInternal(timestamp: Long) {
        synchronized(lock) {
            if (hasEnded != EndState.NotEnded) {
                logger.log(LogLevel.Warning, "Attempt to end a span($name) that has already ended")
                return
            }
            endTime = timestamp
            hasEnded = EndState.Ending
        }
        spanProcessor.onEnding(this)
        synchronized(lock) {
            hasEnded = EndState.Ended
        }
        spanProcessor.onEnded(this)
    }

    override fun makeCurrent(): Scope {
        return SpanStorage.instance.makeCurrent(this)
    }

    override fun <T> withScope(block: () -> T): T {
        return makeCurrent().use { block() }
    }

    override fun getDuration(): Long {
        synchronized(lock) {
            if (hasEnded != EndState.Ended) {
                logger.log(
                    LogLevel.Warning,
                    "Attempt to duration of a span($name) that has not ended",
                )
                return 0
            } else {
                return calculateDuration()
            }
        }
    }

    override fun toSpanData(): SpanData {
        synchronized(lock) {
            return SpanData(
                spanId = spanId,
                traceId = traceId,
                name = name,
                startTime = startTime,
                endTime = endTime,
                status = status,
                hasEnded = hasEnded == EndState.Ended,
                parentId = parentId,
                sessionId = sessionId,
                spanEvents = spanEvents,
                linkedEvents = linkedEvents,
                attributes = attributes,
                duration = calculateDuration(),
            )
        }
    }

    private fun calculateDuration(): Long {
        return (endTime - startTime).coerceAtLeast(0)
    }

    private enum class EndState {
        NotEnded,
        Ending,
        Ended,
    }
}

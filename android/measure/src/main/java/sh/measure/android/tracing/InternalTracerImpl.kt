package sh.measure.android.tracing

import sh.measure.android.Measure

/**
 * Internal API for creating spans, matches the public API.
 */
internal interface InternalTracer {
    fun startSpan(name: String): Span
    fun startSpan(name: String, startTime: Long): Span
    fun startSpan(name: String, startTime: Long, setNoParent: Boolean): Span
    fun startSpan(name: String, setNoParent: Boolean): Span
    fun getCurrentSpan(): Span?
    fun createSpan(name: String): SpanBuilder?
}

/**
 * Delegates to [Measure] public API for creating and configuring spans. This class
 * makes unit testing spans easy.
 */
internal class InternalTracerImpl : InternalTracer {
    override fun startSpan(name: String): Span {
        return Measure.startSpan(name)
    }

    override fun startSpan(name: String, startTime: Long): Span {
        return Measure.startSpan(name, startTime)
    }

    override fun startSpan(name: String, startTime: Long, setNoParent: Boolean): Span {
        return Measure.startSpan(name, startTime, setNoParent)
    }

    override fun startSpan(name: String, setNoParent: Boolean): Span {
        return Measure.startSpan(name, setNoParent)
    }

    override fun getCurrentSpan(): Span? {
        return Measure.getCurrentSpan()
    }

    override fun createSpan(name: String): SpanBuilder? {
        return Measure.createSpan(name)
    }
}

package sh.measure.android.tracing

import java.util.concurrent.ConcurrentHashMap

internal interface SpanBuffer {
    fun onSpanStart(span: Span)
    fun onSpanEnd(spanId: String)
    fun getActiveSpans(): List<Span>
    fun getActiveRootSpans(): List<Span>
}

internal class SpanBufferImpl : SpanBuffer {
    private val activeSpans: MutableMap<String, Span> = ConcurrentHashMap<String, Span>()

    override fun onSpanStart(span: Span) {
        activeSpans[span.spanId] = span
    }

    override fun onSpanEnd(spanId: String) {
        activeSpans.remove(spanId)
    }

    override fun getActiveSpans(): List<Span> {
        return activeSpans.values.toList()
    }

    override fun getActiveRootSpans(): List<Span> {
        return activeSpans.values.filter { it.parentId == null }.toList()
    }
}

package sh.measure.android.tracing

import sh.measure.android.attributes.AttributeProcessor
import sh.measure.android.events.EventProcessor

internal interface SpanProcessor {
    fun onStart(span: Span)
    fun onEnding(span: ReadableSpan)
    fun onEnded(span: ReadableSpan)
}

internal class MsrSpanProcessor(
    private val eventProcessor: EventProcessor,
    private val attributeProcessors: List<AttributeProcessor>,
) : SpanProcessor {
    override fun onStart(span: Span) {
        attributeProcessors.forEach {
            it.appendAttributes(span.attributes)
        }
    }

    override fun onEnding(span: ReadableSpan) {
    }

    override fun onEnded(span: ReadableSpan) {
        val spanData = span.toSpanData()
        eventProcessor.trackSpan(spanData)
    }
}

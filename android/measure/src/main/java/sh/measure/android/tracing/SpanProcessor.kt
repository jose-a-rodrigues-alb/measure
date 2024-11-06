package sh.measure.android.tracing

import android.util.Log
import sh.measure.android.events.EventProcessor

internal interface SpanProcessor {
    fun onStart(span: Span)
    fun onEnding(span: ReadableSpan)
    fun onEnded(span: ReadableSpan)
}

internal class MsrSpanProcessor(
    private val eventProcessor: EventProcessor,
    private val spanBuffer: SpanBuffer,
) : SpanProcessor {
    override fun onStart(span: Span) {
        spanBuffer.onSpanStart(span)
    }

    override fun onEnding(span: ReadableSpan) {
    }

    override fun onEnded(span: ReadableSpan) {
        val spanData = span.toSpanData()
        Log.i(
            "MsrSpan",
            "name: ${spanData.name}, duration: ${spanData.duration}, id: ${spanData.spanId}, parent:${spanData.parentId}, events:${spanData.events.size}",
        )
        spanBuffer.onSpanEnd(spanData.spanId)
        eventProcessor.trackSpan(spanData)
    }
}

package sh.measure.android.tracing

import android.util.Log
import sh.measure.android.events.EventProcessor

internal interface SpanProcessor {
    fun onStart(span: ReadableSpan)
    fun onEnding(span: ReadableSpan)
    fun onEnded(span: ReadableSpan)
}

internal class MsrSpanProcessor(private val eventProcessor: EventProcessor) : SpanProcessor {
    override fun onStart(span: ReadableSpan) {
    }

    override fun onEnding(span: ReadableSpan) {
    }

    override fun onEnded(span: ReadableSpan) {
        val spanData = span.toSpanData()
        Log.i(
            "MsrSpan",
            "name: ${spanData.name}, duration: ${spanData.duration}, id: ${spanData.spanId}, parent:${spanData.parentId}",
        )
        eventProcessor.trackSpan(spanData)
    }
}

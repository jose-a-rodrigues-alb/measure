package sh.measure.android.tracing

import sh.measure.android.attributes.Attribute
import sh.measure.android.attributes.AttributeProcessor
import sh.measure.android.events.SignalProcessor

internal interface SpanProcessor {
    fun onStart(span: Span)
    fun onEnding(span: ReadableSpan)
    fun onEnded(span: ReadableSpan)
}

internal class MsrSpanProcessor(
    private val signalProcessor: SignalProcessor,
    private val attributeProcessors: List<AttributeProcessor>,
) : SpanProcessor {
    override fun onStart(span: Span) {
        InternalTrace.trace(
            { "msr-spanProcessor-onStart" },
            {
                val threadName = Thread.currentThread().name
                span.attributes[Attribute.THREAD_NAME] = threadName
                attributeProcessors.forEach {
                    it.appendAttributes(span.attributes)
                }
            }
        )
    }

    override fun onEnding(span: ReadableSpan) {
    }

    override fun onEnded(span: ReadableSpan) {
        val spanData = span.toSpanData()
        // Log.d("MsrSpan", spanData.toString())
        signalProcessor.trackSpan(spanData)
    }
}

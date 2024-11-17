package sh.measure.android.tracing

import sh.measure.android.attributes.Attribute
import sh.measure.android.attributes.AttributeProcessor
import sh.measure.android.events.SignalProcessor

internal interface SpanProcessor {
    fun onStart(span: ReadWriteSpan)
    fun onEnding(span: ReadWriteSpan)
    fun onEnded(span: ReadWriteSpan)
}

internal class MsrSpanProcessor(
    private val signalProcessor: SignalProcessor,
    private val attributeProcessors: List<AttributeProcessor>,
) : SpanProcessor {
    override fun onStart(span: ReadWriteSpan) {
        InternalTrace.trace(
            { "msr-spanProcessor-onStart" },
            {
                val threadName = Thread.currentThread().name
                span.setAttribute(Attribute.THREAD_NAME to threadName)
                val attributes = span.getAttributesMap()
                attributeProcessors.forEach {
                    it.appendAttributes(attributes)
                }
            },
        )
    }

    override fun onEnding(span: ReadWriteSpan) {
    }

    override fun onEnded(span: ReadWriteSpan) {
        val spanData = span.toSpanData()
        // Log.d("MsrSpan", spanData.toString())
        signalProcessor.trackSpan(spanData)
    }
}

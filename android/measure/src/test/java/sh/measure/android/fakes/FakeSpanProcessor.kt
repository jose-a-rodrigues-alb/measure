package sh.measure.android.fakes

import sh.measure.android.tracing.ReadableSpan
import sh.measure.android.tracing.Span
import sh.measure.android.tracing.SpanProcessor

internal class FakeSpanProcessor : SpanProcessor {
    val startedSpans = mutableListOf<Span>()
    val endingSpans = mutableListOf<Span>()
    val endedSpans = mutableListOf<Span>()

    override fun onStart(span: Span) {
        startedSpans += span
    }

    override fun onEnding(span: ReadableSpan) {
        endingSpans += span as Span
    }

    override fun onEnded(span: ReadableSpan) {
        endedSpans += span as Span
    }
}

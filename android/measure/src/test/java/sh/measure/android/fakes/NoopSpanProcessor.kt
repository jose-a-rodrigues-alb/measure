package sh.measure.android.fakes

import sh.measure.android.tracing.ReadableSpan
import sh.measure.android.tracing.Span
import sh.measure.android.tracing.SpanProcessor

internal class NoopSpanProcessor : SpanProcessor {
    override fun onStart(span: Span) {
        // No-op
    }

    override fun onEnding(span: ReadableSpan) {
        // No-op
    }

    override fun onEnded(span: ReadableSpan) {
        // No-op
    }
}

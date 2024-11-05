package sh.measure.android.tracing

import org.junit.Assert.assertTrue
import sh.measure.android.fakes.FakeIdProvider
import sh.measure.android.fakes.FakeSessionManager
import sh.measure.android.fakes.FakeSpanProcessor
import sh.measure.android.fakes.NoopLogger
import sh.measure.android.utils.TimeProvider

internal class TestTracer(
    timeProvider: TimeProvider,
) : InternalTracer {
    private val logger = NoopLogger()
    private val sessionManager = FakeSessionManager()
    private val spanProcessor = FakeSpanProcessor()
    private val idProvider = FakeIdProvider()

    private val tracer = MsrTracer(
        logger = logger,
        sessionManager = sessionManager,
        timeProvider = timeProvider,
        spanProcessor = spanProcessor,
        idProvider = idProvider,
    )

    override fun startSpan(name: String): Span {
        return tracer.spanBuilder(name).startSpan()
    }

    override fun startSpan(name: String, startTime: Long): Span {
        return tracer.spanBuilder(name).startSpan(startTime)
    }

    override fun startSpan(name: String, startTime: Long, setNoParent: Boolean): Span {
        return if (setNoParent) {
            tracer.spanBuilder(name).setNoParent().startSpan(startTime)
        } else {
            tracer.spanBuilder(name).startSpan(startTime)
        }
    }

    override fun startSpan(name: String, setNoParent: Boolean): Span {
        return tracer.spanBuilder(name).setNoParent().startSpan()
    }

    override fun getCurrentSpan(): Span? {
        return Span.current()
    }

    override fun createSpan(name: String): SpanBuilder {
        return tracer.spanBuilder(name)
    }

    fun assertSpanStarted(name: String) {
        assertTrue(
            "Expected span '$name' to be started, but it was not found. Started spans: ${spanProcessor.startedSpans.map { it.name }}",
            spanProcessor.startedSpans.find { it.name == name } != null,
        )
    }

    fun assertSpanEnded(name: String) {
        assertTrue(
            "Expected span '$name' to be ended, but it was not found. Ended spans: ${spanProcessor.endedSpans.map { it.name }}",
            spanProcessor.endedSpans.find { it.name == name } != null,
        )
    }
}

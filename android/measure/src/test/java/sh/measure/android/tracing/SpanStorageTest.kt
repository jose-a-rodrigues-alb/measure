package sh.measure.android.tracing

import org.junit.Assert
import org.junit.Test
import sh.measure.android.fakes.FakeSessionManager
import sh.measure.android.fakes.NoopLogger
import sh.measure.android.fakes.NoopSpanProcessor
import sh.measure.android.utils.AndroidTimeProvider
import sh.measure.android.utils.IdProviderImpl
import sh.measure.android.utils.RandomizerImpl
import sh.measure.android.utils.TestClock

class SpanStorageTest {
    private val idProvider = IdProviderImpl(RandomizerImpl())
    private val testClock = TestClock.create()
    private val timeProvider = AndroidTimeProvider(testClock)
    private val logger = NoopLogger()
    private val spanProcessor = NoopSpanProcessor()
    private val sessionManager = FakeSessionManager()

    @Test
    fun `current returns null by default`() {
        Assert.assertNull(SpanStorage.instance.current())
    }

    @Test
    fun `makeCurrent sets span as current`() {
        val span = MsrSpanBuilder("span-name", idProvider, timeProvider, spanProcessor, sessionManager, logger).startSpan()
        val scope = span.makeCurrent()
        Assert.assertEquals(span, SpanStorage.instance.current())
        scope.close()
    }

    @Test
    fun `closing the scope resets the current span`() {
        val spanA = MsrSpanBuilder("span-A", idProvider, timeProvider, spanProcessor, sessionManager, logger).startSpan()
        val spanB = MsrSpanBuilder("span-B", idProvider, timeProvider, spanProcessor, sessionManager, logger).startSpan()
        val spanAScope = spanA.makeCurrent()
        val spanBScope = spanB.makeCurrent()
        Assert.assertEquals(spanB, SpanStorage.instance.current())
        spanBScope.close()
        Assert.assertEquals(spanA, SpanStorage.instance.current())
        spanAScope.close()
        Assert.assertEquals(null, SpanStorage.instance.current())
    }
}

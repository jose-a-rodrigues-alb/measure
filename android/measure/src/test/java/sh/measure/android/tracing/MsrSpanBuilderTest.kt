package sh.measure.android.tracing

import org.junit.Assert
import org.junit.Test
import sh.measure.android.fakes.NoopLogger
import sh.measure.android.utils.AndroidTimeProvider
import sh.measure.android.utils.IdProviderImpl
import sh.measure.android.utils.RandomizerImpl
import sh.measure.android.utils.TestClock

class MsrSpanBuilderTest {
    private val idProvider = IdProviderImpl(randomizer = RandomizerImpl())
    private val testClock = TestClock.create()
    private val timeProvider = AndroidTimeProvider(testClock)
    private val logger = NoopLogger()

    @Test
    fun `setsParent sets span parent`() {
        val parentSpan = MsrSpanBuilder("parent-name", idProvider, timeProvider, logger).startSpan()
        val span =
            MsrSpanBuilder("span-name", idProvider, timeProvider, logger).setParent(parentSpan)
                .startSpan()

        Assert.assertEquals(parentSpan.spanId, span.parentId)
    }

    @Test
    fun `sets parent from thread local storage`() {
        val parentSpan = MsrSpanBuilder("parent-name", idProvider, timeProvider, logger).startSpan()
        parentSpan.makeCurrent().use {
            val span =
                MsrSpanBuilder("span-name", idProvider, timeProvider, logger).startSpan()
            Assert.assertEquals(parentSpan.spanId, span.parentId)
        }
    }

    @Test
    fun `setNoParent forces no parent to be set`() {
        val parentSpan = MsrSpanBuilder("parent-name", idProvider, timeProvider, logger).startSpan()
        parentSpan.makeCurrent().use {
            val span =
                MsrSpanBuilder("span-name", idProvider, timeProvider, logger).setNoParent().startSpan()
            Assert.assertNull(span.parentId)
        }
    }
}

package sh.measure.android.tracing

import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import sh.measure.android.events.EventProcessorImpl
import sh.measure.android.fakes.NoopLogger
import sh.measure.android.fakes.TestData
import sh.measure.android.utils.AndroidTimeProvider
import sh.measure.android.utils.TestClock

class MsrSpanProcessorTest {
    private val eventProcessor = mock<EventProcessorImpl>()
    private val logger = NoopLogger()
    private val timeProvider = AndroidTimeProvider(TestClock.create())

    @Test
    fun `onEnded delegates to event processor`() {
        val spanProcessor = MsrSpanProcessor(eventProcessor)
        val span = TestData.getSpan(
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor,
        )
        spanProcessor.onEnded(span)

        verify(eventProcessor).trackSpan(span.toSpanData())
    }
}

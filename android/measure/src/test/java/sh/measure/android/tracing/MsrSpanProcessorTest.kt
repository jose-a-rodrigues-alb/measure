package sh.measure.android.tracing

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import sh.measure.android.attributes.Attribute
import sh.measure.android.attributes.AttributeProcessor
import sh.measure.android.events.SignalProcessorImpl
import sh.measure.android.fakes.NoopLogger
import sh.measure.android.fakes.TestData
import sh.measure.android.utils.AndroidTimeProvider
import sh.measure.android.utils.TestClock

class MsrSpanProcessorTest {
    private val signalProcessor = mock<SignalProcessorImpl>()
    private val logger = NoopLogger()
    private val timeProvider = AndroidTimeProvider(TestClock.create())

    @Test
    fun `onStart appends attributes to spans`() {
        val attributeProcessor = object : AttributeProcessor {
            override fun appendAttributes(attributes: MutableMap<String, Any?>) {
                attributes["key"] = "value"
            }
        }
        val spanProcessor = MsrSpanProcessor(signalProcessor, listOf(attributeProcessor))
        val span = TestData.getSpan(
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor,
            parentId = null,
        )
        spanProcessor.onStart(span)

        // thread name is always added as an attribute, hence size is 2
        Assert.assertEquals(2, span.toSpanData().attributes.size)
        Assert.assertEquals("value", span.toSpanData().attributes["key"])
    }

    @Test
    fun `onStart adds thread name to attributes`() {
        val spanProcessor = MsrSpanProcessor(signalProcessor, emptyList())
        val span = TestData.getSpan(
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor,
            parentId = null,
        )
        spanProcessor.onStart(span)

        val attributes = span.toSpanData().attributes
        Assert.assertEquals(1, attributes.size)
        Assert.assertEquals(Thread.currentThread().name, attributes[Attribute.THREAD_NAME])
    }

    @Test
    fun `onEnded delegates to event processor`() {
        val spanProcessor = MsrSpanProcessor(signalProcessor, emptyList())
        val span = TestData.getSpan(
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor,
        )
        spanProcessor.onEnded(span)

        verify(signalProcessor).trackSpan(span.toSpanData())
    }
}

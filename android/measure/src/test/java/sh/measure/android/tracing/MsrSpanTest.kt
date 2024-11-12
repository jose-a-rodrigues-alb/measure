package sh.measure.android.tracing

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import sh.measure.android.fakes.FakeSessionManager
import sh.measure.android.fakes.NoopLogger
import sh.measure.android.utils.AndroidTimeProvider
import sh.measure.android.utils.IdProviderImpl
import sh.measure.android.utils.RandomizerImpl
import sh.measure.android.utils.TestClock
import java.time.Duration

class MsrSpanTest {
    private val logger = NoopLogger()
    private val testClock = TestClock.create()
    private val timeProvider = AndroidTimeProvider(testClock)
    private val idProvider = IdProviderImpl(randomizer = RandomizerImpl())
    private val spanProcessor = mock<SpanProcessor>()
    private val sessionManager = FakeSessionManager()

    @Test
    fun `startSpan sets parent span if provided`() {
        val parentSpan = MsrSpan(
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            name = "parent-span",
            spanId = "span-id",
            traceId = "trace-id",
            parentId = null,
            sessionId = sessionManager.getSessionId(),
            startTime = 1000,
        )
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = parentSpan,
            timestamp = null,
        )

        Assert.assertEquals(parentSpan.spanId, span.parentId)
    }

    @Test
    fun `startSpan sets current timestamp`() {
        val epochTime = testClock.epochTime()
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
            timestamp = null,
        )
        Assert.assertEquals(epochTime, span.startTime)
    }

    @Test
    fun `startSpan sets timestamp if provided`() {
        val timestamp = 10000L
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
            timestamp = timestamp,
        )
        Assert.assertEquals(timestamp, span.startTime)
    }

    @Test
    fun `startSpan triggers span processor onStart`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        )

        verify(spanProcessor, times(1)).onStart(span as MsrSpan)
    }

    @Test
    fun `default span status is unset`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        )
        Assert.assertEquals(SpanStatus.Unset, span.getStatus())
    }

    @Test
    fun `setStatus updates the span status`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        ).setStatus(SpanStatus.Ok)
        Assert.assertEquals(SpanStatus.Ok, span.getStatus())
    }

    @Test
    fun `hasEnded for active span`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        )
        Assert.assertEquals(false, span.hasEnded())
    }

    @Test
    fun `hasEnded for ended span`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        ).end()
        Assert.assertEquals(true, span.hasEnded())
    }

    @Test
    fun `end updates the span duration`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        )
        testClock.advance(Duration.ofMillis(1000))
        val duration = span.end().getDuration()

        Assert.assertEquals(1000, duration)
    }

    @Test
    fun `end triggers span processor onEnding and onEnded`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        ).end() as MsrSpan

        spanProcessor.inOrder {
            verify().onEnding(span)
            verify().onEnded(span)
        }
    }

    @Test
    fun `setCheckpoint adds checkpoint to span`() {
        val expectedCheckpoint = Checkpoint("name", timeProvider.now())
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        ) as MsrSpan
        span.setCheckpoint(expectedCheckpoint.name)

        Assert.assertEquals(1, span.checkpoints.size)
        Assert.assertEquals(expectedCheckpoint, span.checkpoints.first())
    }

    @Test
    fun `setEvent on ended span is a no-op`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        ).end()
        span.setCheckpoint("event-id")

        Assert.assertEquals(0, span.checkpoints.size)
    }

    @Test
    fun `setAttribute adds attribute to span`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        )
        span.setAttribute("string", "value")
        span.setAttribute("int", 100)
        span.setAttribute("float", 100F)
        span.setAttribute("long", 100L)
        span.setAttribute("double", 100.0)
        span.setAttribute("boolean", true)

        Assert.assertEquals(6, span.attributes.size)
        Assert.assertEquals("value", span.attributes["string"])
        Assert.assertEquals(100, span.attributes["int"])
        Assert.assertEquals(100F, span.attributes["float"])
        Assert.assertEquals(100L, span.attributes["long"])
        Assert.assertEquals(100.0, span.attributes["double"])
        Assert.assertEquals(true, span.attributes["boolean"])
    }

    @Test
    fun `setAttribute on ended span is a no-op`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        ).end()
        span.setAttribute("string", "value")
        span.setAttribute("int", 100)
        span.setAttribute("float", 100F)
        span.setAttribute("long", 100L)
        span.setAttribute("double", 100.0)
        span.setAttribute("boolean", true)

        Assert.assertEquals(0, span.attributes.size)
    }

    @Test
    fun `duration is 0 for active span`() {
        val span = MsrSpan.startSpan(
            "span-name",
            logger = logger,
            timeProvider = timeProvider,
            spanProcessor = spanProcessor,
            sessionManager = sessionManager,
            idProvider = idProvider,
            parentSpan = null,
        )
        testClock.advance(Duration.ofMillis(1000))
        val duration = span.getDuration()

        Assert.assertEquals(0, duration)
    }
}

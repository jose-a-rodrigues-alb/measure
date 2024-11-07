package sh.measure.android.exporter

import androidx.concurrent.futures.ResolvableFuture
import org.junit.Test
import org.mockito.Mockito.atMostOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import sh.measure.android.fakes.FakeConfigProvider
import sh.measure.android.fakes.ImmediateExecutorService
import sh.measure.android.fakes.NoopLogger
import sh.measure.android.utils.AndroidTimeProvider
import sh.measure.android.utils.TestClock
import java.time.Duration

class PeriodicEventExporterTest {
    private val logger = NoopLogger()
    private val configProvider = FakeConfigProvider()
    private val executorService = ImmediateExecutorService(ResolvableFuture.create<Any>())
    private val testClock = TestClock.create()
    private val timeProvider = AndroidTimeProvider(testClock)
    private val heartbeat = mock<Heartbeat>()
    private val eventExporter = mock<EventExporter>()

    private val periodicEventExporter = PeriodicEventExporterImpl(
        logger,
        configProvider,
        executorService,
        timeProvider,
        heartbeat,
        eventExporter,
    )

    @Test
    fun `adds a listener to heartbeat on initialization`() {
        verify(heartbeat).addListener(periodicEventExporter)
    }

    @Test
    fun `starts heartbeat when app comes to foreground with a delay`() {
        periodicEventExporter.onAppForeground()

        verify(heartbeat, atMostOnce()).start(
            configProvider.eventsBatchingIntervalMs,
            configProvider.eventsBatchingIntervalMs,
        )
    }

    @Test
    fun `starts heartbeat on cold launch with a delay`() {
        periodicEventExporter.onColdLaunch()

        verify(heartbeat, atMostOnce()).start(
            configProvider.eventsBatchingIntervalMs,
            configProvider.eventsBatchingIntervalMs,
        )
    }

    @Test
    fun `stops heartbeat when app goes to background`() {
        periodicEventExporter.onAppBackground()

        verify(heartbeat, atMostOnce()).stop()
    }

    @Test
    fun `exports existing batches, when app goes to background`() {
        val batch1 =
            Batch("batch1", eventIds = listOf("event1, event2"), spanIds = listOf("span1", "span2"))
        val batch2 =
            Batch("batch2", eventIds = listOf("event1, event2"), spanIds = listOf("span1", "span2"))
        `when`(eventExporter.getExistingBatches()).thenReturn(listOf(batch1, batch2))

        periodicEventExporter.onAppBackground()

        verify(eventExporter).export(batch1)
        verify(eventExporter).export(batch2)
    }

    @Test
    fun `stops exporting existing batches if one of them fails to export due to server error`() {
        val batch1 =
            Batch("batch1", eventIds = listOf("event1, event2"), spanIds = listOf("span1", "span2"))
        val batch2 =
            Batch("batch2", eventIds = listOf("event3, event4"), spanIds = listOf("span1", "span2"))
        `when`(eventExporter.getExistingBatches()).thenReturn(listOf(batch1, batch2))
        `when`(eventExporter.export(batch1)).thenReturn(HttpResponse.Error.ServerError(500))

        periodicEventExporter.onAppBackground()

        verify(eventExporter).export(batch1)
        verify(eventExporter, never()).export(batch2)
    }

    @Test
    fun `stops exporting existing batches if one of them fails to export due to rate limit error`() {
        val batch1 =
            Batch("batch1", eventIds = listOf("event1, event2"), spanIds = listOf("span1", "span2"))
        val batch2 =
            Batch("batch2", eventIds = listOf("event3, event4"), spanIds = listOf("span1", "span2"))
        `when`(eventExporter.getExistingBatches()).thenReturn(listOf(batch1, batch2))
        `when`(eventExporter.export(batch1)).thenReturn(HttpResponse.Error.RateLimitError())

        periodicEventExporter.onAppBackground()

        verify(eventExporter).export(batch1)
        verify(eventExporter, never()).export(batch2)
    }

    @Test
    fun `creates and exports new batch when app goes to background and conditions are met`() {
        // Given no existing batches to export
        `when`(eventExporter.getExistingBatches()).thenReturn(listOf())
        // Given a new batch is created successfully
        val batch = Batch("batchId", listOf("event1", "event2"), listOf("span1", "span2"))
        `when`(eventExporter.createBatch()).thenReturn(batch)

        // When
        periodicEventExporter.onAppBackground()

        // Then
        verify(eventExporter).export(batch)
    }

    @Test
    fun `does not export if last batch was created within 30 seconds, when app goes to background`() {
        val initialTime = testClock.epochTime()
        // Given no existing batches to export
        `when`(eventExporter.getExistingBatches()).thenReturn(listOf())
        periodicEventExporter.lastBatchCreationTimeMs = initialTime

        // Advance time within threshold
        testClock.advance(Duration.ofSeconds(29))

        // When
        periodicEventExporter.onAppBackground()

        // Then
        verify(eventExporter, never()).export(any())
    }

    @Test
    fun `given an export is in progress, does not trigger new export, when app goes to background`() {
        // ensure other conditions for triggering an export are met
        val batch = Batch(
            "batch1",
            eventIds = mutableListOf("event1, event2"),
            spanIds = listOf("span1", "span2"),
        )
        `when`(eventExporter.getExistingBatches()).thenReturn(listOf(batch))

        // forcefully mark an export in progress
        periodicEventExporter.isExportInProgress.set(true)

        periodicEventExporter.onAppBackground()

        verify(eventExporter, never()).export(any())
    }
}

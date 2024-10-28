package sh.measure.android.fakes

import sh.measure.android.events.Event
import sh.measure.android.storage.EventStore
import sh.measure.android.tracing.SpanData

internal class FakeEventStore : EventStore {
    val trackedEvents = mutableListOf<Event<*>>()
    val trackedSpans = mutableListOf<SpanData>()

    override fun <T> store(event: Event<T>) {
        trackedEvents.add(event)
    }

    override fun store(spanData: SpanData, sessionId: String) {
        trackedSpans.add(spanData)
    }
}

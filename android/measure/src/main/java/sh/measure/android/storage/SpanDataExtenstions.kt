package sh.measure.android.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import sh.measure.android.tracing.SpanData
import sh.measure.android.utils.toJsonElement

internal fun SpanData.toSpanEntity(): SpanEntity {
    return SpanEntity(
        name = name,
        spanId = spanId,
        startTime = startTime,
        sessionId = sessionId,
        duration = duration,
        status = status,
        parentId = parentId,
        endTime = endTime,
        traceId = traceId,
        serializedEvents = Json.encodeToString(events),
        serializedAttributes = Json.encodeToString(
            JsonElement.serializer(),
            attributes.toJsonElement(),
        ),
        hasEnded = hasEnded,
    )
}

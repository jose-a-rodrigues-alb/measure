package sh.measure.android.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import sh.measure.android.tracing.SpanData
import sh.measure.android.tracing.SpanEvent
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
        serializedSpanEvents = serializeSpanEvent(),
        serializedLinkedEvents = Json.encodeToString(linkedEvents),
        serializedAttributes = Json.encodeToString(
            JsonElement.serializer(),
            attributes.toJsonElement(),
        ),
        hasEnded = hasEnded,
    )
}

private fun SpanData.serializeSpanEvent(): String {
    return spanEvents.joinToString(",", prefix = "[", postfix = "]") { it.serialize() }
}

private fun SpanEvent.serialize(): String {
    val serializedAttributes = Json.encodeToString(attributes.toJsonElement())
    return "{\"name\":\"${name}\",\"timestamp\":$timestamp,\"attributes\":$serializedAttributes}"
}

package sh.measure.android.exporter

internal data class EventPacket(
    val eventId: String,
    val sessionId: String,
    val timestamp: String,
    val type: String,
    val userTriggered: Boolean,
    val serializedData: String?,
    val serializedDataFilePath: String?,
    val serializedAttachments: String?,
    val serializedAttributes: String,
    val serializedUserDefinedAttributes: String?,
)

internal data class SpanPacket(
    val name: String,
    val traceId: String,
    val spanId: String,
    val parentId: String?,
    val sessionId: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val status: Int,
    val serializedAttributes: String?,
    val serializedSpanEvents: String?,
    val hasEnded: Boolean,
)

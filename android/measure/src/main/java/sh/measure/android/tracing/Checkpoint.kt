package sh.measure.android.tracing

/**
 * Annotates a specific time on a span.
 */
class Checkpoint(
    val name: String,
    val timestamp: Long,
)
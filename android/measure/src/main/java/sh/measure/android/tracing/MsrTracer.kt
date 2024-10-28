package sh.measure.android.tracing

import sh.measure.android.SessionManager
import sh.measure.android.logger.Logger
import sh.measure.android.utils.IdProvider
import sh.measure.android.utils.TimeProvider

internal class MsrTracer(
    private val logger: Logger,
    private val idProvider: IdProvider,
    private val timeProvider: TimeProvider,
    private val spanProcessor: SpanProcessor,
    private val sessionManager: SessionManager,
) : Tracer {
    override fun spanBuilder(name: String): SpanBuilder {
        return MsrSpanBuilder(name, idProvider, timeProvider, spanProcessor, sessionManager, logger)
    }
}

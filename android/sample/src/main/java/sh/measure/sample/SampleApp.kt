package sh.measure.sample

import android.app.Application
import sh.measure.android.Measure
import sh.measure.android.config.MeasureConfig
import sh.measure.android.config.ScreenshotMaskLevel
import sh.measure.android.tracing.SpanStatus

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appStartTime = System.currentTimeMillis()
        Measure.init(
            this, MeasureConfig(
                enableLogging = true,
                trackScreenshotOnCrash = true,
                screenshotMaskLevel = if (BuildConfig.DEBUG) {
                    ScreenshotMaskLevel.SensitiveFieldsOnly
                } else {
                    ScreenshotMaskLevel.AllTextAndMedia
                },
                trackHttpHeaders = true,
                trackHttpBody = true,
                trackActivityIntentData = true,
                httpUrlBlocklist = listOf("http://localhost:8080"),
                sessionSamplingRate = 0.5f,
            )
        )
        val msrInitEndTime = System.currentTimeMillis()

        val appSpan = Measure.startSpan("app-on-create", appStartTime)
        appSpan.withScope {
            Measure.startSpan("msr-init", appStartTime).setStatus(SpanStatus.Ok)
                .end(timestamp = msrInitEndTime)
            Measure.setUserId("sample-user-sd")
            Measure.clearUserId()
            Measure.trackScreenView("screen-name")
            Measure.trackHandledException(RuntimeException("sample-handled-exception"))
        }
        appSpan.setStatus(SpanStatus.Ok).end()
    }
}

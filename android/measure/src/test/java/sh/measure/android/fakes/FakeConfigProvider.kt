package sh.measure.android.fakes

import sh.measure.android.config.ConfigProvider
import sh.measure.android.config.ScreenshotMaskLevel

internal class FakeConfigProvider : ConfigProvider {
    override fun loadNetworkConfig() {
        // no-op
    }

    override val enableLogging: Boolean = false
    override var trackScreenshotOnCrash: Boolean = false
    override var screenshotMaskLevel: ScreenshotMaskLevel = ScreenshotMaskLevel.SensitiveFieldsOnly
    override var screenshotMaskHexColor: String = "#222222"
    override var screenshotCompressionQuality: Int = 25
    override val eventTypeExportAllowList: List<String> = emptyList()
    override val maxEventsInDatabase: Int = 50_000
    override var trackHttpHeaders: Boolean = false
    override var trackHttpBody: Boolean = false
    override var httpHeadersBlocklist: List<String> = emptyList()
    override var httpUrlBlocklist: List<String> = emptyList()
    override var httpUrlAllowlist: List<String> = emptyList()
    override var trackActivityIntentData: Boolean = false
    override var sessionSamplingRate: Float = 1.0f
    override var maxAttachmentSizeInEventsBatchInBytes: Int = 3
    override var eventsBatchingIntervalMs: Long = 30_000 // 30 seconds
    override var maxEventsInBatch: Int = 100
    override var httpContentTypeAllowlist: List<String> = emptyList()
    override var defaultHttpHeadersBlocklist: List<String> = emptyList()
    override var sessionEndLastEventThresholdMs: Long = 20 * 60 * 1000 // 20 minutes
    override var maxSessionDurationMs: Long = 6 * 60 * 60 * 1000 // 6 hours
    override var maxUserDefinedAttributeKeyLength: Int = 64
    override var maxUserDefinedAttributeValueLength: Int = 256
    override var userDefinedAttributeKeyWithSpaces: Boolean = false

    var shouldTrackHttpBody = false
    override fun shouldTrackHttpBody(url: String, contentType: String?): Boolean {
        return shouldTrackHttpBody
    }

    var shouldTrackHttpUrl = false
    override fun shouldTrackHttpUrl(url: String): Boolean {
        return shouldTrackHttpUrl
    }

    var headerKeysToBlock = emptyList<String>()
    override fun shouldTrackHttpHeader(key: String): Boolean {
        return !headerKeysToBlock.any { key.contains(it, ignoreCase = true) }
    }

    override fun setMeasureUrl(url: String) {
        // no-op
    }
}

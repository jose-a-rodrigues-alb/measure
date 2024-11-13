package sh.measure.android

import sh.measure.android.exporter.PeriodicExporter

internal class NoopPeriodicExporter : PeriodicExporter {
    override fun onAppForeground() {
        // No-op
    }

    override fun onAppBackground() {
        // No-op
    }

    override fun onColdLaunch() {
        // No-op
    }
}

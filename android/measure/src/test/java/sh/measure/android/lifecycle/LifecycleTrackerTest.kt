package sh.measure.android.lifecycle

import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.view.ViewTreeObserver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import curtains.onContentChangedListeners
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import sh.measure.android.TestLifecycleActivity
import sh.measure.android.applaunch.LaunchState
import sh.measure.android.attributes.SpanConstant
import sh.measure.android.fakes.TestData
import sh.measure.android.tracing.TestTracer
import sh.measure.android.utils.AndroidTimeProvider
import sh.measure.android.utils.TestClock

@RunWith(AndroidJUnit4::class)
class LifecycleTrackerTest {
    private val application = InstrumentationRegistry.getInstrumentation().context as Application
    private val testClock = TestClock.create()
    private val timeProvider = AndroidTimeProvider(testClock)
    private val internalTracer = TestTracer(timeProvider)
    private val lifecycleTracker = LifecycleTracker(application, timeProvider, internalTracer)
    private val activityController: ActivityController<TestLifecycleActivity> =
        Robolectric.buildActivity(TestLifecycleActivity::class.java)

    @Before
    fun setup() {
        LaunchState.contentLoaderAttachUptime = timeProvider.millisTime - 5000
    }

    @Test
    fun `tracks app startup span`() {
        lifecycleTracker.register()

        // creates app-startup span on initialization
        internalTracer.assertSpanStarted(SpanConstant.APP_STARTUP)

        activityController.setup()
        forceNextDraw()
        lifecycleTracker.onClick(TestData.getClickData())

        // ends app-startup span on first interaction
        internalTracer.assertSpanEnded(SpanConstant.APP_STARTUP)
    }

    @Test
    fun `tracks cold_launch ttid span`() {
        LaunchState.processImportanceOnInit = RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        lifecycleTracker.register()
        activityController.setup()
        forceNextDraw()
        internalTracer.assertSpanEnded(SpanConstant.COLD_LAUNCH_TTID)
    }

    @Test
    fun `tracks lukewarm warm_launch ttid span`() {
        lifecycleTracker.register()
        LaunchState.processImportanceOnInit = RunningAppProcessInfo.IMPORTANCE_CACHED
        activityController.setup()
        forceNextDraw()
        internalTracer.assertSpanEnded(SpanConstant.WARM_LAUNCH_TTID)
    }

    @Test
    fun `tracks hot_launch ttid span`() {
        lifecycleTracker.register()
        LaunchState.processImportanceOnInit = RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        // trigger cold launch
        activityController.setup()
        forceNextDraw()

        // move app to background
        val savedState = Bundle()
        activityController.pause().saveInstanceState(savedState).stop()

        // move app to foreground
        activityController.restart().restoreInstanceState(savedState).resume()
        forceNextDraw()

        internalTracer.assertSpanEnded(SpanConstant.HOT_LAUNCH_TTID)
    }

    @Test
    fun `tracks cold_launch ttfi span`() {
        LaunchState.processImportanceOnInit = RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        lifecycleTracker.register()
        activityController.setup()
        forceNextDraw()
        lifecycleTracker.onClick(TestData.getClickData())
        internalTracer.assertSpanEnded(SpanConstant.COLD_LAUNCH_TTFI)
    }

    @Test
    fun `tracks lukewarm warm_launch ttfi span`() {
        lifecycleTracker.register()
        LaunchState.processImportanceOnInit = RunningAppProcessInfo.IMPORTANCE_CACHED
        activityController.setup()
        forceNextDraw()
        lifecycleTracker.onClick(TestData.getClickData())
        internalTracer.assertSpanEnded(SpanConstant.WARM_LAUNCH_TTFI)
    }

    @Test
    fun `tracks hot_launch ttfi span`() {
        lifecycleTracker.register()
        LaunchState.processImportanceOnInit = RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        // trigger cold launch
        activityController.setup()
        forceNextDraw()

        // move app to background
        val savedState = Bundle()
        activityController.pause().saveInstanceState(savedState).stop()

        // move app to foreground
        activityController.restart().restoreInstanceState(savedState).resume()
        forceNextDraw()
        lifecycleTracker.onClick(TestData.getClickData())
        internalTracer.assertSpanEnded(SpanConstant.HOT_LAUNCH_TTFI)
    }

    @Test
    fun `tracks current_activity span`() {
        lifecycleTracker.register()
        activityController.setup()
        internalTracer.assertSpanStarted(SpanConstant.CURRENT_ACTIVITY)
        activityController.pause()
        internalTracer.assertSpanEnded(SpanConstant.CURRENT_ACTIVITY)
    }

    private fun forceNextDraw() {
        activityController.get().window.onContentChangedListeners.forEach { it.onContentChanged() }
        activityController.get().window.decorView.viewTreeObserver.getOnDrawListeners()?.forEach {
            it.onDraw()
        }
        // wait for looper to be idle
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun ViewTreeObserver.getOnDrawListeners(): ArrayList<ViewTreeObserver.OnDrawListener>? {
        return try {
            // Get the private field
            val field = ViewTreeObserver::class.java.getDeclaredField("mOnDrawListeners")
            // Make it accessible
            field.isAccessible = true
            // Get the value
            @Suppress("UNCHECKED_CAST")
            field.get(this)
                as? ArrayList<ViewTreeObserver.OnDrawListener>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

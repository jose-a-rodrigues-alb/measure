package sh.measure.android.storage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import sh.measure.android.appexit.AppExit
import sh.measure.android.applaunch.ColdLaunchData
import sh.measure.android.applaunch.HotLaunchData
import sh.measure.android.applaunch.WarmLaunchData
import sh.measure.android.events.Event
import sh.measure.android.events.EventType
import sh.measure.android.exceptions.ExceptionData
import sh.measure.android.gestures.ClickData
import sh.measure.android.gestures.LongClickData
import sh.measure.android.gestures.ScrollData
import sh.measure.android.lifecycle.ActivityLifecycleData
import sh.measure.android.lifecycle.ApplicationLifecycleData
import sh.measure.android.lifecycle.FragmentLifecycleData
import sh.measure.android.navigation.NavigationData
import sh.measure.android.navigation.ScreenViewData
import sh.measure.android.networkchange.NetworkChangeData
import sh.measure.android.okhttp.HttpData
import sh.measure.android.performance.CpuUsageData
import sh.measure.android.performance.MemoryUsageData
import sh.measure.android.performance.TrimMemoryData
import sh.measure.android.utils.toJsonElement

/**
 * Serializes the attributes of the event to a JSON string.
 */
internal fun <T> Event<T>.serializeAttributes(): String? {
    if (attributes.isEmpty()) {
        return null
    }
    val result = Json.encodeToString(
        JsonElement.serializer(),
        attributes.toJsonElement(),
    )
    return result
}

/**
 *
 */
internal fun <T> Event<T>.serializeUserDefinedAttributes(): String? {
    if (userDefinedAttributes.isEmpty()) {
        return null
    }
    val result = Json.encodeToString(
        JsonElement.serializer(),
        userDefinedAttributes.toJsonElement(),
    )
    return result
}

/**
 * Serializes the event data to a JSON string.
 */
internal fun <T> Event<T>.serializeDataToString(): String {
    return when (type) {
        EventType.EXCEPTION -> {
            Json.encodeToString(ExceptionData.serializer(), data as ExceptionData)
        }

        EventType.ANR -> {
            Json.encodeToString(ExceptionData.serializer(), data as ExceptionData)
        }

        EventType.APP_EXIT -> {
            Json.encodeToString(AppExit.serializer(), data as AppExit)
        }

        EventType.CLICK -> {
            Json.encodeToString(ClickData.serializer(), data as ClickData)
        }

        EventType.LONG_CLICK -> {
            Json.encodeToString(LongClickData.serializer(), data as LongClickData)
        }

        EventType.SCROLL -> {
            Json.encodeToString(ScrollData.serializer(), data as ScrollData)
        }

        EventType.LIFECYCLE_ACTIVITY -> {
            Json.encodeToString(
                ActivityLifecycleData.serializer(),
                data as ActivityLifecycleData,
            )
        }

        EventType.LIFECYCLE_FRAGMENT -> {
            Json.encodeToString(
                FragmentLifecycleData.serializer(),
                data as FragmentLifecycleData,
            )
        }

        EventType.LIFECYCLE_APP -> {
            Json.encodeToString(
                ApplicationLifecycleData.serializer(),
                data as ApplicationLifecycleData,
            )
        }

        EventType.COLD_LAUNCH -> {
            Json.encodeToString(ColdLaunchData.serializer(), data as ColdLaunchData)
        }

        EventType.WARM_LAUNCH -> {
            Json.encodeToString(WarmLaunchData.serializer(), data as WarmLaunchData)
        }

        EventType.HOT_LAUNCH -> {
            Json.encodeToString(HotLaunchData.serializer(), data as HotLaunchData)
        }

        EventType.NETWORK_CHANGE -> {
            Json.encodeToString(NetworkChangeData.serializer(), data as NetworkChangeData)
        }

        EventType.HTTP -> {
            Json.encodeToString(HttpData.serializer(), data as HttpData)
        }

        EventType.MEMORY_USAGE -> {
            Json.encodeToString(MemoryUsageData.serializer(), data as MemoryUsageData)
        }

        EventType.TRIM_MEMORY -> {
            Json.encodeToString(TrimMemoryData.serializer(), data as TrimMemoryData)
        }

        EventType.CPU_USAGE -> {
            Json.encodeToString(CpuUsageData.serializer(), data as CpuUsageData)
        }

        EventType.NAVIGATION -> {
            Json.encodeToString(NavigationData.serializer(), data as NavigationData)
        }

        EventType.SCREEN_VIEW -> {
            Json.encodeToString(ScreenViewData.serializer(), data as ScreenViewData)
        }

        else -> {
            throw IllegalArgumentException("Unknown event type: $type")
        }
    }
}

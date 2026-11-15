package com.nuraya.app.qa

import android.app.Activity
import android.view.View
import org.robolectric.util.ReflectionHelpers

/** Include Compose's non-modal Popup window in host screenshots. */
internal fun hostWindow(activity: Activity): View {
    val popup = runCatching {
        val global = ReflectionHelpers.callStaticMethod<Any>(Class.forName("android.view.WindowManagerGlobal"), "getInstance")
        ReflectionHelpers.getField<List<View>>(global, "mViews").lastOrNull {
            it.javaClass.name.contains("PopupLayout") && it.isShown && it.width > 0 && it.height > 0
        }
    }.getOrNull()
    return popup ?: org.robolectric.shadows.ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }?.window?.decorView
        ?: activity.window.decorView
}

package com.qomg.webappcatalogs.webapp

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object WebAppShortcuts {
    private const val SCHEME = "webapp"

    /** Each app gets a distinct data URI, so documentLaunchMode gives it its own task in recents. */
    fun uri(appId: Long): Uri = Uri.parse("$SCHEME://app/$appId")

    fun appIdFrom(intent: Intent): Long? =
        intent.data?.takeIf { it.scheme == SCHEME }?.lastPathSegment?.toLongOrNull()

    fun launchIntent(context: Context, appId: Long): Intent =
        Intent(context, WebAppActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(uri(appId))

    private fun shortcutId(appId: Long) = "webapp_$appId"

    /** Asks the launcher to pin a shortcut. Returns false if the launcher does not support it. */
    fun requestPin(context: Context, app: WebApp, icon: Bitmap): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId(app.id))
            .setShortLabel(app.name)
            .setIcon(IconCompat.createWithBitmap(icon))
            .setIntent(launchIntent(context, app.id))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    fun disable(context: Context, appId: Long, message: CharSequence) {
        ShortcutManagerCompat.disableShortcuts(context, listOf(shortcutId(appId)), message)
    }
}

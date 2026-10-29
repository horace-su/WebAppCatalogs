package com.qomg.webappcatalogs.webapp

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.qomg.webappcatalogs.api.AppLogger

/**
 * Gives every web app its own WebView profile (cookies, storage, cache), so two apps can be
 * signed in to different accounts of the same site. Falls back to the shared default profile
 * on WebView versions without multi-profile support. All calls must be on the UI thread.
 */
object WebAppProfiles {
    private const val TAG = "WebAppProfiles"

    val isSupported: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)

    private fun name(appId: Long) = "webapp_$appId"

    /** Must be called before [webView] is used for anything else. Returns the cookie manager in effect. */
    fun attach(webView: WebView, appId: Long): CookieManager {
        if (!isSupported) return CookieManager.getInstance()
        val profileName = name(appId)
        ProfileStore.getInstance().getOrCreateProfile(profileName)
        WebViewCompat.setProfile(webView, profileName)
        return WebViewCompat.getProfile(webView).cookieManager
    }

    fun delete(appId: Long) {
        if (!isSupported) return
        try {
            ProfileStore.getInstance().deleteProfile(name(appId))
        } catch (e: IllegalStateException) {
            // Still held by a WebView that has not been destroyed yet; its data stays on disk.
            AppLogger.w(TAG, "Profile for app $appId is in use, not deleted", e)
        }
    }
}

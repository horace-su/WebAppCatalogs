package com.qomg.webappcatalogs.webapp

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.qomg.webappcatalogs.R
import com.qomg.webappcatalogs.api.AppLogger
import com.qomg.webappcatalogs.ui.theme.WebAppCatalogsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hosts a single web app in a full-screen WebView, in its own task with its own WebView profile. */
class WebAppActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var cookieManager: CookieManager
    private lateinit var repository: WebAppRepository
    private var appId = 0L

    private var app by mutableStateOf<WebApp?>(null)
    private var pageTitle by mutableStateOf("")
    private var loadProgress by mutableIntStateOf(0)
    private var fullscreenView by mutableStateOf<View?>(null)
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            fileChooserCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
            fileChooserCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = WebAppShortcuts.appIdFrom(intent)
        if (id == null) {
            finish()
            return
        }
        appId = id
        repository = WebAppRepository(this)
        enableEdgeToEdge()

        webView = WebView(this)
        cookieManager = WebAppProfiles.attach(webView, appId)
        configureWebView()

        onBackPressedDispatcher.addCallback(this) { handleBack() }
        setContent {
            WebAppCatalogsTheme {
                WebAppScreen()
            }
        }

        lifecycleScope.launch {
            val loaded = repository.get(appId)
            if (loaded == null) {
                Toast.makeText(this@WebAppActivity, R.string.webapp_not_found, Toast.LENGTH_SHORT).show()
                finishAndRemoveTask()
                return@launch
            }
            app = loaded
            pageTitle = loaded.name
            updateTaskDescription(loaded)
            repository.markOpened(appId)
            if (savedInstanceState?.let { webView.restoreState(it) } == null) {
                webView.loadUrl(loaded.url)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            // target=_blank links open in this same view instead of being dropped
            setSupportMultipleWindows(false)
            // Some sites (e.g. Google sign-in) refuse to work inside a WebView that announces itself
            userAgentString = userAgentString.replace("; wv", "")
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, emptySet())
        }
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme
                if (scheme == "http" || scheme == "https") return false
                openExternally(request.url)
                return true
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                loadProgress = newProgress
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                if (!title.isNullOrBlank()) pageTitle = title
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
                val copy = icon?.copy(Bitmap.Config.ARGB_8888, false) ?: return
                lifecycleScope.launch {
                    val changed = withContext(Dispatchers.IO) {
                        WebAppIcons.save(this@WebAppActivity, appId, copy)
                    }
                    if (changed) app?.let { updateTaskDescription(it) }
                }
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                fullscreenView = view
                fullscreenCallback = callback
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }

            override fun onHideCustomView() {
                exitFullscreen()
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                return try {
                    fileChooser.launch(fileChooserParams.createIntent())
                    true
                } catch (e: ActivityNotFoundException) {
                    fileChooserCallback = null
                    false
                }
            }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            download(url, userAgent, contentDisposition, mimeType)
        }
    }

    private fun exitFullscreen() {
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun handleBack() {
        when {
            fullscreenView != null -> exitFullscreen()
            webView.canGoBack() -> webView.goBack()
            // Keep the page alive in the background, like a native app
            else -> moveTaskToBack(true)
        }
    }

    /** Hands non-web links (intent:, mailto:, tel:, market: ...) to other apps. */
    private fun openExternally(uri: Uri) {
        val intent = if (uri.scheme == "intent") {
            try {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
                    // Never let a page target a specific component, including our own non-browsable ones
                    component = null
                    selector = null
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Bad intent URI: $uri", e)
                return
            }
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val fallback = intent.getStringExtra("browser_fallback_url")
            if (fallback != null && URLUtil.isNetworkUrl(fallback)) {
                webView.loadUrl(fallback)
            } else {
                Toast.makeText(this, R.string.webapp_no_handler, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openInBrowser(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.webapp_no_handler, Toast.LENGTH_SHORT).show()
        }
    }

    private fun download(url: String, userAgent: String, contentDisposition: String?, mimeType: String?) {
        if (!URLUtil.isNetworkUrl(url)) {
            // blob: and data: downloads only exist inside the page and cannot go through DownloadManager
            Toast.makeText(this, R.string.webapp_download_unsupported, Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType(mimeType)
            .addRequestHeader("User-Agent", userAgent)
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        cookieManager.getCookie(url)?.let { request.addRequestHeader("Cookie", it) }
        try {
            getSystemService<DownloadManager>()!!.enqueue(request)
            Toast.makeText(this, getString(R.string.webapp_download_started, fileName), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Download failed: $url", e)
            Toast.makeText(this, R.string.webapp_download_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pinToHome(app: WebApp) {
        lifecycleScope.launch {
            val icon = withContext(Dispatchers.IO) { WebAppIcons.shortcutIcon(this@WebAppActivity, app) }
            if (!WebAppShortcuts.requestPin(this@WebAppActivity, app, icon)) {
                Toast.makeText(this@WebAppActivity, R.string.webapp_pin_unsupported, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTaskDescription(app: WebApp) {
        lifecycleScope.launch {
            val icon = withContext(Dispatchers.IO) { WebAppIcons.shortcutIcon(this@WebAppActivity, app) }
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription(app.name, icon))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun WebAppScreen() {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(pageTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            IconButton(onClick = ::handleBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.webapp_back))
                            }
                        },
                        actions = {
                            IconButton(onClick = { webView.reload() }) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.webapp_reload))
                            }
                            WebAppMenu()
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                        .fillMaxSize()
                ) {
                    AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
                    if (loadProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { loadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }
                }
            }
            fullscreenView?.let { view ->
                AndroidView(
                    factory = { context ->
                        FrameLayout(context).apply { setBackgroundColor(android.graphics.Color.BLACK) }
                    },
                    update = { container ->
                        if (container.getChildAt(0) !== view) {
                            container.removeAllViews()
                            (view.parent as? ViewGroup)?.removeView(view)
                            container.addView(view, FrameLayout.LayoutParams(-1, -1))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @Composable
    private fun WebAppMenu() {
        var expanded by remember { mutableStateOf(false) }
        val clipboard = LocalClipboardManager.current
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.webapp_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.webapp_forward)) },
                enabled = webView.canGoForward(),
                onClick = { expanded = false; webView.goForward() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.webapp_home)) },
                onClick = { expanded = false; app?.let { webView.loadUrl(it.url) } }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.webapp_copy_link)) },
                onClick = {
                    expanded = false
                    webView.url?.let { clipboard.setText(AnnotatedString(it)) }
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.webapp_open_in_browser)) },
                onClick = { expanded = false; webView.url?.let(::openInBrowser) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.webapp_add_to_home)) },
                onClick = { expanded = false; app?.let(::pinToHome) }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            webView.onPause()
            cookieManager.flush()
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }

    private companion object {
        const val TAG = "WebAppActivity"
    }
}

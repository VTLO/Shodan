package io.cybersnippets.shodan

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.runBlocking

/**
 * Integrated browser that opens a finding's URL in a WebView. The configured
 * user-agent preset (Settings) is applied automatically on launch, and can be
 * switched live from the globe menu. The URL comes in via [EXTRA_URL].
 */
class BrowserActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"

        fun intent(context: Context, url: String): Intent =
            Intent(context, BrowserActivity::class.java).putExtra(EXTRA_URL, url)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        // Automated user-agent setup: resolve the saved preference once here.
        val initialUa = runBlocking { ApiKeyStore(applicationContext).currentUserAgent() }

        setContent {
            MaterialTheme(colorScheme = ShodanColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrowserScreen(
                        startUrl = startUrl,
                        initialPreset = initialUa.preset,
                        customUa = initialUa.custom,
                        onClose = { finish() },
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun BrowserScreen(
    startUrl: String,
    initialPreset: UserAgentPreset,
    customUa: String,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf(startUrl) }
    var pageTitle by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    var preset by remember { mutableStateOf(initialPreset) }
    var uaMenuOpen by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                UserAgentPreset.resolve(initialPreset, customUa)?.let { userAgentString = it }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    // Keep http/https navigation inside the WebView.
                    val scheme = request.url.scheme?.lowercase()
                    return if (scheme == "http" || scheme == "https") {
                        false
                    } else {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        }
                        true
                    }
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    loading = true
                    url?.let { currentUrl = it }
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    loading = false
                    url?.let { currentUrl = it }
                    canGoBack = view.canGoBack()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress / 100f
                }

                override fun onReceivedTitle(view: WebView, title: String?) {
                    pageTitle = title.orEmpty()
                }
            }
            loadUrl(startUrl)
        }
    }

    BackHandler(enabled = canGoBack) {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            pageTitle.ifBlank { "Finding" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            currentUrl,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { uaMenuOpen = true }) {
                        Icon(Icons.Filled.Language, contentDescription = "User agent")
                    }
                    DropdownMenu(expanded = uaMenuOpen, onDismissRequest = { uaMenuOpen = false }) {
                        UserAgentPreset.entries
                            // "Custom…" only applies if a custom string is configured.
                            .filter { it != UserAgentPreset.CUSTOM || customUa.isNotBlank() }
                            .forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        val mark = if (option == preset) "✓ " else ""
                                        Text("$mark${option.label}")
                                    },
                                    onClick = {
                                        preset = option
                                        uaMenuOpen = false
                                        val ua = UserAgentPreset.resolve(option, customUa)
                                        webView.settings.userAgentString = ua
                                        webView.reload()
                                    },
                                )
                            }
                    }
                    IconButton(onClick = { webView.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload")
                    }
                    IconButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)),
                            )
                        }
                    }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open externally")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                if (loading) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

package com.ipvio.stream

import android.webkit.ConsoleMessage
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Trailer inside the app (#15). Full screen; Back or the dialog close ends it and destroys the WebView, so no sound
 * keeps playing. Only the embed page is shown: navigation of the main frame is blocked.
 */
@Composable
fun TrailerDialog(key: String?, onClose: () -> Unit, onOpenExternal: () -> Unit = {}) {
    val html = TrailerLogic.embedHtml(key) ?: return
    val holder = remember { arrayOfNulls<WebView>(1) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        DisposableEffect(html) {
            onDispose {
                holder[0]?.let { w -> w.stopLoading(); w.loadUrl("about:blank"); w.destroy() }
                holder[0] = null
            }
        }
        // On Android TV the remote only reaches Compose views that have focus: the WebView must not take it,
        // and the "Im YouTube öffnen" button gets focus when the dialog opens (19 Nov feedback: button not clickable)
        val buttonFocus = remember { FocusRequester() }
        LaunchedEffect(html) { delay(200); runCatching { buttonFocus.requestFocus() } }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        isFocusable = false
                        isFocusableInTouchMode = false
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                                request?.isForMainFrame != false
                            // Diagnostics for the black-picture trailers (T-032): codes only, no addresses
                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                // Also errors of the YouTube iframe (T-032, 20 Nov): they were hidden before
                                Diag.report("Trailer: WebView-Fehler ${error?.errorCode} (Hauptseite=${request?.isForMainFrame != false})")
                            }
                            // Same behaviour as the default (connection is refused); only reports the reason
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                Diag.report("Trailer: SSL-Fehler ${error?.primaryError}")
                                handler?.cancel()
                            }
                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                Diag.report("Trailer: HTTP ${errorResponse?.statusCode} (Hauptseite=${request?.isForMainFrame == true})")
                            }
                        }
                        // Player errors/states of the embed page (T-032); only lines of TrailerLogic.diagLine pass
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(m: ConsoleMessage?): Boolean {
                                TrailerLogic.diagLine(m?.message())?.let { Diag.report(it) }
                                return true
                            }
                        }
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "utf-8", null)
                        holder[0] = this
                    }
                },
            )
            // Fallback (19 Nov feedback: black picture, no sound on some titles): the same trailer in YouTube
            PillButton(Lang.tr("Im YouTube öffnen"), modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).focusRequester(buttonFocus), light = true) { onOpenExternal() }
        }
    }
}

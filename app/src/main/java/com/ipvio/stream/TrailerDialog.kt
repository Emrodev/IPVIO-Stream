package com.ipvio.stream

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                                request?.isForMainFrame != false
                        }
                        loadDataWithBaseURL("https://www.youtube-nocookie.com", html, "text/html", "utf-8", null)
                        holder[0] = this
                    }
                },
            )
            // Fallback (19 Nov feedback: black picture, no sound on some titles): the same trailer in YouTube
            PillButton(Lang.tr("Im YouTube öffnen"), modifier = Modifier.align(Alignment.TopEnd).padding(24.dp), light = true) { onOpenExternal() }
        }
    }
}

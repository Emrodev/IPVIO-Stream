package com.ipvio.stream

import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

/** Time a way gets to report "playing" before the next way is tried (same rule as the trailer dialog, T-032). */
private const val PREVIEW_ATTEMPT_MS = 10_000L

/** After "playing", wait a moment before the hole opens: YouTube may still show a spinner or pause symbol. */
private const val REVEAL_DELAY_MS = 800L

/**
 * The WebView is taller than the visible place: the 16:9 video fills the place and YouTube's own overlays (title bar at
 * the top, logo and "More videos" at the bottom) lie in the extra space above and below, which the page covers.
 */
private const val EXTRA_HEIGHT = 0.6f

private fun activityOf(c: Context): ComponentActivity? =
    generateSequence(c) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<ComponentActivity>().firstOrNull()

/** Place of the trailer (root coordinates) while it plays under the page; backgrounds leave it out. */
object TrailerHole {
    var rect by mutableStateOf<Rect?>(null)
}

/** Background in [color] with a hole where the trailer plays under the page ([TrailerHole]). */
fun Modifier.holeBackground(color: Color): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    this.onGloballyPositioned { origin = it.positionInRoot() }
        .drawBehind {
            val h = TrailerHole.rect
            if (h == null) drawRect(color)
            else clipRect(h.left - origin.x, h.top - origin.y, h.right - origin.x, h.bottom - origin.y, ClipOp.Difference) { drawRect(color) }
        }
}

/**
 * Trailer behind the header, like Prime Video: starts [TrailerLogic.PREVIEW_DELAY_MS] after [active] turns true.
 *
 * The WebView is a plain Android view in the activity's content frame UNDER the Compose view: inside Compose the video
 * stays black on TV devices (Fire TV, Shield: sound only; T-032). While it plays, the app's backgrounds leave a hole at
 * [modifier]'s place ([TrailerHole], [holeBackground]); the picture behind steps aside, and the page's gradients, text and
 * buttons are drawn on top of the video. Until then the page stays as it is.
 *
 * Ends silently: when the trailer is over, when [active] turns false, when the app goes to the background, or when no
 * way plays within 10 s each. Never opens YouTube outside the app and never takes focus.
 */
@Composable
fun TrailerPreview(key: String?, active: Boolean, modifier: Modifier = Modifier, trailerLang: String? = null) {
    if (!TrailerLogic.validKey(key)) return
    val ctx = LocalContext.current
    val activity = remember(ctx) { activityOf(ctx) }
    var resumed by remember { mutableStateOf(true) }
    val lifecycle = activity?.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, ev ->
            when (ev) {
                Lifecycle.Event.ON_PAUSE -> resumed = false
                Lifecycle.Event.ON_RESUME -> resumed = true
                else -> {}
            }
        }
        lifecycle?.addObserver(obs)
        onDispose { lifecycle?.removeObserver(obs) }
    }
    val run = active && resumed
    // A new run (focus back on the header, back from the background) starts the trailer again after the delay
    var started by remember(key, run) { mutableStateOf(false) }
    var attempt by remember(key, run) { mutableStateOf(0) }
    var playing by remember(key, run) { mutableStateOf(false) }
    var shown by remember(key, run) { mutableStateOf(false) }
    var done by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key, run) {
        if (!run || done) return@LaunchedEffect
        delay(TrailerLogic.PREVIEW_DELAY_MS)
        started = true
    }
    LaunchedEffect(key, run, started, attempt) {
        if (!started) return@LaunchedEffect
        delay(PREVIEW_ATTEMPT_MS)
        if (!playing) {
            Diag.report("Trailer preview: way ${attempt + 1} no playback after 10 s")
            if (attempt + 1 < TrailerLogic.ATTEMPTS) attempt++ else done = true
        }
    }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        delay(REVEAL_DELAY_MS)
        shown = true
    }
    if (!run || !started || done) return
    val content = remember(activity) { activity?.findViewById<FrameLayout>(android.R.id.content) } ?: return
    androidx.compose.runtime.key(attempt) {
        val n = attempt
        val holder = remember { arrayOfNulls<WebView>(1) }
        var box by remember { mutableStateOf<Rect?>(null) }
        DisposableEffect(Unit) {
            onDispose {
                TrailerHole.rect = null
                holder[0]?.let { w ->
                    (w.parent as? ViewGroup)?.removeView(w)
                    w.stopLoading(); w.loadUrl("about:blank"); w.destroy()
                }
                holder[0] = null
            }
        }
        // Player in the app language; subtitles (app language preferred) when the trailer is in another language
        val ui = Lang.code()
        val way = TrailerLogic.attempt(n, key, preview = true, lang = TrailerLogic.langParams(ui, TrailerLogic.wantCaptions(trailerLang, ui)))
        if (way != null) {
            Spacer(modifier.onGloballyPositioned { lc ->
                box = lc.boundsInRoot()
                val b = lc.boundsInWindow()
                val loc = IntArray(2).also { content.getLocationInWindow(it) }
                val extra = (b.height * EXTRA_HEIGHT).toInt()
                val lp = FrameLayout.LayoutParams(b.width.toInt(), b.height.toInt() + extra).apply {
                    leftMargin = b.left.toInt() - loc[0]; topMargin = b.top.toInt() - loc[1] - extra / 2
                }
                val w = holder[0] ?: WebView(content.context).apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                            request?.isForMainFrame != false
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(m: ConsoleMessage?): Boolean {
                            val msg = m?.message()
                            TrailerLogic.diagLine(msg)?.let { Diag.report("$it (preview way ${n + 1})") }
                            if (TrailerLogic.isPlaying(msg)) playing = true
                            // Over, or YouTube forbids embedding: back to the still picture
                            if (TrailerLogic.isEnded(msg) || TrailerLogic.isVideoBlocked(TrailerLogic.playerError(msg))) done = true
                            return true
                        }
                    }
                    loadDataWithBaseURL(way.baseUrl, way.html, "text/html", "utf-8", null)
                    holder[0] = this
                    // Index 0: under the Compose view, so the page is drawn on top
                    content.addView(this, 0, lp)
                }
                val old = w.layoutParams as? FrameLayout.LayoutParams
                if (old != null && (old.width != lp.width || old.height != lp.height || old.leftMargin != lp.leftMargin || old.topMargin != lp.topMargin)) w.layoutParams = lp
            })
            // Read in composition (not only inside SideEffect), so a change of "shown" or of the place recomposes and the hole follows
            val hole = if (shown) box else null
            SideEffect { TrailerHole.rect = hole }
            LaunchedEffect(hole != null) { if (hole != null) Diag.report("Trailer preview: picture shown (way ${n + 1})") }
        }
    }
}

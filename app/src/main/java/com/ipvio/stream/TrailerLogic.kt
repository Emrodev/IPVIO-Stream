package com.ipvio.stream

/**
 * Trailer played inside the app (#15): a YouTube video id from TMDB is embedded in a WebView page. The id is checked
 * before it goes into the HTML, so no other text can get in. Pure logic, no Android, testable.
 */
object TrailerLogic {
    private val ID = Regex("^[A-Za-z0-9_-]{6,20}$")

    /** A YouTube video id as TMDB gives it (letters, digits, - and _). */
    fun validKey(key: String?): Boolean = key != null && ID.matches(key)

    /** Page for the WebView, or null if the id is not valid. Uses the privacy-enhanced embed host. */
    fun embedHtml(key: String?): String? {
        if (!validKey(key)) return null
        return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
            "<style>html,body{margin:0;height:100%;background:#000}iframe{border:0;width:100%;height:100%}</style></head>" +
            "<body><iframe id=\"yt\" src=\"https://www.youtube-nocookie.com/embed/$key?autoplay=1&playsinline=1&rel=0" +
            "&enablejsapi=1&origin=https://www.youtube-nocookie.com\" " +
            "allow=\"autoplay; encrypted-media; picture-in-picture\" allowfullscreen></iframe>" +
            "<script>$DIAG_SCRIPT</script></body></html>"
    }

    /**
     * Diagnostics only (T-032): asks the YouTube player to report its state and errors and writes them to the WebView
     * console as `IPVIO_TRAILER_ERR <code>` / `IPVIO_TRAILER_STATE <code>`; after 15 s without playback it writes
     * `IPVIO_TRAILER_TIMEOUT`. The app reads these lines in `diagLine`. No bridge object is exposed to the page.
     */
    private const val DIAG_SCRIPT =
        "var f=document.getElementById('yt'),ok=false;" +
            "function s(o){try{f.contentWindow.postMessage(JSON.stringify(o),'*')}catch(e){}}" +
            "f.addEventListener('load',function(){console.log('IPVIO_TRAILER_LOAD');s({event:'listening',id:1,channel:'widget'});" +
            "s({event:'command',func:'addEventListener',args:['onError'],id:1,channel:'widget'});" +
            "s({event:'command',func:'addEventListener',args:['onStateChange'],id:1,channel:'widget'})});" +
            "window.addEventListener('message',function(e){if(String(e.origin).indexOf('youtube')<0)return;var d;" +
            "try{d=JSON.parse(e.data)}catch(x){return}" +
            "if(d.event==='onError')console.log('IPVIO_TRAILER_ERR '+parseInt(d.info,10));" +
            "else if(d.event==='onStateChange'){if(d.info===1)ok=true;console.log('IPVIO_TRAILER_STATE '+parseInt(d.info,10))}});" +
            "setTimeout(function(){if(!ok)console.log('IPVIO_TRAILER_TIMEOUT')},15000);"

    private val DIAG_LINE = Regex("^IPVIO_TRAILER_(ERR|STATE) (-?\\d{1,4})$")

    /** Diagnose line for a WebView console message of the embed page, or null for any other message. */
    fun diagLine(console: String?): String? {
        if (console == null) return null
        if (console == "IPVIO_TRAILER_LOAD") return "Trailer: iframe loaded"
        if (console == "IPVIO_TRAILER_TIMEOUT") return "Trailer: no playback after 15 s"
        val m = DIAG_LINE.matchEntire(console) ?: return null
        val code = m.groupValues[2]
        return if (m.groupValues[1] == "ERR") "Trailer: player error $code" else "Trailer: player state $code"
    }
}

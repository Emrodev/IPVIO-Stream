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
            "<body><iframe src=\"https://www.youtube-nocookie.com/embed/$key?autoplay=1&playsinline=1&rel=0\" " +
            "allow=\"autoplay; encrypted-media; picture-in-picture\" allowfullscreen></iframe></body></html>"
    }
}

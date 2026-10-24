package com.tritv.photo.data

/**
 * A Synology Photos public share link, such as `http://192.168.50.94:5000/mo/sharing/tZ3xYyQ5C`.
 * [baseUrl] is the DSM address without a trailing slash; [passphrase] is the share's code.
 */
data class ShareLink(val baseUrl: String, val passphrase: String) {

    /** The page that hands out the share's session cookie. */
    val sharingPage: String get() = "$baseUrl$pathPrefix/mo/sharing/$passphrase"

    /** DSM 7.2 serves Photos under /photo as well; keep whichever prefix the link used. */
    var pathPrefix: String = ""
        private set

    override fun toString() = sharingPage

    companion object {
        private val PATTERN = Regex(
            """^(https?)://([^/\s?#]+)(/photo)?/mo/sharing/([A-Za-z0-9_-]+)""",
            RegexOption.IGNORE_CASE,
        )

        /**
         * Reads a link as typed on the remote: surrounding spaces are ignored, `http://` is assumed
         * when the scheme is left out, and anything after the code (`/`, `?…`, `#…`) is dropped.
         * Returns null when the text is not a share link.
         */
        fun parse(text: String): ShareLink? {
            var s = text.trim()
            if (s.isEmpty()) return null
            if (!s.contains("://")) s = "http://$s"
            val m = PATTERN.find(s) ?: return null
            val (scheme, host, prefix, code) = m.destructured
            return ShareLink("${scheme.lowercase()}://$host", code).also { it.pathPrefix = prefix }
        }
    }
}

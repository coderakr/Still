package com.sandboxuser.distractfree.blocking

import java.net.IDN
import java.net.URI
import java.util.Locale

enum class FeedRule(val title: String, val subtitle: String, val packages: Set<String>) {
    YOUTUBE("YouTube Shorts", "Keep videos. Skip the endless shorts.", setOf("com.google.android.youtube")),
    INSTAGRAM("Instagram Reels", "Keep connections. Leave the loop.", setOf("com.instagram.android")),
    FACEBOOK("Facebook Reels", "A little less autoplay in your day.", setOf("com.facebook.katana")),
    TIKTOK("TikTok", "Block the entire app and its feeds.", setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"))
}

/** Only known viewer surfaces or a selected feed tab qualify. An ordinary tab label doesn't. */
data class ScreenNode(val id: String = "", val text: String = "", val description: String = "", val selected: Boolean = false)

object BlockingRules {
    val browserAddressIds = mapOf(
        "com.android.chrome" to setOf("url_bar"),
        "com.chrome.beta" to setOf("url_bar"),
        "com.brave.browser" to setOf("url_bar"),
        "com.microsoft.emmx" to setOf("url_bar"),
        "org.mozilla.firefox" to setOf("mozac_browser_toolbar_url_view", "url_bar_title"),
        "com.sec.android.app.sbrowser" to setOf("location_bar_edit_text", "location_bar_text"),
        "com.opera.browser" to setOf("url_field")
    )

    fun detectedFeed(pkg: String, nodes: List<ScreenNode>, enabled: Set<FeedRule>): FeedRule? {
        val rule = enabled.firstOrNull { pkg in it.packages } ?: return null
        if (rule == FeedRule.TIKTOK) return rule
        val viewerIds = when (rule) {
            FeedRule.YOUTUBE -> setOf("reel_recycler", "reel_watch_player", "shorts_player", "reel_player_page_container")
            FeedRule.INSTAGRAM -> setOf("clips_viewer_view_pager", "clips_viewer_root", "clips_viewer_video_layout", "clips_viewer_container")
            FeedRule.FACEBOOK -> setOf("reels_viewer", "reels_video_container", "reels_viewer_container")
            else -> emptySet()
        }
        val label = if (rule == FeedRule.YOUTUBE) "shorts" else "reels"
        return rule.takeIf {
            nodes.any { node ->
                node.id.substringAfterLast('/') in viewerIds ||
                    (node.selected && (node.text.equals(label, true) || node.description.equals(label, true)))
            }
        }
    }

    /** Accept a domain or an HTTP(S) URL. Store only a canonical hostname. */
    fun normalizeDomain(input: String): String? {
        val raw = input.trim()
        if (raw.isEmpty() || raw.any { it.isWhitespace() } || raw.contains('\\')) return null
        return try {
            val uri = URI(if (raw.contains("://")) raw else "https://$raw")
            if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https")) return null
            val authority = uri.rawAuthority ?: return null
            if ('@' in authority || '%' in authority) return null
            val hostAndPort = authority.split(':')
            if (hostAndPort.size > 2) return null
            if (hostAndPort.size == 2 && (hostAndPort[1].toIntOrNull() ?: return null) !in 1..65535) return null
            val host = IDN.toASCII(hostAndPort[0].trimEnd('.'), IDN.USE_STD3_ASCII_RULES)
                .lowercase(Locale.ROOT).removePrefix("www.")
            val labels = host.split('.')
            host.takeIf {
                it.length <= 253 && labels.size >= 2 && labels.all { label ->
                    label.isNotEmpty() && label.length <= 63 && !label.startsWith('-') && !label.endsWith('-') &&
                        label.all { c -> c in 'a'..'z' || c in '0'..'9' || c == '-' }
                } && labels.last().any { c -> c in 'a'..'z' }
            }
        } catch (_: Exception) { null }
    }

    fun matchesDomain(host: String, blocked: String): Boolean = host == blocked || host.endsWith(".$blocked")

    fun blockedWebsite(url: String, domains: Set<String>): String? {
        val host = normalizeDomain(url) ?: return null
        return domains.firstOrNull { matchesDomain(host, it) }
    }

    fun shortFormWebsite(url: String, enabled: Set<FeedRule>): FeedRule? {
        val host = normalizeDomain(url) ?: return null
        val path = try { URI(if (url.contains("://")) url else "https://$url").path.orEmpty().lowercase(Locale.ROOT) } catch (_: Exception) { "" }
        return when {
            FeedRule.YOUTUBE in enabled && matchesDomain(host, "youtube.com") && (path == "/shorts" || path.startsWith("/shorts/")) -> FeedRule.YOUTUBE
            FeedRule.INSTAGRAM in enabled && matchesDomain(host, "instagram.com") && (path == "/reels" || path.startsWith("/reels/") || path.startsWith("/reel/")) -> FeedRule.INSTAGRAM
            FeedRule.FACEBOOK in enabled && matchesDomain(host, "facebook.com") && (path == "/reel" || path.startsWith("/reel/") || path == "/reels" || path.startsWith("/reels/")) -> FeedRule.FACEBOOK
            FeedRule.TIKTOK in enabled && matchesDomain(host, "tiktok.com") -> FeedRule.TIKTOK
            else -> null
        }
    }
}

/** Monotonic deadlines resist clock changes. A device restart ends a break safely. */
object BreakClock {
    fun remaining(deadline: Long, savedBoot: Int, currentBoot: Int, elapsed: Long): Long =
        if (savedBoot != currentBoot) 0 else (deadline - elapsed).coerceAtLeast(0)
}

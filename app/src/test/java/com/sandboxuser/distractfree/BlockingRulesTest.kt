package com.sandboxuser.distractfree

import com.sandboxuser.distractfree.blocking.*
import org.junit.Assert.*
import org.junit.Test

class BlockingRulesTest {
    @Test fun normalizesUrlsAndInternationalDomains() {
        assertEquals("reddit.com",BlockingRules.normalizeDomain(" HTTPS://WWW.Reddit.com/r/android?q=test "))
        assertEquals("example.com",BlockingRules.normalizeDomain("example.com.:443/path"))
        assertEquals("xn--bcher-kva.de",BlockingRules.normalizeDomain("bücher.de"))
    }
    @Test fun rejectsInvalidAndAmbiguousInput() {
        listOf("", "hello", "not a website.com", "https://", "ftp://example.com", "user@example.com", "example.com@evil.com", "https://exa_mple.com", "*.example.com", "example.com:70000", "example..com", "https://example.com\\@evil.com", "https://example.com:abc", "https://127.0.0.1").forEach { assertNull(it,BlockingRules.normalizeDomain(it)) }
    }
    @Test fun matchesSubdomainsButNotLookalikes() {
        val blocked=setOf("reddit.com")
        assertEquals("reddit.com",BlockingRules.blockedWebsite("https://old.reddit.com/r/all",blocked))
        assertNull(BlockingRules.blockedWebsite("https://notreddit.com",blocked))
        assertNull(BlockingRules.blockedWebsite("https://reddit.com.evil.com",blocked))
        assertNull(BlockingRules.blockedWebsite("https://example.com/reddit.com",blocked))
    }
    @Test fun ordinaryShortsLabelIsNotAPlayer() {
        assertNull(BlockingRules.detectedFeed("com.google.android.youtube",listOf(ScreenNode(text="Shorts")),FeedRule.entries.toSet()))
        assertNull(BlockingRules.detectedFeed("com.instagram.android",listOf(ScreenNode(description="Reels")),FeedRule.entries.toSet()))
    }
    @Test fun selectedTabsAndKnownViewersAreDetected() {
        assertEquals(FeedRule.YOUTUBE,BlockingRules.detectedFeed("com.google.android.youtube",listOf(ScreenNode(id="com.google.android.youtube:id/reel_recycler")),setOf(FeedRule.YOUTUBE)))
        assertEquals(FeedRule.INSTAGRAM,BlockingRules.detectedFeed("com.instagram.android",listOf(ScreenNode(description="Reels",selected=true)),setOf(FeedRule.INSTAGRAM)))
        assertEquals(FeedRule.FACEBOOK,BlockingRules.detectedFeed("com.facebook.katana",listOf(ScreenNode(id="com.facebook.katana:id/reels_viewer")),setOf(FeedRule.FACEBOOK)))
    }
    @Test fun respectsDisabledRulesAndPackageBoundaries() {
        val nodes=listOf(ScreenNode(id="com.google.android.youtube:id/reel_recycler"))
        assertNull(BlockingRules.detectedFeed("com.google.android.youtube",nodes,emptySet()))
        assertNull(BlockingRules.detectedFeed("some.other.app",nodes,setOf(FeedRule.YOUTUBE)))
        assertEquals(FeedRule.TIKTOK,BlockingRules.detectedFeed("com.zhiliaoapp.musically",emptyList(),setOf(FeedRule.TIKTOK)))
    }
    @Test fun browserShortFormRulesArePathSpecific() {
        val enabled=FeedRule.entries.toSet()
        assertEquals(FeedRule.YOUTUBE,BlockingRules.shortFormWebsite("m.youtube.com/shorts/abc",enabled))
        assertEquals(FeedRule.INSTAGRAM,BlockingRules.shortFormWebsite("instagram.com/reel/abc/",enabled))
        assertEquals(FeedRule.FACEBOOK,BlockingRules.shortFormWebsite("facebook.com/reels/123",enabled))
        assertNull(BlockingRules.shortFormWebsite("youtube.com/watch?v=shorts",enabled))
        assertNull(BlockingRules.shortFormWebsite("youtube.com/shortstory",enabled))
        assertNull(BlockingRules.shortFormWebsite("youtube.com.evil.com/shorts/abc",enabled))
        assertNull(BlockingRules.shortFormWebsite("youtube.com/shorts/abc",emptySet()))
    }
    @Test fun breakExpiresWithoutDependingOnWallClock() {
        assertEquals(30_000L,BreakClock.remaining(60_000,4,4,30_000))
        assertEquals(0L,BreakClock.remaining(60_000,4,4,60_000))
        assertEquals(0L,BreakClock.remaining(60_000,4,4,70_000))
        assertEquals(0L,BreakClock.remaining(60_000,4,5,10_000))
    }
}

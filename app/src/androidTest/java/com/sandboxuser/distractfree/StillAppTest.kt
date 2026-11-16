package com.sandboxuser.distractfree

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.sandboxuser.distractfree.blocking.FeedRule
import com.sandboxuser.distractfree.data.FocusStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StillAppTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val store get() = FocusStore.get(context)

    @Before fun reset() {
        compose.runOnUiThread {
            context.getSharedPreferences("still_preferences",0).edit().clear().commit()
            store.refresh()
        }
    }

    @Test fun websiteCanBeAddedValidatedAndRemoved() {
        compose.onNodeWithText("Blocks",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Websites",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Add a website").performScrollTo().performClick()
        compose.onNodeWithText("Website address").performTextInput("not a domain")
        compose.onNodeWithText("Add to blocklist").performClick()
        compose.onNodeWithText("Enter a valid domain, like reddit.com.").assertIsDisplayed()
        compose.onNodeWithText("Website address").performTextReplacement("https://www.reddit.com/r/android")
        compose.onNodeWithText("Add to blocklist").performClick()
        compose.onNodeWithText("reddit.com").assertIsDisplayed()
        assertTrue(store.state.value.websites.contains("reddit.com"))
        compose.onNodeWithContentDescription("Remove reddit.com").performClick()
        compose.onNodeWithText("Leave the rabbit holes behind.").performScrollTo().assertIsDisplayed()
        assertTrue(store.state.value.websites.isEmpty())
    }

    @Test fun feedTogglePersistsAcrossRecreation() {
        compose.onNodeWithText("Blocks",useUnmergedTree=true).performClick()
        compose.onNodeWithText("YouTube Shorts").performClick()
        compose.runOnIdle { assertFalse(store.state.value.feeds.contains(FeedRule.YOUTUBE)) }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertFalse(store.state.value.feeds.contains(FeedRule.YOUTUBE))
    }

    @Test fun accessibilityDisclosureExplainsDataBeforeSettings() {
        compose.onNodeWithText("Enable protection").performClick()
        compose.onNodeWithText("What stays private").assertExists()
        compose.onNodeWithText("Agree & open settings").performScrollTo().assertIsDisplayed()
        assertFalse(store.state.value.consent)
    }

    @Test fun breakSurvivesRecreationAndCanEndEarly() {
        compose.runOnUiThread { store.startBreak(5) }
        assertTrue(store.remainingBreak() in 1..300_000)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("End break early").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(0L,store.remainingBreak()) }
    }

    @Test fun countsAreRealAndCanBeCleared() {
        compose.runOnUiThread { store.recordBlock();store.recordBlock() }
        compose.onNodeWithText("Activity",useUnmergedTree=true).performClick()
        compose.onNodeWithText("2 blocks").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Clear activity").performClick()
        compose.onNodeWithText("Clear activity?").assertIsDisplayed()
        compose.onAllNodesWithText("Clear activity").onLast().performClick()
        compose.runOnIdle { assertEquals(0,store.state.value.totalBlocks);assertEquals(0,store.state.value.todayBlocks) }
    }
}

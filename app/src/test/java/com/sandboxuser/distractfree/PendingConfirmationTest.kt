package com.sandboxuser.distractfree

import com.sandboxuser.distractfree.data.ConfirmationAction
import com.sandboxuser.distractfree.data.PendingConfirmation
import org.junit.Assert.*
import org.junit.Test

class PendingConfirmationTest {
    private val request = PendingConfirmation(ConfirmationAction.TAKE_BREAK, deadline=130_000, boot=7)

    @Test fun requiresTheFullTwoMinutes() {
        assertEquals(120_000L, PendingConfirmation.WAIT_MILLIS)
        assertEquals(120_000L, request.remaining(10_000, 7))
        assertEquals(1L, request.remaining(129_999, 7))
        assertFalse(request.permits(ConfirmationAction.TAKE_BREAK, "", 129_999, 7))
        assertEquals(0L, request.remaining(130_000, 7))
        assertTrue(request.permits(ConfirmationAction.TAKE_BREAK, "", 130_000, 7))
    }

    @Test fun completedWaitOnlyAuthorizesItsOriginalActionAndWebsite() {
        val removal = PendingConfirmation(ConfirmationAction.REMOVE_WEBSITE, "reddit.com", 130_000, 7)
        assertTrue(removal.permits(ConfirmationAction.REMOVE_WEBSITE, "reddit.com", 140_000, 7))
        assertFalse(removal.permits(ConfirmationAction.REMOVE_WEBSITE, "youtube.com", 140_000, 7))
        assertFalse(removal.permits(ConfirmationAction.TAKE_BREAK, "", 140_000, 7))
        assertFalse(request.permits(ConfirmationAction.REMOVE_WEBSITE, "reddit.com", 140_000, 7))
    }

    @Test fun restartingThePhoneNeverUnlocksAnOldRequest() {
        assertEquals(120_000L, request.remaining(1_000_000, 8))
        assertFalse(request.permits(ConfirmationAction.TAKE_BREAK, "", 1_000_000, 8))
    }

    @Test fun completedRequestDoesNotExpireOrBecomeNegative() {
        assertEquals(0L, request.remaining(400_000, 7))
        assertTrue(request.permits(ConfirmationAction.TAKE_BREAK, "", 400_000, 7))
    }
}

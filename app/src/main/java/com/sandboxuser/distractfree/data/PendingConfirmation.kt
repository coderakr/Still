package com.sandboxuser.distractfree.data

enum class ConfirmationAction { TAKE_BREAK, REMOVE_WEBSITE }

/** A persisted, action-specific cooling-off period measured with the monotonic clock. */
data class PendingConfirmation(
    val action: ConfirmationAction,
    val website: String = "",
    val deadline: Long,
    val boot: Int
) {
    fun remaining(elapsed: Long, currentBoot: Int): Long =
        if (boot != currentBoot) WAIT_MILLIS else (deadline - elapsed).coerceAtLeast(0)

    fun permits(action: ConfirmationAction, website: String, elapsed: Long, currentBoot: Int): Boolean =
        this.action == action && this.website == website && boot == currentBoot && elapsed >= deadline

    companion object {
        const val WAIT_MILLIS = 120_000L
    }
}

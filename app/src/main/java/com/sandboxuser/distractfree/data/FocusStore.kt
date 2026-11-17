package com.sandboxuser.distractfree.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import com.sandboxuser.distractfree.blocking.BreakClock
import com.sandboxuser.distractfree.blocking.BlockingRules
import com.sandboxuser.distractfree.blocking.FeedRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class FocusSettings(
    val protection: Boolean = true,
    val feeds: Set<FeedRule> = setOf(FeedRule.YOUTUBE, FeedRule.INSTAGRAM),
    val websites: Set<String> = emptySet(),
    val websitesEnabled: Boolean = true,
    val breakDeadline: Long = 0,
    val breakBoot: Int = -1,
    val todayBlocks: Int = 0,
    val totalBlocks: Int = 0,
    val week: List<Int> = List(7) { 0 },
    val consent: Boolean = false,
    val confirmation: PendingConfirmation? = null
) {
    val ruleCount: Int get() = feeds.size + if (websitesEnabled) websites.size else 0
}

class FocusStore private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("still_preferences", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    val state = mutable.asStateFlow()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    init { refresh(); prefs.registerOnSharedPreferenceChangeListener(listener) }

    fun refresh() {
        // Elapsed time resets at reboot. Restart the wait instead of unlocking the action.
        val pending = readConfirmation()
        if (pending != null && pending.boot != bootCount()) {
            prefs.edit().putLong("confirmationDeadline", SystemClock.elapsedRealtime() + PendingConfirmation.WAIT_MILLIS)
                .putInt("confirmationBoot", bootCount()).apply()
        }
        mutable.value = read()
    }
    private fun bootCount() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
    fun remainingBreak(settings: FocusSettings = state.value): Long = BreakClock.remaining(
        settings.breakDeadline, settings.breakBoot, bootCount(), SystemClock.elapsedRealtime()
    )
    fun blockingActive(): Boolean = state.value.let { it.consent && it.protection && remainingBreak(it) == 0L }
    fun setProtection(enabled: Boolean) { prefs.edit().putBoolean("protection", enabled).putLong("breakDeadline", 0).apply() }
    fun setFeed(rule: FeedRule, enabled: Boolean) {
        val rules = state.value.feeds.toMutableSet().apply { if (enabled) add(rule) else remove(rule) }
        prefs.edit().putStringSet("feeds", rules.map { it.name }.toSet()).apply()
    }
    fun setWebsitesEnabled(enabled: Boolean) { prefs.edit().putBoolean("websitesEnabled", enabled).apply() }
    fun addWebsite(input: String): String? {
        val domain = BlockingRules.normalizeDomain(input) ?: return "Enter a valid domain, like reddit.com."
        if (domain in state.value.websites) return "This website is already on your list."
        prefs.edit().putStringSet("websites", state.value.websites + domain).apply()
        return null
    }
    fun requestBreakConfirmation() { requestConfirmation(ConfirmationAction.TAKE_BREAK) }
    fun requestWebsiteRemoval(domain: String) {
        if (domain in state.value.websites) requestConfirmation(ConfirmationAction.REMOVE_WEBSITE, domain)
    }
    private fun requestConfirmation(action: ConfirmationAction, website: String = "") {
        refresh()
        val pending = state.value.confirmation
        // Reopening the same pending request (including after process recreation) keeps its deadline.
        if (pending?.action == action && pending.website == website) return
        prefs.edit().putString("confirmationAction", action.name).putString("confirmationWebsite", website)
            .putLong("confirmationDeadline", SystemClock.elapsedRealtime() + PendingConfirmation.WAIT_MILLIS)
            .putInt("confirmationBoot", bootCount()).apply()
    }
    fun remainingConfirmation(): Long = state.value.confirmation?.remaining(SystemClock.elapsedRealtime(), bootCount())
        ?: PendingConfirmation.WAIT_MILLIS
    fun cancelConfirmation() { prefs.edit().clearConfirmation().apply() }
    private fun SharedPreferences.Editor.clearConfirmation(): SharedPreferences.Editor =
        remove("confirmationAction").remove("confirmationWebsite").remove("confirmationDeadline").remove("confirmationBoot")

    private fun canConfirm(action: ConfirmationAction, website: String = ""): Boolean {
        refresh()
        return state.value.confirmation?.permits(action, website, SystemClock.elapsedRealtime(), bootCount()) == true
    }
    fun removeWebsite(domain: String): Boolean {
        if (!canConfirm(ConfirmationAction.REMOVE_WEBSITE, domain) || domain !in state.value.websites) return false
        prefs.edit().putStringSet("websites", state.value.websites - domain).clearConfirmation().apply()
        return true
    }
    fun startBreak(minutes: Int): Boolean {
        require(minutes in setOf(5, 10, 15, 30))
        if (!canConfirm(ConfirmationAction.TAKE_BREAK) || !state.value.consent || !state.value.protection) return false
        prefs.edit().putLong("breakDeadline", SystemClock.elapsedRealtime() + minutes * 60_000L)
            .putInt("breakBoot", bootCount()).clearConfirmation().apply()
        return true
    }
    fun endBreak() { prefs.edit().putLong("breakDeadline", 0).apply() }
    fun consent() { prefs.edit().putBoolean("consent", true).apply() }
    fun recordBlock() {
        val key = "blocks_${LocalDate.now()}"
        val editor = prefs.edit().putInt(key, prefs.getInt(key, 0) + 1)
            .putInt("totalBlocks", prefs.getInt("totalBlocks", 0) + 1)
        // Counts only; don't retain browsing history. Bound the daily buckets to 30 days.
        prefs.all.keys.filter { it.startsWith("blocks_") }.forEach {
            val date = runCatching { LocalDate.parse(it.removePrefix("blocks_")) }.getOrNull()
            if (date != null && date.isBefore(LocalDate.now().minusDays(30))) editor.remove(it)
        }
        editor.apply()
    }
    fun clearStats() {
        val editor = prefs.edit().remove("totalBlocks")
        prefs.all.keys.filter { it.startsWith("blocks_") }.forEach { editor.remove(it) }
        editor.apply()
    }
    private fun readConfirmation(): PendingConfirmation? {
        val action = ConfirmationAction.entries.find { it.name == prefs.getString("confirmationAction", null) } ?: return null
        val deadline = prefs.getLong("confirmationDeadline", 0).takeIf { it > 0 } ?: return null
        return PendingConfirmation(action, prefs.getString("confirmationWebsite", "").orEmpty(), deadline, prefs.getInt("confirmationBoot", -1))
    }
    private fun read(): FocusSettings {
        val today = LocalDate.now()
        return FocusSettings(
            protection = prefs.getBoolean("protection", true),
            feeds = prefs.getStringSet("feeds", setOf("YOUTUBE", "INSTAGRAM"))!!.mapNotNull { name -> FeedRule.entries.find { it.name == name } }.toSet(),
            websites = prefs.getStringSet("websites", emptySet())!!.toSet(),
            websitesEnabled = prefs.getBoolean("websitesEnabled", true),
            breakDeadline = prefs.getLong("breakDeadline", 0),
            breakBoot = prefs.getInt("breakBoot", -1),
            todayBlocks = prefs.getInt("blocks_$today", 0),
            totalBlocks = prefs.getInt("totalBlocks", 0),
            week = (6 downTo 0).map { prefs.getInt("blocks_${today.minusDays(it.toLong())}", 0) },
            consent = prefs.getBoolean("consent", false),
            confirmation = readConfirmation()
        )
    }

    companion object {
        @Volatile private var instance: FocusStore? = null
        fun get(context: Context): FocusStore = instance ?: synchronized(this) {
            instance ?: FocusStore(context.applicationContext).also { instance = it }
        }
    }
}

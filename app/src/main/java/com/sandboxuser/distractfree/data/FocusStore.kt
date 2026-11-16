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
    val consent: Boolean = false
) {
    val ruleCount: Int get() = feeds.size + if (websitesEnabled) websites.size else 0
}

class FocusStore private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("still_preferences", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())
    val state = mutable.asStateFlow()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    init { prefs.registerOnSharedPreferenceChangeListener(listener) }

    fun refresh() { mutable.value = read() }
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
    fun removeWebsite(domain: String) { prefs.edit().putStringSet("websites", state.value.websites - domain).apply() }
    fun startBreak(minutes: Int) {
        require(minutes in setOf(5, 10, 15, 30))
        prefs.edit().putLong("breakDeadline", SystemClock.elapsedRealtime() + minutes * 60_000L)
            .putInt("breakBoot", bootCount()).apply()
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
            consent = prefs.getBoolean("consent", false)
        )
    }

    companion object {
        @Volatile private var instance: FocusStore? = null
        fun get(context: Context): FocusStore = instance ?: synchronized(this) {
            instance ?: FocusStore(context.applicationContext).also { instance = it }
        }
    }
}

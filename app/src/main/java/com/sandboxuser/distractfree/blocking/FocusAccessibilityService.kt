package com.sandboxuser.distractfree.blocking

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import com.sandboxuser.distractfree.BlockedActivity
import com.sandboxuser.distractfree.data.FocusStore

class FocusAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val store by lazy { FocusStore.get(this) }
    private var lastBlock = -10_000L
    private val inspect = Runnable { inspectWindow() }
    private val tick = object : Runnable {
        override fun run() {
            // Also recheck a stationary feed when a break expires, even without a UI event.
            inspectWindow()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onServiceConnected() { handler.post(tick) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        handler.removeCallbacks(inspect)
        handler.postDelayed(inspect, 120)
    }
    override fun onInterrupt() { handler.removeCallbacks(inspect) }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }

    private fun inspectWindow() {
        if (!store.blockingActive() || SystemClock.elapsedRealtime() - lastBlock < 1800) return
        val root = rootInActiveWindow ?: return
        try {
            val pkg = root.packageName?.toString() ?: return
            val settings = store.state.value
            val addressIds = BlockingRules.browserAddressIds[pkg]
            if (addressIds == null && settings.feeds.none { pkg in it.packages }) return
            val nodes = mutableListOf<ScreenNode>()
            val addresses = mutableListOf<String>()
            var remaining = 450
            fun walk(node: AccessibilityNodeInfo, depth: Int) {
                if (remaining-- <= 0 || depth > 35 || !node.isVisibleToUser) return
                val id = node.viewIdResourceName.orEmpty()
                if (addressIds == null) {
                    nodes.add(ScreenNode(id, node.text?.toString().orEmpty(), node.contentDescription?.toString().orEmpty(), node.isSelected))
                } else if (id.substringAfterLast('/') in addressIds && !(node.isEditable && node.isFocused)) {
                    node.text?.toString()?.takeIf { it.isNotBlank() }?.let(addresses::add)
                }
                for (i in 0 until node.childCount) {
                    if (remaining <= 0) break
                    val child = node.getChild(i) ?: continue
                    try { walk(child, depth + 1) } finally { @Suppress("DEPRECATION") child.recycle() }
                }
            }
            walk(root, 0)
            var title = BlockingRules.detectedFeed(pkg, nodes, settings.feeds)?.title
            if (title == null && addressIds != null) {
                for (url in addresses) {
                    title = if (settings.websitesEnabled) BlockingRules.blockedWebsite(url, settings.websites) else null
                    if (title == null) title = BlockingRules.shortFormWebsite(url, settings.feeds)?.title
                    if (title != null) break
                }
            }
            val exitAction = if (addressIds == null) GLOBAL_ACTION_BACK else GLOBAL_ACTION_HOME
            if (title != null && performGlobalAction(exitAction)) {
                lastBlock = SystemClock.elapsedRealtime()
                store.recordBlock()
                val blockedTitle = title
                // Exit a feed player before going Home, so YouTube cannot carry its
                // playing Short into picture-in-picture. Browser pages go straight Home.
                handler.postDelayed({
                    if (store.blockingActive()) {
                        if (addressIds == null) performGlobalAction(GLOBAL_ACTION_HOME)
                        // HOME is asynchronous; presenting immediately races the launcher.
                        handler.postDelayed({
                            if (store.blockingActive()) {
                                startActivity(Intent(this, BlockedActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                    putExtra("blocked_title", blockedTitle)
                                })
                            }
                        }, 500)
                    }
                }, if (addressIds == null) 350 else 0)
            }
        } finally { @Suppress("DEPRECATION") root.recycle() }
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val expected = ComponentName(context, FocusAccessibilityService::class.java)
            return manager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                val info = it.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name) == expected
            }
        }
    }
}

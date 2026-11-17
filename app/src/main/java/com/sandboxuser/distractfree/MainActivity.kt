package com.sandboxuser.distractfree

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sandboxuser.distractfree.data.FocusStore
import com.sandboxuser.distractfree.ui.StillApp
import com.sandboxuser.distractfree.ui.theme.DistractfreeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(0, 0), navigationBarStyle = SystemBarStyle.light(0, 0))
        consumeBreakRequest(intent)
        setContent { DistractfreeTheme { StillApp() } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeBreakRequest(intent)
    }
    private fun consumeBreakRequest(intent: Intent) {
        if (intent.getBooleanExtra("open_break", false)) {
            intent.removeExtra("open_break")
            FocusStore.get(this).requestBreakConfirmation()
        }
    }
}

package com.sandboxuser.distractfree

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import com.sandboxuser.distractfree.ui.StillApp
import com.sandboxuser.distractfree.ui.theme.DistractfreeTheme

class MainActivity : ComponentActivity() {
    private val breakRequests = mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(0, 0), navigationBarStyle = SystemBarStyle.light(0, 0))
        if (intent.getBooleanExtra("open_break", false)) breakRequests.intValue++
        setContent { DistractfreeTheme { StillApp(breakRequests.intValue) } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_break", false)) breakRequests.intValue++
    }
}

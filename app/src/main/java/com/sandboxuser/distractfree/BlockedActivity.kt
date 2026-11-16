package com.sandboxuser.distractfree

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxuser.distractfree.ui.BotanicalArt
import com.sandboxuser.distractfree.ui.Glyph
import com.sandboxuser.distractfree.ui.theme.*

class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.light(0,0),navigationBarStyle=SystemBarStyle.light(0,0))
        val title=intent.getStringExtra("blocked_title") ?: "This distraction"
        setContent {
            DistractfreeTheme {
                BackHandler { goHome() }
                Surface(Modifier.fillMaxSize(),color=Paper) {
                    Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp).widthIn(max=600.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(22.dp)) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Glyph("leaf"); Text("still",fontSize=30.sp,fontWeight=FontWeight.Bold) }
                        Surface(shape=RoundedCornerShape(100.dp),color=Ink,modifier=Modifier.size(190.dp)) { BotanicalArt(Modifier.fillMaxSize().padding(14.dp)) }
                        Text("A MOMENT TO CHOOSE",style=MaterialTheme.typography.labelSmall,color=Forest)
                        Text("Take back\nthis moment.",style=MaterialTheme.typography.headlineLarge,textAlign=TextAlign.Center)
                        Text("$title can wait.\nYou made a little room for something better.",textAlign=TextAlign.Center,color=Muted)
                        Button(onClick=::goHome,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp),shape=RoundedCornerShape(16.dp)) { Text("Back to my day"); Spacer(Modifier.width(12.dp)); Glyph("arrow",tint=Color.White,size=18.dp) }
                        TextButton(onClick={startActivity(Intent(this@BlockedActivity,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("open_break",true));finish()}) { Glyph("cup",size=19.dp); Spacer(Modifier.width(10.dp)); Text("Take a short break") }
                        Text("No guilt. Just a gentle nudge.",style=MaterialTheme.typography.bodySmall,color=Muted)
                    }
                }
            }
        }
    }
    private fun goHome() { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); finish() }
}

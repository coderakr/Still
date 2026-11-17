package com.sandboxuser.distractfree.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sandboxuser.distractfree.blocking.FocusAccessibilityService
import com.sandboxuser.distractfree.blocking.FeedRule
import com.sandboxuser.distractfree.data.FocusSettings
import com.sandboxuser.distractfree.data.FocusStore
import com.sandboxuser.distractfree.data.ConfirmationAction
import com.sandboxuser.distractfree.data.PendingConfirmation
import com.sandboxuser.distractfree.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StillApp() {
    val context = LocalContext.current
    val store = remember { FocusStore.get(context) }
    val settings by store.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var blockTab by rememberSaveable { mutableIntStateOf(0) }
    var connected by remember { mutableStateOf(FocusAccessibilityService.isEnabled(context)) }
    var remaining by remember { mutableLongStateOf(store.remainingBreak()) }
    val pending = settings.confirmation
    val showBreak = pending?.action == ConfirmationAction.TAKE_BREAK
    var confirmationRemaining by remember(pending) { mutableLongStateOf(store.remainingConfirmation()) }
    var showSetup by rememberSaveable { mutableStateOf(false) }
    var showWebsite by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showClear by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { connected = FocusAccessibilityService.isEnabled(context); store.refresh() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(settings.breakDeadline, settings.breakBoot) {
        while (true) { remaining = store.remainingBreak(); delay(1000) }
    }
    LaunchedEffect(pending) {
        if (pending != null) {
            while (true) { confirmationRemaining = store.remainingConfirmation(); delay(250) }
        }
    }
    BackHandler(tab != 0 && pending == null && !showSetup && !showWebsite && !showHelp) { tab = 0 }
    val active = connected && settings.consent && settings.protection && remaining == 0L
    Scaffold(
        containerColor = Paper,
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    listOf("Focus" to "focus", "Blocks" to "blocks", "Activity" to "activity", "Settings" to "settings").forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == index, onClick = { tab = index },
                            icon = { Glyph(item.second, tint = if (tab == index) Forest else Muted, size = 22.dp) },
                            label = { Text(item.first, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Sage, selectedTextColor = Forest, unselectedTextColor = Muted)
                        )
                    }
                }
            }
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets),contentAlignment = Alignment.TopCenter) {
            Crossfade(tab, label = "navigation") { selected ->
                Column(Modifier.widthIn(max=600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(top=18.dp, bottom=28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    BrandHeader(active, remaining > 0, connected && settings.consent)
                    when(selected) {
                        0 -> FocusScreen(settings, active, connected && settings.consent, remaining,
                            onSetup = { showSetup = true }, onBreak = store::requestBreakConfirmation, onResume = store::endBreak,
                            onProtection = store::setProtection,
                            onBlocks = { blockTab=0; tab=1 }, onWebsites = { blockTab=1; tab=1 }, onActivity = { tab=2 }, onFeed = store::setFeed)
                        1 -> BlocksScreen(settings, blockTab, { blockTab = it }, store::setFeed, store::setWebsitesEnabled, { showWebsite = true }, store::requestWebsiteRemoval, { showHelp = true })
                        2 -> ActivityScreen(settings)
                        3 -> SettingsScreen(connected && settings.consent, { showSetup=true }, { showHelp=true }, { showClear=true })
                    }
                }
            }
        }
    }
    if (showBreak) {
        var minutes by rememberSaveable { mutableIntStateOf(5) }
        ModalBottomSheet(sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true), onDismissRequest=store::cancelConfirmation, containerColor=Paper) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp), verticalArrangement=Arrangement.spacedBy(20.dp)) {
                IconTile("cup", Sage, Forest, 56)
                Text("A break, on purpose.",style=MaterialTheme.typography.headlineMedium)
                Text("Give yourself two minutes to decide. Choose a break length while you wait, then confirm if you still want a break.",color=Muted)
                ConfirmationCountdown(confirmationRemaining, "Your blocking rules stay in place while you wait.")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    listOf(5,10,15,30).forEach { value ->
                        Surface(onClick={minutes=value},modifier=Modifier.weight(1f),shape=RoundedCornerShape(18.dp),color=if(minutes==value) Forest else Color.White,border=BorderStroke(1.dp,if(minutes==value) Forest else Outline)) {
                            Column(Modifier.padding(vertical=18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text("$value",fontSize=25.sp,fontWeight=FontWeight.SemiBold,color=if(minutes==value) Color.White else Ink)
                                Text("min",color=if(minutes==value) Color.White.copy(alpha=.75f) else Muted,style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Note("clock","All feed and website blocks pause together. Protection resumes automatically, even if you close Still. Restarting your phone ends the break.")
                Button(onClick={
                    connected = FocusAccessibilityService.isEnabled(context)
                    if (connected) store.startBreak(minutes)
                },modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=RoundedCornerShape(16.dp),enabled=confirmationRemaining == 0L && connected && settings.consent && settings.protection) {
                    Glyph("pause",tint=Color.White,size=18.dp); Spacer(Modifier.width(10.dp)); Text("Start $minutes-minute break")
                }
                if (!connected || !settings.consent || !settings.protection) Text("Turn on protection and finish setup before starting a break.",color=Amber,style=MaterialTheme.typography.bodySmall)
                TextButton(onClick=store::cancelConfirmation,modifier=Modifier.align(Alignment.CenterHorizontally)) { Text("Keep focusing") }
            }
        }
    }
    if (pending?.action == ConfirmationAction.REMOVE_WEBSITE) {
        ModalBottomSheet(sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true), onDismissRequest=store::cancelConfirmation, containerColor=Paper) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                IconTile("globe", Sage, Forest, 56)
                Text("Reconsider this boundary.",style=MaterialTheme.typography.headlineMedium)
                Text("Take two minutes before removing ${pending.website}. It stays on your blocklist until you confirm.",color=Muted)
                ConfirmationCountdown(confirmationRemaining, "Nothing will be removed automatically.")
                Button(onClick={store.removeWebsite(pending.website)},enabled=confirmationRemaining == 0L,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=RoundedCornerShape(16.dp)) {
                    Glyph("trash",tint=Color.White,size=18.dp); Spacer(Modifier.width(10.dp)); Text("Remove website")
                }
                TextButton(onClick=store::cancelConfirmation,modifier=Modifier.align(Alignment.CenterHorizontally)) { Text("Keep website") }
            }
        }
    }
    if (showSetup) {
        ModalBottomSheet(sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true), onDismissRequest={showSetup=false},containerColor=Paper) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                IconTile("shield",Sage,Forest,56)
                Text("A small permission.\nA little more peace.",style=MaterialTheme.typography.headlineMedium)
                Text("Still uses Android Accessibility to recognize short-form feeds and website addresses in supported apps.",color=Muted)
                DetailLine("globe","What Still can see","Visible screen content and browser addresses are processed on your device to check your block rules.")
                DetailLine("shield","What Still does","When a rule matches, Still leaves the distracting screen and shows a gentle reminder. You stay in control.")
                DetailLine("lock","What stays private","Screen content and browsing history are never saved or shared. Only daily block counts are stored. No account, ads, or internet permission.")
                Text("By continuing, you consent to this access. On the next screen, select Still and turn on Use Still. You can revoke access there at any time.",style=MaterialTheme.typography.bodySmall,color=Muted)
                Button(onClick={
                    store.consent()
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    showSetup=false
                },modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=RoundedCornerShape(16.dp)) { Text(if(connected && settings.consent) "Open accessibility settings" else "Agree & open settings"); Spacer(Modifier.width(12.dp)); Glyph("arrow",tint=Color.White,size=18.dp) }
                TextButton(onClick={showSetup=false},modifier=Modifier.align(Alignment.CenterHorizontally)) { Text("Maybe later") }
            }
        }
    }
    if(showWebsite) WebsiteSheet(onDismiss={showWebsite=false},onAdd=store::addWebsite)
    if(showHelp) AlertDialog(onDismissRequest={showHelp=false},title={Text("How blocking works")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("Short-form: Still recognizes supported Shorts and Reels viewer screens. Normal YouTube videos and other app sections remain available. TikTok blocks the entire app.")
            Text("Websites: Chrome, Brave, Edge, Firefox, Samsung Internet, and Opera are supported when they expose the address bar. Subdomains are included. Short-form web URLs are checked too.")
            Text("Coverage depends on the app version, language, and accessible screen information. Hidden addresses, picture-in-picture, embedded browsers, and some feed layouts may not be detected. Disable picture-in-picture for video apps in Android settings for more reliable blocking. This is a screen-based blocker, not a network filter.")
            Text("Taking a break or removing a website requires a two-minute wait, followed by your confirmation. Closing the sheet cancels the request; trying again starts a fresh wait. Leaving the app keeps the pending countdown. A phone restart restarts the wait.")
            Text("Breaks: After confirmation, all rules pause for your chosen duration and resume automatically. You can end a break early. A phone restart ends an active break.")
        }
    },confirmButton={TextButton(onClick={showHelp=false}) { Text("Got it") }})
    if(showClear) AlertDialog(onDismissRequest={showClear=false},title={Text("Clear activity?")},text={Text("This removes your daily and all-time block counts. Your rules and websites stay as they are.")},confirmButton={TextButton(onClick={store.clearStats();showClear=false}) { Text("Clear activity") }},dismissButton={TextButton(onClick={showClear=false}) { Text("Keep it") }})
}

@Composable
private fun ConfirmationCountdown(remaining: Long, message: String) {
    Surface(shape=RoundedCornerShape(20.dp),color=Sage) {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Glyph(if(remaining>0) "clock" else "check",size=20.dp)
                Text(if(remaining>0) "TIME TO THINK" else "YOUR CHOICE",style=MaterialTheme.typography.labelSmall,color=Forest)
            }
            Text(if(remaining>0) formatTime(remaining) else "Ready to confirm",style=MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress={ (1f - remaining.toFloat() / PendingConfirmation.WAIT_MILLIS).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth(),color=Forest,trackColor=Color.White)
            Text(if(remaining>0) "$message You can cancel at any time, but this wait cannot be skipped." else "The wait is over. Confirm below, or keep your boundary.",style=MaterialTheme.typography.bodySmall,color=Forest)
        }
    }
}

@Composable
private fun BrandHeader(active: Boolean, paused: Boolean, connected: Boolean) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Glyph("leaf",size=27.dp); Text("still",fontSize=30.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1.4).sp)
        }
        Surface(shape=CircleShape,color=if(paused) Color(0xFFF6EDDA) else Sage) {
            Row(Modifier.padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).background(if(paused) Amber else if(active) Forest else Muted,CircleShape))
                Text(if(paused) "On a break" else if(active) "Protected" else if(!connected) "Setup needed" else "Protection off",style=MaterialTheme.typography.labelMedium,color=if(paused) Amber else Forest)
            }
        }
    }
}

@Composable
private fun FocusScreen(s: FocusSettings, active: Boolean, connected: Boolean, remaining: Long, onSetup:()->Unit, onBreak:()->Unit, onResume:()->Unit, onProtection:(Boolean)->Unit, onBlocks:()->Unit, onWebsites:()->Unit, onActivity:()->Unit, onFeed:(FeedRule,Boolean)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Eyebrow("LESS SCROLLING. MORE LIVING.")
        Text("Your time,\nback in your hands.",style=MaterialTheme.typography.headlineLarge)
    }
    Surface(shape=CardShape,color=Ink) {
        Box(Modifier.fillMaxWidth()) {
            BotanicalArt(Modifier.align(Alignment.CenterEnd).width(142.dp).height(205.dp))
            Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    Glyph(if(remaining>0) "cup" else "shield",tint=Lime,size=17.dp)
                    Text(if(remaining>0) "A MOMENT TO UNWIND" else if(active) "YOUR SPACE IS PROTECTED" else "MAKE SPACE FOR WHAT MATTERS",style=MaterialTheme.typography.labelSmall,color=Lime)
                }
                Text(if(remaining>0) formatTime(remaining) else if(active) "Less noise.\nMore you." else "A calmer day\nstarts here.",style=MaterialTheme.typography.headlineMedium,color=Color.White,modifier=Modifier.fillMaxWidth(.72f))
                Text(if(remaining>0) "until protection returns" else if(active) "Your boundaries are in place." else "Small boundaries. Big difference.",style=MaterialTheme.typography.bodySmall,color=Color(0xFFB7CCBC),modifier=Modifier.fillMaxWidth(.72f))
                FilledTonalButton(onClick=if(remaining>0) onResume else if(!connected) onSetup else if(!s.protection) ({onProtection(true)}) else onBreak,shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=Lime,contentColor=Ink),contentPadding=PaddingValues(horizontal=16.dp,vertical=10.dp)) {
                    Text(if(remaining>0) "End break early" else if(!connected) "Enable protection" else if(!s.protection) "Turn on protection" else "Take a break",style=MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(12.dp)); Glyph("arrow",size=17.dp,tint=Ink)
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        StatTile("${s.todayBlocks}","blocks today","shield",Modifier.weight(1f),onActivity)
        StatTile("${s.ruleCount}","rules enabled","blocks",Modifier.weight(1f),onBlocks)
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        SectionTitle("Your boundaries","Manage",onBlocks)
        Surface(shape=CardShape,color=Color.White,border=BorderStroke(1.dp,Outline)) {
            Column(Modifier.padding(horizontal=16.dp)) {
                listOf(FeedRule.YOUTUBE,FeedRule.INSTAGRAM).forEachIndexed { index,rule ->
                    FeedRow(rule,rule in s.feeds,{onFeed(rule,it)},compact=true)
                    if(index==0) HorizontalDivider(color=Outline)
                }
                HorizontalDivider(color=Outline)
                Row(Modifier.fillMaxWidth().clickable(onClick=onWebsites).padding(vertical=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    IconTile("globe",Sage,Forest,44)
                    Column(Modifier.weight(1f)) { Text("Websites",style=MaterialTheme.typography.titleMedium); Text(if(s.websites.isEmpty()) "Choose what stays out" else "${s.websites.size} on your blocklist",style=MaterialTheme.typography.bodySmall,color=Muted) }
                    Glyph("chevron",tint=Muted,size=19.dp)
                }
            }
        }
    }
    Surface(onClick=if(remaining>0) onResume else onBreak,shape=CardShape,color=Sage) {
        Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            IconTile(if(remaining>0) "play" else "cup",Color.White.copy(alpha=.7f),Forest,44)
            Column(Modifier.weight(1f)) { Text(if(remaining>0) "Ready to come back?" else "Take a mindful break",style=MaterialTheme.typography.titleMedium); Text(if(remaining>0) "Resume your boundaries now" else "Pause the rules. Set a timer.",style=MaterialTheme.typography.bodySmall,color=Muted) }
            Glyph("arrow",size=21.dp)
        }
    }
    if(connected) Surface(shape=RoundedCornerShape(18.dp),color=Color.Transparent) {
        Row(Modifier.fillMaxWidth().toggleable(s.protection,role=Role.Switch,onValueChange=onProtection).padding(horizontal=3.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Protection",style=MaterialTheme.typography.titleMedium); Text(if(remaining>0) "Paused until your break ends" else if(s.protection) "Your enabled rules are ready" else "All rules are turned off",style=MaterialTheme.typography.bodySmall,color=Muted) }
            Switch(checked=s.protection,onCheckedChange=null)
        }
    }
    Row(Modifier.alignCenter(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Glyph("leaf",tint=Muted,size=14.dp); Text("A little less online. A little more present.",color=Muted,style=MaterialTheme.typography.bodySmall)
    }
}

private fun Modifier.alignCenter(): Modifier = fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)

@Composable
private fun BlocksScreen(s:FocusSettings,selected:Int,onSelected:(Int)->Unit,onFeed:(FeedRule,Boolean)->Unit,onWebEnabled:(Boolean)->Unit,onAdd:()->Unit,onRemove:(String)->Unit,onHelp:()->Unit) {
    PageTitle("YOUR ATTENTION, YOUR RULES","Build your boundaries.","Keep the useful parts. Leave the rabbit holes.")
    Row(Modifier.fillMaxWidth().background(Sage,RoundedCornerShape(16.dp)).padding(5.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        listOf("Short-form" to "play","Websites" to "globe").forEachIndexed { index,item ->
            Surface(onClick={onSelected(index)},modifier=Modifier.weight(1f),color=if(selected==index) Color.White else Color.Transparent,shape=RoundedCornerShape(12.dp),shadowElevation=if(selected==index) 1.dp else 0.dp) {
                Row(Modifier.padding(vertical=13.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) { Glyph(item.second,size=17.dp,tint=if(selected==index) Forest else Muted); Spacer(Modifier.width(8.dp)); Text(item.first,style=MaterialTheme.typography.labelLarge,color=if(selected==index) Forest else Muted) }
            }
        }
    }
    if(selected==0) {
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            SectionTitle("Short-form feeds","${s.feeds.size} enabled")
            FeedRule.entries.forEach { rule -> Surface(shape=RoundedCornerShape(20.dp),color=Color.White,border=BorderStroke(1.dp,Outline)) { Box(Modifier.padding(horizontal=16.dp)) { FeedRow(rule,rule in s.feeds,{onFeed(rule,it)}) } } }
        }
        Note("info","Shorts and Reels are blocked when their viewer is detected. Other app sections stay available. TikTok blocks the entire app.")
    } else {
        Surface(shape=CardShape,color=Ink) {
            Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Glyph("globe",tint=Lime,size=28.dp)
                Column(Modifier.weight(1f)) { Text("A quieter corner\nof the internet.",color=Color.White,style=MaterialTheme.typography.titleLarge); Spacer(Modifier.height(6.dp)); Text("Your list. Your limits.",color=Color(0xFFB7CCBC),style=MaterialTheme.typography.bodySmall) }
            }
        }
        Row(Modifier.fillMaxWidth().toggleable(s.websitesEnabled,role=Role.Switch,onValueChange=onWebEnabled),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Website blocking",style=MaterialTheme.typography.titleMedium); Text("Includes all subdomains",color=Muted,style=MaterialTheme.typography.bodySmall) }
            Switch(checked=s.websitesEnabled,onCheckedChange=null)
        }
        SectionTitle("Your blocklist",if(s.websites.size==1) "1 website" else "${s.websites.size} websites")
        Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=RoundedCornerShape(16.dp)) { Glyph("plus",tint=Color.White,size=18.dp); Spacer(Modifier.width(10.dp)); Text("Add a website") }
        if(s.websites.isEmpty()) {
            Surface(shape=CardShape,color=Color.White,border=BorderStroke(1.dp,Outline)) {
                Column(Modifier.fillMaxWidth().padding(26.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    IconTile("globe",Sage,Forest,56)
                    Text("Leave the rabbit holes behind.",style=MaterialTheme.typography.titleMedium,textAlign=TextAlign.Center)
                    Text("Add the sites that pull you away\nfrom what you meant to do.",color=Muted,textAlign=TextAlign.Center,style=MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Surface(shape=CardShape,color=Color.White,border=BorderStroke(1.dp,Outline)) {
                Column(Modifier.padding(horizontal=16.dp)) {
                    s.websites.sorted().forEachIndexed { index,domain ->
                        Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            IconTile("globe",Sage,Forest,40)
                            Column(Modifier.weight(1f)) { Text(domain,style=MaterialTheme.typography.titleMedium); Text(if(s.websitesEnabled) "On your blocklist" else "Rule paused",color=Muted,style=MaterialTheme.typography.bodySmall) }
                            IconButton(onClick={onRemove(domain)},modifier=Modifier.semantics { contentDescription="Remove $domain" }) { Glyph("close",tint=Muted,size=19.dp) }
                        }
                        if(index<s.websites.size-1) HorizontalDivider(color=Outline)
                    }
                }
            }
        }
        Note("info","Works in supported browsers when the website address is visible to Android. In-app browsers and hidden address bars may not be covered.")
    }
    TextButton(onClick=onHelp,modifier=Modifier.alignCenter()) { Text("How does blocking work?"); Spacer(Modifier.width(8.dp)); Glyph("arrow",size=16.dp) }
}

@Composable
private fun ActivityScreen(s:FocusSettings) {
    PageTitle("SMALL CHOICES ADD UP","Look at you, showing up.","Every interrupted scroll is a chance to choose again.")
    Surface(shape=CardShape,color=Ink) {
        Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Glyph("leaf",tint=Lime,size=20.dp); Eyebrow("ROOM FOR REAL LIFE",Lime) }
            Text("${s.todayBlocks}",fontSize=64.sp,lineHeight=70.sp,fontWeight=FontWeight.SemiBold,color=Color.White,letterSpacing=(-2).sp)
            Text("distractions stopped today",color=Color(0xFFC5D5C8))
            Text(if(s.todayBlocks==0) "Your next intentional choice starts here." else "Small boundaries are making a difference.",style=MaterialTheme.typography.bodySmall,color=Lime)
        }
    }
    Surface(shape=CardShape,color=Color.White,border=BorderStroke(1.dp,Outline)) {
        Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            SectionTitle("This week","${s.week.sum()} blocks")
            Row(Modifier.fillMaxWidth().height(150.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Bottom) {
                val max=(s.week.maxOrNull() ?: 0).coerceAtLeast(1)
                s.week.forEachIndexed { index,count ->
                    val date=LocalDate.now().minusDays((6-index).toLong())
                    Column(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription="${date.dayOfWeek}: $count blocks" },verticalArrangement=Arrangement.Bottom,horizontalAlignment=Alignment.CenterHorizontally) {
                        if(count>0) Text("$count",style=MaterialTheme.typography.labelSmall,color=Muted)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(if(count==0) 5.dp else (count.toFloat()/max*95).coerceAtLeast(8f).dp).background(if(index==6) Forest else Color(0xFFCFDDBB),RoundedCornerShape(6.dp)))
                        Spacer(Modifier.height(12.dp))
                        Text(date.format(DateTimeFormatter.ofPattern("EE",Locale.getDefault())).take(1),style=MaterialTheme.typography.labelMedium,color=if(index==6) Forest else Muted)
                    }
                }
            }
            if(s.week.sum()==0) Text("Your first block will show up here. No pressure, just progress.",style=MaterialTheme.typography.bodySmall,color=Muted)
        }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        StatTile("${s.totalBlocks}","all-time blocks","shield",Modifier.weight(1f))
        StatTile("${s.ruleCount}","rules enabled","blocks",Modifier.weight(1f))
    }
    Note("lock","Just the numbers, never your history. Activity counts stay on this device. Still does not estimate time saved or track what you watch.")
}

@Composable
private fun SettingsScreen(connected:Boolean,onSetup:()->Unit,onHelp:()->Unit,onClear:()->Unit) {
    PageTitle("A LITTLE HOUSEKEEPING","Make yourself at home.","Simple settings. Nothing more than you need.")
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Eyebrow("PROTECTION")
        Surface(onClick=onSetup,shape=CardShape,color=Color.White,border=BorderStroke(1.dp,Outline)) {
            Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                IconTile("shield",Sage,Forest,44)
                Column(Modifier.weight(1f)) { Text("Accessibility service",style=MaterialTheme.typography.titleMedium); Text(if(connected) "Enabled · ready to protect" else "Setup needed · tap to connect",color=if(connected) Forest else Amber,style=MaterialTheme.typography.bodySmall) }
                Glyph("chevron",tint=Muted,size=18.dp)
            }
        }
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Eyebrow("GOOD TO KNOW")
        Surface(shape=CardShape,color=Color.White,border=BorderStroke(1.dp,Outline)) {
            Column(Modifier.padding(horizontal=18.dp)) {
                ActionRow("info","How Still works","Supported apps, browsers & breaks",onHelp)
                HorizontalDivider(color=Outline)
                ActionRow("trash","Clear activity","Start fresh with your block counts",onClear)
            }
        }
    }
    Surface(shape=CardShape,color=Sage) {
        Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Glyph("lock",size=28.dp)
            Text("Your attention is personal.\nYour data should be, too.",style=MaterialTheme.typography.titleLarge)
            Text("No account. No ads. No analytics. Your rules and activity live on this device, and screen content is never saved or sent anywhere.",style=MaterialTheme.typography.bodyMedium,color=Muted)
        }
    }
    Column(Modifier.fillMaxWidth().padding(top=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Glyph("leaf",tint=Forest,size=26.dp)
        Text("still",fontSize=24.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
        Text("Made for a more intentional everyday.",style=MaterialTheme.typography.bodySmall,color=Muted)
        Text("Version 1.0",style=MaterialTheme.typography.labelSmall,color=Muted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebsiteSheet(onDismiss:()->Unit,onAdd:(String)->String?) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun submit() { error=onAdd(input); if(error==null) onDismiss() }
    ModalBottomSheet(sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true), onDismissRequest=onDismiss,containerColor=Paper) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            IconTile("globe",Sage,Forest,56)
            Text("One less rabbit hole.",style=MaterialTheme.typography.headlineMedium)
            Text("Add a domain or paste a link. The whole website and its subdomains will be blocked.",color=Muted)
            OutlinedTextField(value=input,onValueChange={input=it;error=null},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Website address")},placeholder={Text("e.g. reddit.com")},isError=error!=null,supportingText={if(error!=null) Text(error!!)},shape=RoundedCornerShape(16.dp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={submit()}))
            Button(onClick={submit()},modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),enabled=input.isNotBlank(),shape=RoundedCornerShape(16.dp)) { Glyph("plus",tint=Color.White,size=18.dp); Spacer(Modifier.width(10.dp)); Text("Add to blocklist") }
        }
    }
}

@Composable
private fun PageTitle(eyebrow:String,title:String,subtitle:String) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { Eyebrow(eyebrow); Text(title,style=MaterialTheme.typography.headlineLarge); Text(subtitle,color=Muted,style=MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun Eyebrow(text:String,color:Color=Muted) { Text(text,style=MaterialTheme.typography.labelSmall,color=color) }

@Composable
private fun SectionTitle(title:String,action:String,onClick:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text(title,style=MaterialTheme.typography.titleMedium)
        if(onClick!=null) TextButton(onClick=onClick,contentPadding=PaddingValues(start=8.dp)) { Text(action,style=MaterialTheme.typography.labelMedium); Spacer(Modifier.width(3.dp)); Glyph("chevron",size=14.dp) }
        else Text(action,style=MaterialTheme.typography.labelMedium,color=Muted)
    }
}

@Composable
private fun StatTile(value:String,label:String,icon:String,modifier:Modifier=Modifier,onClick:(()->Unit)?=null) {
    Surface(modifier=modifier.then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier),shape=RoundedCornerShape(20.dp),color=Color.White,border=BorderStroke(1.dp,Outline)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text(value,fontSize=29.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.7).sp); Glyph(icon,tint=Forest.copy(alpha=.65f),size=20.dp) }
            Text(label,color=Muted,style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FeedRow(rule:FeedRule,enabled:Boolean,onToggle:(Boolean)->Unit,compact:Boolean=false) {
    Row(Modifier.fillMaxWidth().toggleable(enabled,role=Role.Switch,onValueChange=onToggle).padding(vertical=if(compact) 12.dp else 18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        FeedIcon(rule)
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) { Text(rule.title,style=MaterialTheme.typography.titleMedium); Text(if(compact) { if(enabled) "Block rule enabled" else "Allowed" } else rule.subtitle,style=MaterialTheme.typography.bodySmall,color=Muted) }
        Switch(checked=enabled,onCheckedChange=null,thumbContent=if(enabled) ({Glyph("check",size=13.dp,tint=Forest)}) else null,colors=SwitchDefaults.colors(checkedTrackColor=Forest,checkedThumbColor=Color.White,uncheckedTrackColor=Color(0xFFE4E8E1),uncheckedBorderColor=Color.Transparent,uncheckedThumbColor=Color.White))
    }
}

@Composable
internal fun IconTile(icon:String,bg:Color=Sage,tint:Color=Forest,size:Int=44) {
    Box(Modifier.size(size.dp).background(bg,RoundedCornerShape((size*.3).dp)),contentAlignment=Alignment.Center) { Glyph(icon,tint=tint,size=(size*.48).dp) }
}

@Composable
private fun Note(icon:String,text:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) { Glyph(icon,tint=Muted,size=18.dp,modifier=Modifier.padding(top=2.dp)); Text(text,style=MaterialTheme.typography.bodySmall,color=Muted,modifier=Modifier.weight(1f)) }
}

@Composable
private fun DetailLine(icon:String,title:String,body:String) {
    Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) { IconTile(icon,Sage,Forest,40); Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(title,style=MaterialTheme.typography.titleMedium); Text(body,style=MaterialTheme.typography.bodySmall,color=Muted) } }
}

@Composable
private fun ActionRow(icon:String,title:String,subtitle:String,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) { Glyph(icon,size=22.dp); Column(Modifier.weight(1f)) { Text(title,style=MaterialTheme.typography.titleMedium); Text(subtitle,style=MaterialTheme.typography.bodySmall,color=Muted) }; Glyph("chevron",tint=Muted,size=18.dp) }
}

private fun formatTime(millis:Long):String { val seconds=(millis+999)/1000; return "%02d:%02d".format(seconds/60,seconds%60) }

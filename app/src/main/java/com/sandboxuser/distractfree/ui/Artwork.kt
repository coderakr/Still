package com.sandboxuser.distractfree.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandboxuser.distractfree.blocking.FeedRule
import com.sandboxuser.distractfree.ui.theme.*

@Composable
fun Glyph(name: String, modifier: Modifier = Modifier, tint: Color = Forest, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        scale(this.size.width / 24f, this.size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.7f, cap = StrokeCap.Round)
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(tint, Offset(x,y), Offset(x2,y2), 1.7f, StrokeCap.Round)
            when (name) {
                "shield", "focus" -> {
                    drawPath(Path().apply { moveTo(12f,2.5f); lineTo(20f,6f); lineTo(20f,12f); cubicTo(20f,17f,16f,20f,12f,22f); cubicTo(8f,20f,4f,17f,4f,12f); lineTo(4f,6f); close() }, tint, style = stroke)
                    line(8f,12f,11f,15f); line(11f,15f,16f,9f)
                }
                "blocks" -> { listOf(6f,12f,18f).forEach { line(3f,it,21f,it) }; drawCircle(Paper,3f,Offset(8f,6f)); drawCircle(tint,3f,Offset(8f,6f),style=stroke); drawCircle(Paper,3f,Offset(16f,12f)); drawCircle(tint,3f,Offset(16f,12f),style=stroke); drawCircle(Paper,3f,Offset(9f,18f)); drawCircle(tint,3f,Offset(9f,18f),style=stroke) }
                "activity" -> { line(4f,20f,4f,13f); line(12f,20f,12f,4f); line(20f,20f,20f,9f) }
                "settings" -> { drawCircle(tint,7f,Offset(12f,12f),style=stroke); drawCircle(tint,2.5f,Offset(12f,12f),style=stroke); for(i in 0..7) { val a=i*Math.PI/4; val x=kotlin.math.cos(a).toFloat(); val y=kotlin.math.sin(a).toFloat(); line(12+8*x,12+8*y,12+10*x,12+10*y) } }
                "arrow" -> { line(5f,12f,19f,12f); line(14f,7f,19f,12f); line(14f,17f,19f,12f) }
                "chevron" -> { line(9f,6f,15f,12f); line(15f,12f,9f,18f) }
                "plus" -> { line(12f,5f,12f,19f); line(5f,12f,19f,12f) }
                "close" -> { line(6f,6f,18f,18f); line(18f,6f,6f,18f) }
                "check" -> { line(5f,12f,10f,17f); line(10f,17f,19f,7f) }
                "pause" -> { drawRoundRect(tint,Offset(6f,5f),Size(4f,14f),CornerRadius(1f)); drawRoundRect(tint,Offset(14f,5f),Size(4f,14f),CornerRadius(1f)) }
                "play" -> drawPath(Path().apply { moveTo(8f,5f); lineTo(19f,12f); lineTo(8f,19f); close() }, tint)
                "clock" -> { drawCircle(tint,9f,Offset(12f,12f),style=stroke); line(12f,7f,12f,12f); line(12f,12f,16f,14f) }
                "globe" -> { drawCircle(tint,9f,Offset(12f,12f),style=stroke); drawOval(tint,Offset(8f,3f),Size(8f,18f),style=stroke); line(3f,12f,21f,12f); line(5f,7f,19f,7f); line(5f,17f,19f,17f) }
                "lock" -> { drawRoundRect(tint,Offset(5f,10f),Size(14f,11f),CornerRadius(3f),style=stroke); drawArc(tint,180f,180f,false,Offset(8f,2f),Size(8f,14f),style=stroke); drawCircle(tint,1f,Offset(12f,15f)) }
                "info" -> { drawCircle(tint,9f,Offset(12f,12f),style=stroke); line(12f,11f,12f,17f); drawCircle(tint,1f,Offset(12f,7f)) }
                "cup" -> { drawPath(Path().apply { moveTo(4f,8f); lineTo(17f,8f); lineTo(16f,16f); quadraticTo(10.5f,21f,5f,16f); close() },tint,style=stroke); drawArc(tint,-90f,180f,false,Offset(15f,8f),Size(7f,7f),style=stroke); line(4f,22f,18f,22f); line(8f,2f,8f,4f); line(13f,2f,13f,4f) }
                "leaf" -> { drawPath(Path().apply { moveTo(5f,19f); cubicTo(0f,9f,8f,3f,21f,3f); cubicTo(21f,16f,15f,23f,5f,19f); close() }, tint); drawLine(Paper,Offset(5f,20f),Offset(16f,8f),1.5f,StrokeCap.Round) }
                "trash" -> { line(5f,7f,19f,7f); line(9f,4f,15f,4f); drawPath(Path().apply { moveTo(7f,7f); lineTo(8f,21f); lineTo(16f,21f); lineTo(17f,7f) },tint,style=stroke); line(10f,11f,10f,17f); line(14f,11f,14f,17f) }
            }
        }
    }
}

@Composable
fun FeedIcon(rule: FeedRule, size: Dp = 44.dp) {
    val bg = when(rule) { FeedRule.YOUTUBE -> Color(0xFFFDEAE8); FeedRule.INSTAGRAM -> Color(0xFFF7E9EE); FeedRule.FACEBOOK -> Color(0xFFE9F0FD); FeedRule.TIKTOK -> Color(0xFFECEFEF) }
    Box(Modifier.size(size).background(bg,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center) {
        when(rule) {
            FeedRule.YOUTUBE -> Canvas(Modifier.size(24.dp)) { drawRoundRect(Color(0xFFE55247), Offset(0f,this.size.height*.16f),Size(this.size.width,this.size.height*.68f),CornerRadius(6f)); drawPath(Path().apply { moveTo(this@Canvas.size.width*.4f,this@Canvas.size.height*.33f); lineTo(this@Canvas.size.width*.68f,this@Canvas.size.height*.5f); lineTo(this@Canvas.size.width*.4f,this@Canvas.size.height*.67f); close() },Color.White) }
            FeedRule.INSTAGRAM -> Canvas(Modifier.size(24.dp)) { val w=this.size.width; drawRoundRect(Color(0xFFB55279),cornerRadius=CornerRadius(w*.27f),style=Stroke(w*.09f)); drawCircle(Color(0xFFB55279),w*.23f,style=Stroke(w*.09f)); drawCircle(Color(0xFFB55279),w*.06f,Offset(w*.77f,w*.23f)) }
            FeedRule.FACEBOOK -> Text("f",fontWeight=FontWeight.Bold,fontSize=31.sp,color=Color(0xFF3976C7))
            FeedRule.TIKTOK -> Text("♪",fontWeight=FontWeight.Bold,fontSize=29.sp,color=Ink)
        }
    }
}

@Composable
fun BotanicalArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        drawCircle(Color(0xFF37634B),w*.47f,Offset(w*.67f,h*.5f),style=Stroke(1.dp.toPx()))
        drawCircle(Color(0xFF37634B),w*.63f,Offset(w*.67f,h*.5f),style=Stroke(1.dp.toPx()))
        drawCircle(Color(0xFF37634B),w*.8f,Offset(w*.67f,h*.5f),style=Stroke(1.dp.toPx()))
        drawPath(Path().apply { moveTo(w*.46f,h*.9f); cubicTo(w*.4f,h*.65f,w*.64f,h*.4f,w*.72f,h*.15f) },Color(0xFFAFCB8E),style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
        drawPath(Path().apply { moveTo(w*.57f,h*.57f); cubicTo(w*.25f,h*.57f,w*.27f,h*.28f,w*.31f,h*.21f); cubicTo(w*.55f,h*.27f,w*.63f,h*.41f,w*.57f,h*.57f); close() },Color(0xFFBBD58F))
        drawPath(Path().apply { moveTo(w*.61f,h*.47f); cubicTo(w*.63f,h*.22f,w*.85f,h*.18f,w*.98f,h*.21f); cubicTo(w*.92f,h*.46f,w*.77f,h*.54f,w*.61f,h*.47f); close() },Color(0xFFDAE9AC))
        drawPath(Path().apply { moveTo(w*.48f,h*.75f); cubicTo(w*.53f,h*.53f,w*.78f,h*.51f,w*.89f,h*.56f); cubicTo(w*.82f,h*.76f,w*.68f,h*.82f,w*.48f,h*.75f); close() },Color(0xFF87AD73))
    }
}

package com.sandboxuser.distractfree.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF19392E)
val Forest = Color(0xFF245C43)
val Sage = Color(0xFFE9EEDF)
val Paper = Color(0xFFF8F9F4)
val Muted = Color(0xFF737D73)
val Outline = Color(0xFFE3E7DE)
val Lime = Color(0xFFD7EDAA)
val Amber = Color(0xFF946320)

@Composable
fun DistractfreeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Forest, onPrimary = Color.White, primaryContainer = Sage,
            onPrimaryContainer = Ink, secondary = Forest, background = Paper,
            onBackground = Ink, surface = Paper, onSurface = Ink,
            surfaceVariant = Sage, onSurfaceVariant = Muted,
            outline = Outline, outlineVariant = Outline
        ),
        typography = Typography,
        content = content
    )
}

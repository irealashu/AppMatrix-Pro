package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppTypeTag(isSystemApp: Boolean, isCompact: Boolean = false) {
    val isDark = isSystemInDarkTheme()

    // Distinct Colors:
    // USER APP: Vibrant Emerald / Mint
    // SYSTEM APP: Bold Red
    val bgColor = if (isSystemApp) {
        if (isDark) Color(0xFF450A0A) else Color(0xFFFEE2E2)
    } else {
        if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
    }

    val textColor = if (isSystemApp) {
        if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
    } else {
        if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
    }

    val icon = if (isSystemApp) Icons.Default.Shield else Icons.Default.Person
    val label = if (isSystemApp) {
        if (isCompact) "SYSTEM" else "SYSTEM APP"
    } else {
        if (isCompact) "USER" else "USER APP"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(
                horizontal = if (isCompact) 6.dp else 8.dp,
                vertical = if (isCompact) 2.dp else 4.dp
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(if (isCompact) 11.dp else 13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (isCompact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        )
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BillItem
import com.example.data.local.WarrantyStatus
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusExpiredRed
import com.example.ui.theme.StatusExpiredRedBg
import com.example.ui.theme.StatusExpiringAmber
import com.example.ui.theme.StatusExpiringAmberBg

@Composable
fun WarrantyBadge(
    bill: BillItem,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val status = bill.getWarrantyStatus()
    val daysRemaining = bill.getDaysRemaining()

    val (bg, fg, icon, text) = when (status) {
        WarrantyStatus.ACTIVE -> {
            val label = if (daysRemaining > 365) {
                "${daysRemaining / 365}y ${(daysRemaining % 365) / 30}m ${strings.timeLeft}"
            } else {
                "$daysRemaining ${strings.daysLeft}"
            }
            Quadruple(
                StatusActiveGreenBg,
                StatusActiveGreen,
                Icons.Default.CheckCircle,
                label
            )
        }
        WarrantyStatus.EXPIRING_SOON -> {
            val label = if (daysRemaining <= 0) {
                strings.expiringToday
            } else {
                "$daysRemaining ${strings.daysLeft} (${strings.expiringSoon})"
            }
            Quadruple(
                StatusExpiringAmberBg,
                StatusExpiringAmber,
                Icons.Default.Warning,
                label
            )
        }
        WarrantyStatus.EXPIRED -> {
            Quadruple(
                StatusExpiredRedBg,
                StatusExpiredRed,
                Icons.Default.HourglassBottom,
                strings.expiredStatus
            )
        }
        WarrantyStatus.LIFETIME -> {
            Quadruple(
                StatusActiveGreenBg,
                StatusActiveGreen,
                Icons.Default.CheckCircle,
                strings.lifetime
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

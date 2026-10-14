package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WarrantyStatus
import com.example.ui.VaultStats
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusExpiredRed
import com.example.ui.theme.StatusExpiringAmber

@Composable
fun StatCardsRow(
    stats: VaultStats,
    selectedStatus: WarrantyStatus?,
    onStatusSelected: (WarrantyStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            StatCard(
                title = strings.totalBills,
                count = stats.totalBills,
                color = MaterialTheme.colorScheme.primary,
                isSelected = selectedStatus == null,
                onClick = { onStatusSelected(null) },
                tag = "stat_total_bills"
            )
        }
        item {
            StatCard(
                title = strings.active,
                count = stats.activeCount,
                color = StatusActiveGreen,
                isSelected = selectedStatus == WarrantyStatus.ACTIVE,
                onClick = {
                    if (selectedStatus == WarrantyStatus.ACTIVE) onStatusSelected(null)
                    else onStatusSelected(WarrantyStatus.ACTIVE)
                },
                tag = "stat_active_bills"
            )
        }
        item {
            StatCard(
                title = strings.expiringSoon,
                count = stats.expiringSoonCount,
                color = StatusExpiringAmber,
                isSelected = selectedStatus == WarrantyStatus.EXPIRING_SOON,
                onClick = {
                    if (selectedStatus == WarrantyStatus.EXPIRING_SOON) onStatusSelected(null)
                    else onStatusSelected(WarrantyStatus.EXPIRING_SOON)
                },
                tag = "stat_expiring_bills"
            )
        }
        item {
            StatCard(
                title = strings.expired,
                count = stats.expiredCount,
                color = StatusExpiredRed,
                isSelected = selectedStatus == WarrantyStatus.EXPIRED,
                onClick = {
                    if (selectedStatus == WarrantyStatus.EXPIRED) onStatusSelected(null)
                    else onStatusSelected(WarrantyStatus.EXPIRED)
                },
                tag = "stat_expired_bills"
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val borderColor = if (isSelected) color else Color.Transparent

    Card(
        modifier = Modifier
            .width(112.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

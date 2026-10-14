package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BillItem
import com.example.data.local.WarrantyStatus
import com.example.ui.BillVaultViewModel
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.StatusActiveGreen as SuccessGreen
import com.example.ui.theme.StatusExpiringAmber as AmberColor
import com.example.ui.theme.StatusExpiredRed as DangerRed
import java.text.NumberFormat
import java.util.Locale

// Store spending summary data model
data class StoreSpendSummary(
    val storeName: String,
    val totalAmount: Double,
    val billCount: Int,
    val percentageOfTotal: Float,
    val color: Color
)

// Expiration timeframe bucket
data class ExpirationBucket(
    val title: String,
    val count: Int,
    val totalValue: Double,
    val color: Color
)

// Upcoming expiring item
data class UpcomingExpiryItem(
    val bill: BillItem,
    val daysRemaining: Long,
    val isExpired: Boolean,
    val urgencyColor: Color
)

@Composable
fun DashboardScreen(
    viewModel: BillVaultViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val bills by viewModel.userBills.collectAsState()
    val stats by viewModel.vaultStats.collectAsState()

    // 1. Process Store Spending Data
    val storeSpends = remember(bills) {
        val totalSpending = bills.sumOf { it.price }.coerceAtLeast(1.0)
        val palette = listOf(
            Color(0xFF2563EB), // Primary Blue
            Color(0xFF06B6D4), // Cyan
            Color(0xFF8B5CF6), // Purple
            Color(0xFFF59E0B), // Amber
            Color(0xFF10B981), // Emerald
            Color(0xFFEC4899), // Pink
            Color(0xFF6366F1), // Indigo
            Color(0xFF14B8A6)  // Teal
        )

        bills.groupBy { if (it.storeOrSeller.isNotBlank()) it.storeOrSeller.trim() else "Other / Local" }
            .map { (store, storeBills) ->
                val amount = storeBills.sumOf { it.price }
                val pct = ((amount / totalSpending) * 100f).toFloat()
                Triple(store, amount, storeBills.size) to pct
            }
            .sortedByDescending { it.first.second }
            .mapIndexed { index, (triple, pct) ->
                StoreSpendSummary(
                    storeName = triple.first,
                    totalAmount = triple.second,
                    billCount = triple.third,
                    percentageOfTotal = pct,
                    color = palette[index % palette.size]
                )
            }
    }

    // 2. Process Upcoming Warranty Expirations
    val upcomingExpirations = remember(bills) {
        bills.map { bill ->
            val days = bill.getDaysRemaining()
            val isExpired = days <= 0
            val color = when {
                isExpired -> DangerRed
                days <= 30 -> AmberColor
                days <= 90 -> Color(0xFFFBBF24)
                else -> SuccessGreen
            }
            UpcomingExpiryItem(
                bill = bill,
                daysRemaining = days,
                isExpired = isExpired,
                urgencyColor = color
            )
        }.sortedWith(
            compareBy<UpcomingExpiryItem> { it.isExpired } // Active ones first, or prioritize closest expiry
                .thenBy { it.daysRemaining }
        )
    }

    // 3. Process Expiration Timeframe Buckets for Visual Timeline Chart
    val expirationBuckets = remember(bills) {
        var next30 = 0
        var next30Val = 0.0
        var m1To3 = 0
        var m1To3Val = 0.0
        var m3To6 = 0
        var m3To6Val = 0.0
        var m6To12 = 0
        var m6To12Val = 0.0
        var over1Year = 0
        var over1YearVal = 0.0
        var expiredCount = 0
        var expiredVal = 0.0

        bills.forEach { b ->
            val days = b.getDaysRemaining()
            val price = b.price
            when {
                days <= 0 -> {
                    expiredCount++
                    expiredVal += price
                }
                days in 1..30 -> {
                    next30++
                    next30Val += price
                }
                days in 31..90 -> {
                    m1To3++
                    m1To3Val += price
                }
                days in 91..180 -> {
                    m3To6++
                    m3To6Val += price
                }
                days in 181..365 -> {
                    m6To12++
                    m6To12Val += price
                }
                else -> {
                    over1Year++
                    over1YearVal += price
                }
            }
        }

        listOf(
            ExpirationBucket(strings.next30Days, next30, next30Val, AmberColor),
            ExpirationBucket(strings.oneToThreeMonths, m1To3, m1To3Val, Color(0xFF38BDF8)),
            ExpirationBucket(strings.threeToSixMonths, m3To6, m3To6Val, PrimaryBlue),
            ExpirationBucket(strings.sixToTwelveMonths, m6To12, m6To12Val, SuccessGreen),
            ExpirationBucket(strings.overOneYear, over1Year, over1YearVal, Color(0xFF8B5CF6)),
            ExpirationBucket(strings.expiredStatus, expiredCount, expiredVal, DangerRed)
        )
    }

    val totalSpending = remember(bills) { bills.sumOf { it.price } }
    val avgBillAmount = remember(bills) { if (bills.isNotEmpty()) totalSpending / bills.size else 0.0 }
    val topStoreName = storeSpends.firstOrNull()?.storeName ?: "—"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .padding(bottom = 80.dp)
            .testTag("dashboard_screen")
    ) {
        // Header
        DashboardHeader(
            title = strings.dashboardTitle,
            totalBills = stats.totalBills,
            totalSpend = totalSpending
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (bills.isEmpty()) {
            EmptyDashboardCard(strings.noBillsForDashboard)
        } else {
            // KPI Summary Row (Total Spend, Avg Bill, Expiring Soon, Active Warranties)
            KpiSummaryCardsRow(
                totalSpend = totalSpending,
                avgBill = avgBillAmount,
                expiringSoon = stats.expiringSoonCount,
                activeCount = stats.activeCount,
                topStore = topStoreName,
                currency = bills.firstOrNull()?.currency ?: "₹"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CHART 1: Total Spending Grouped by Store
            StoreSpendingCard(
                storeSpends = storeSpends,
                totalSpending = totalSpending,
                title = strings.spendingByStore,
                currency = bills.firstOrNull()?.currency ?: "₹"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CHART 2: Warranty Expiration Timeline (Timeframe Buckets Chart)
            ExpirationTimelineBucketsCard(
                buckets = expirationBuckets,
                totalBills = bills.size,
                title = strings.expirationTimeline
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: Upcoming Warranty Expirations List (Countdown & Detail Preview)
            UpcomingExpirationsCard(
                items = upcomingExpirations,
                title = strings.upcomingExpirations,
                onBillClick = { bill -> viewModel.selectBillForDetail(bill) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 4: Category Distribution
            CategoryDistributionCard(
                bills = bills,
                totalSpending = totalSpending,
                title = strings.spendingByCategory
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    title: String,
    totalBills: Int,
    totalSpend: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$totalBills bills & warranties tracked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KpiSummaryCardsRow(
    totalSpend: Double,
    avgBill: Double,
    expiringSoon: Int,
    activeCount: Int,
    topStore: String,
    currency: String
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total Spending KPI
            KpiMetricTile(
                modifier = Modifier.weight(1f),
                title = "Total Spending",
                value = "$currency ${numberFormat.format(totalSpend.toLong())}",
                subtext = "across all receipts",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = PrimaryBlue,
                testTag = "kpi_total_spending"
            )

            // Avg Bill Amount KPI
            KpiMetricTile(
                modifier = Modifier.weight(1f),
                title = "Avg. Receipt",
                value = "$currency ${numberFormat.format(avgBill.toLong())}",
                subtext = "per purchase",
                icon = Icons.Default.Storefront,
                accentColor = Color(0xFF06B6D4),
                testTag = "kpi_avg_receipt"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Expiring Soon (Critical KPI)
            KpiMetricTile(
                modifier = Modifier.weight(1f),
                title = "Expiring Soon",
                value = "$expiringSoon",
                subtext = if (expiringSoon > 0) "Needs attention in 30d" else "All warranties safe",
                icon = Icons.Default.WarningAmber,
                accentColor = if (expiringSoon > 0) AmberColor else SuccessGreen,
                testTag = "kpi_expiring_soon"
            )

            // Active Warranties KPI
            KpiMetricTile(
                modifier = Modifier.weight(1f),
                title = "Active Protection",
                value = "$activeCount",
                subtext = "items under warranty",
                icon = Icons.Default.Shield,
                accentColor = SuccessGreen,
                testTag = "kpi_active_warranties"
            )
        }
    }
}

@Composable
private fun KpiMetricTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * VISUAL BAR CHART: Total Spending Grouped by Store
 * Renders an interactive bar graph showing spending amount, bill count, and percentage per store.
 */
@Composable
private fun StoreSpendingCard(
    storeSpends: List<StoreSpendSummary>,
    totalSpending: Double,
    title: String,
    currency: String
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_store_spending"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Total badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${storeSpends.size} stores",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bars for each store
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                storeSpends.take(6).forEach { store ->
                    val animatedProgress by animateFloatAsState(
                        targetValue = (store.percentageOfTotal / 100f).coerceIn(0.04f, 1f),
                        animationSpec = tween(durationMillis = 800),
                        label = "store_bar_${store.storeName}"
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(store.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = store.storeName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${store.billCount} bills)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "$currency ${numberFormat.format(store.totalAmount.toLong())}  (${String.format(Locale.getDefault(), "%.1f", store.percentageOfTotal)}%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom colored animated progress bar
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = store.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

/**
 * VISUAL EXPIRATION TIMELINE: Distribution of upcoming warranties across timeframes.
 */
@Composable
private fun ExpirationTimelineBucketsCard(
    buckets: List<ExpirationBucket>,
    totalBills: Int,
    title: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_expiration_timeline"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AmberColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Timeline Distribution",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stacked colored bar showing relative distribution of expiration windows
            val total = totalBills.coerceAtLeast(1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                buckets.forEach { b ->
                    if (b.count > 0) {
                        val weight = (b.count.toFloat() / total).coerceAtLeast(0.02f)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(14.dp)
                                .background(b.color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeframe buckets grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buckets.forEach { b ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(b.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = b.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = b.color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${b.count} items",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = b.color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * UPCOMING EXPIRATIONS LIST: Chronological list of products with closest warranty expiration dates.
 */
@Composable
private fun UpcomingExpirationsCard(
    items: List<UpcomingExpiryItem>,
    title: String,
    onBillClick: (BillItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("upcoming_expirations_list_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Sorted by Date",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(5).forEach { item ->
                    val bill = item.bill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { onBillClick(bill) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bill.productName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (bill.brand.isNotBlank()) {
                                    Text(
                                        text = bill.brand,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = " • ",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Exp: ${bill.expiryDate}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Days badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = item.urgencyColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (item.isExpired) "Expired" else "${item.daysRemaining} days left",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = item.urgencyColor
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * VISUAL CATEGORY SPENDING BREAKDOWN
 */
@Composable
private fun CategoryDistributionCard(
    bills: List<BillItem>,
    totalSpending: Double,
    title: String
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val categories = remember(bills) {
        val total = totalSpending.coerceAtLeast(1.0)
        bills.groupBy { if (it.category.isNotBlank()) it.category else "Other" }
            .map { (cat, catBills) ->
                val sum = catBills.sumOf { it.price }
                Triple(cat, sum, ((sum / total) * 100f).toFloat())
            }
            .sortedByDescending { it.second }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_category_spending"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categories.forEach { (cat, sum, pct) ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cat,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "₹ ${numberFormat.format(sum.toLong())}  (${String.format(Locale.getDefault(), "%.1f", pct)}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (pct / 100f).coerceIn(0.02f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboardCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.BillItem
import com.example.data.local.WarrantyStatus
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusActiveGreenBg
import com.example.ui.theme.StatusExpiredRed
import com.example.ui.theme.StatusExpiredRedBg
import com.example.ui.theme.StatusExpiringAmber
import com.example.ui.theme.StatusExpiringAmberBg
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailDialog(
    bill: BillItem,
    onDismiss: () -> Unit,
    onEdit: (BillItem) -> Unit,
    onDelete: (BillItem) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val clipboardManager = LocalClipboardManager.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isFullImageOpen by remember { mutableStateOf(false) }

    val status = bill.getWarrantyStatus()
    val daysRemaining = bill.getDaysRemaining()
    val progress = bill.getWarrantyProgress()

    val (statusBg, statusFg, statusTitle) = when (status) {
        WarrantyStatus.ACTIVE -> Triple(
            StatusActiveGreenBg,
            StatusActiveGreen,
            if (daysRemaining > 365) "${strings.active} • ${daysRemaining / 365}y ${(daysRemaining % 365) / 30}m ${strings.daysLeft}" else "${strings.active} • $daysRemaining ${strings.daysLeft}"
        )
        WarrantyStatus.EXPIRING_SOON -> Triple(
            StatusExpiringAmberBg,
            StatusExpiringAmber,
            if (daysRemaining <= 0) strings.expiringToday else "${strings.expiringSoon} • $daysRemaining ${strings.daysLeft}"
        )
        WarrantyStatus.EXPIRED -> Triple(
            StatusExpiredRedBg,
            StatusExpiredRed,
            strings.expiredStatus
        )
        WarrantyStatus.LIFETIME -> Triple(
            StatusActiveGreenBg,
            StatusActiveGreen,
            strings.lifetime
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.billDetailsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_detail_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // Receipt Photo Preview (if present)
                    val hasImage = bill.imageUri.isNotBlank() && File(bill.imageUri).exists()
                    if (hasImage) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clickable { isFullImageOpen = true }
                                .testTag("bill_photo_preview"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = File(bill.imageUri),
                                    contentDescription = "Bill Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = strings.tapToViewFull,
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Product Title & Brand
                    Text(
                        text = bill.productName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (bill.brand.isNotBlank() || bill.category.isNotBlank()) {
                        Text(
                            text = listOfNotNull(
                                bill.brand.takeIf { it.isNotBlank() },
                                bill.category.takeIf { it.isNotBlank() }
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Warranty Hero Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = statusBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = statusFg,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusTitle,
                                    color = statusFg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = statusFg,
                                trackColor = statusFg.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${bill.purchaseDate}",
                                    fontSize = 12.sp,
                                    color = statusFg.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "${bill.expiryDate}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusFg
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Key Information Fields
                    InfoRow(icon = Icons.Default.Receipt, label = strings.pricePaid, value = if (bill.price > 0) "${bill.currency} ${"%,.2f".format(bill.price)}" else strings.notRecorded)
                    if (bill.storeOrSeller.isNotBlank()) {
                        InfoRow(icon = Icons.Default.Store, label = strings.storeOrSeller, value = bill.storeOrSeller)
                    }
                    if (bill.invoiceNumber.isNotBlank()) {
                        InfoRowWithCopy(
                            icon = Icons.Default.Tag,
                            label = strings.invoiceNumber,
                            value = bill.invoiceNumber,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(bill.invoiceNumber))
                                Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    if (bill.serialNumber.isNotBlank()) {
                        InfoRowWithCopy(
                            icon = Icons.Default.Tag,
                            label = strings.serialNumber,
                            value = bill.serialNumber,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(bill.serialNumber))
                                Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    if (bill.warrantyType.isNotBlank()) {
                        InfoRow(icon = Icons.Default.Security, label = strings.warrantyType, value = bill.warrantyType)
                    }
                    if (bill.coverageDetails.isNotBlank()) {
                        InfoRow(icon = Icons.Default.Security, label = strings.coverageDetails, value = bill.coverageDetails)
                    }
                    if (bill.supportContact.isNotBlank()) {
                        InfoRowWithAction(
                            icon = Icons.Default.Phone,
                            label = strings.supportContact,
                            value = bill.supportContact,
                            actionLabel = "Call",
                            onAction = {
                                val cleanPhone = bill.supportContact.filter { it.isDigit() || it == '+' }
                                if (cleanPhone.length >= 4) {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                    context.startActivity(dialIntent)
                                } else {
                                    clipboardManager.setText(AnnotatedString(bill.supportContact))
                                    Toast.makeText(context, strings.copied, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    if (bill.notes.isNotBlank()) {
                        InfoRow(icon = Icons.Default.Receipt, label = strings.notes, value = bill.notes)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: Share, Edit, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shareText = buildString {
                                    appendLine("📋 ${strings.billDetailsTitle}")
                                    appendLine("Product: ${bill.productName}")
                                    if (bill.brand.isNotBlank()) appendLine("Brand: ${bill.brand}")
                                    appendLine("Purchase: ${bill.purchaseDate}")
                                    appendLine("Expiry: ${bill.expiryDate}")
                                    if (bill.invoiceNumber.isNotBlank()) appendLine("Invoice: ${bill.invoiceNumber}")
                                    if (bill.serialNumber.isNotBlank()) appendLine("Serial: ${bill.serialNumber}")
                                    if (bill.supportContact.isNotBlank()) appendLine("Support: ${bill.supportContact}")
                                    appendLine("BillVault")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, strings.share))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_bill_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.share)
                        }

                        Button(
                            onClick = { onEdit(bill) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_bill_button")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.edit)
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusExpiredRed),
                            modifier = Modifier.testTag("delete_bill_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteConfirmTitle) },
            text = { Text(strings.deleteConfirmMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(bill)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusExpiredRed),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (isFullImageOpen && bill.imageUri.isNotBlank()) {
        Dialog(
            onDismissRequest = { isFullImageOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = File(bill.imageUri),
                    contentDescription = "Full Receipt",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { isFullImageOpen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun InfoRowWithCopy(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun InfoRowWithAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        TextButton(onClick = onAction) {
            Text(actionLabel, fontSize = 12.sp)
        }
    }
}

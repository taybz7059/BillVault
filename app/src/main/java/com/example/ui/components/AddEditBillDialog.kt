package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.ai.ScannedBillResult
import com.example.data.local.BillItem
import com.example.ui.localization.LocalAppStrings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(
    initialBill: BillItem?,
    scannedData: ScannedBillResult?,
    imagePath: String?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        productName: String,
        brand: String,
        storeOrSeller: String,
        category: String,
        purchaseDate: String,
        warrantyPeriodMonths: Int,
        expiryDate: String,
        price: Double,
        currency: String,
        invoiceNumber: String,
        serialNumber: String,
        warrantyType: String,
        coverageDetails: String,
        supportContact: String,
        notes: String,
        imageUri: String
    ) -> Unit
) {
    val strings = LocalAppStrings.current
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())

    val defaultProductName = initialBill?.productName ?: scannedData?.productName ?: ""
    val defaultBrand = initialBill?.brand ?: scannedData?.brand ?: ""
    val defaultStore = initialBill?.storeOrSeller ?: scannedData?.storeOrSeller ?: ""
    val defaultCategory = initialBill?.category ?: scannedData?.category ?: "Electronics"
    val defaultPurchaseDate = initialBill?.purchaseDate ?: scannedData?.purchaseDate?.takeIf { it.isNotBlank() } ?: today
    val defaultMonths = initialBill?.warrantyPeriodMonths ?: scannedData?.warrantyPeriodMonths ?: 12

    val defaultExpiryDate = initialBill?.expiryDate ?: scannedData?.expiryDate?.takeIf { it.isNotBlank() } ?: run {
        calculateExpiryDate(defaultPurchaseDate, defaultMonths)
    }

    val defaultPrice = (initialBill?.price ?: scannedData?.price ?: 0.0).let {
        if (it > 0.0) it.toString() else ""
    }
    val defaultCurrency = initialBill?.currency ?: scannedData?.currency ?: "₹"
    val defaultInvoice = initialBill?.invoiceNumber ?: scannedData?.invoiceNumber ?: ""
    val defaultSerial = initialBill?.serialNumber ?: scannedData?.serialNumber ?: ""
    val defaultWarrantyType = initialBill?.warrantyType ?: scannedData?.warrantyType ?: "Manufacturer Warranty"
    val defaultCoverage = initialBill?.coverageDetails ?: scannedData?.coverageDetails ?: ""
    val defaultSupport = initialBill?.supportContact ?: scannedData?.supportContact ?: ""
    val defaultNotes = initialBill?.notes ?: scannedData?.notes ?: ""
    val defaultImage = imagePath ?: initialBill?.imageUri ?: ""

    var productName by remember { mutableStateOf(defaultProductName) }
    var brand by remember { mutableStateOf(defaultBrand) }
    var storeOrSeller by remember { mutableStateOf(defaultStore) }
    var category by remember { mutableStateOf(defaultCategory) }
    var purchaseDate by remember { mutableStateOf(defaultPurchaseDate) }
    var warrantyPeriodMonths by remember { mutableStateOf(defaultMonths) }
    var expiryDate by remember { mutableStateOf(defaultExpiryDate) }
    var priceText by remember { mutableStateOf(defaultPrice) }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var invoiceNumber by remember { mutableStateOf(defaultInvoice) }
    var serialNumber by remember { mutableStateOf(defaultSerial) }
    var warrantyType by remember { mutableStateOf(defaultWarrantyType) }
    var coverageDetails by remember { mutableStateOf(defaultCoverage) }
    var supportContact by remember { mutableStateOf(defaultSupport) }
    var notes by remember { mutableStateOf(defaultNotes) }

    var productError by remember { mutableStateOf(false) }

    val categories = listOf(
        "Electronics",
        "Home Appliances",
        "Mobile & Gadgets",
        "Computers & Laptops",
        "Vehicles & Auto",
        "Furniture & Home",
        "Fashion & Watch",
        "Other"
    )

    fun applyMonths(months: Int) {
        warrantyPeriodMonths = months
        expiryDate = calculateExpiryDate(purchaseDate, months)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialBill != null) strings.editBillTitle else strings.addBillTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_edit_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // AI Detected Banner
                    if (scannedData != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.aiExtractedBanner,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Attached Receipt Image Preview
                    if (defaultImage.isNotBlank() && File(defaultImage).exists()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = File(defaultImage),
                                contentDescription = "Attached Bill",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Product Name (Mandatory)
                    OutlinedTextField(
                        value = productName,
                        onValueChange = {
                            productName = it
                            if (it.isNotBlank()) productError = false
                        },
                        label = { Text(strings.productNameLabel) },
                        placeholder = { Text(strings.productNamePlaceholder) },
                        isError = productError,
                        supportingText = if (productError) { { Text("Required") } } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Brand & Store
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text(strings.brandLabel) },
                            placeholder = { Text(strings.brandPlaceholder) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("brand_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = storeOrSeller,
                            onValueChange = { storeOrSeller = it },
                            label = { Text(strings.storeLabel) },
                            placeholder = { Text(strings.storePlaceholder) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("store_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selector
                    Text(strings.categoryLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category.equals(cat, ignoreCase = true),
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                modifier = Modifier.testTag("category_chip_$cat")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Purchase Date & Expiry Date
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = purchaseDate,
                            onValueChange = {
                                purchaseDate = it
                                expiryDate = calculateExpiryDate(it, warrantyPeriodMonths)
                            },
                            label = { Text(strings.purchaseDateLabel) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("purchase_date_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            label = { Text(strings.expiryDateLabel) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("expiry_date_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Warranty Duration Buttons
                    Text(strings.quickDuration, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(6 to "6 Mos", 12 to "1 Year", 24 to "2 Years", 36 to "3 Years", 60 to "5 Years", 1200 to "Lifetime").forEach { (months, label) ->
                            FilterChip(
                                selected = warrantyPeriodMonths == months,
                                onClick = { applyMonths(months) },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Price & Currency
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text(strings.currencyLabel) },
                            modifier = Modifier.width(90.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text(strings.pricePaidLabel) },
                            placeholder = { Text("45000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("price_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Invoice Number & Serial Number
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text(strings.invoiceNumber) },
                            placeholder = { Text("INV-2024-88") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("invoice_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = serialNumber,
                            onValueChange = { serialNumber = it },
                            label = { Text(strings.serialNumber) },
                            placeholder = { Text("SN-998811") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("serial_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Warranty Type & Support Contact
                    OutlinedTextField(
                        value = warrantyType,
                        onValueChange = { warrantyType = it },
                        label = { Text(strings.warrantyType) },
                        placeholder = { Text("1 Year Machine + 5 Year Motor") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = supportContact,
                        onValueChange = { supportContact = it },
                        label = { Text(strings.supportContact) },
                        placeholder = { Text("1800-103-7799 / care@brand.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(strings.notes) },
                        placeholder = { Text("Bill notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (productName.isBlank()) {
                                productError = true
                            } else {
                                val priceVal = priceText.toDoubleOrNull() ?: 0.0
                                onSave(
                                    initialBill?.id ?: 0L,
                                    productName.trim(),
                                    brand.trim(),
                                    storeOrSeller.trim(),
                                    category.trim(),
                                    purchaseDate.trim(),
                                    warrantyPeriodMonths,
                                    expiryDate.trim(),
                                    priceVal,
                                    currency.trim(),
                                    invoiceNumber.trim(),
                                    serialNumber.trim(),
                                    warrantyType.trim(),
                                    coverageDetails.trim(),
                                    supportContact.trim(),
                                    notes.trim(),
                                    defaultImage
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_bill_button")
                    ) {
                        Text(if (initialBill != null) strings.update else strings.saveToVault, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

private fun calculateExpiryDate(purchaseDateStr: String, months: Int): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val pDate = sdf.parse(purchaseDateStr) ?: Date()
        val cal = Calendar.getInstance().apply {
            time = pDate
            add(Calendar.MONTH, months)
        }
        sdf.format(cal.time)
    } catch (e: Exception) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, months)
        }
        sdf.format(cal.time)
    }
}

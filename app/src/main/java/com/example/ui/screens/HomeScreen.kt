package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.BillImageStorage
import com.example.ui.BillVaultViewModel
import com.example.ui.components.AddEditBillDialog
import com.example.ui.components.BillCard
import com.example.ui.components.BillDetailDialog
import com.example.ui.components.ScanOptionsBottomSheet
import com.example.ui.components.ScanningOverlayDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StatCardsRow
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.localization.LocalAppStrings
import kotlinx.coroutines.launch

enum class NavTab {
    HOME,
    DASHBOARD,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BillVaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(NavTab.HOME) }

    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()
    val filteredBills by viewModel.filteredBills.collectAsState()
    val vaultStats by viewModel.vaultStats.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val activeScannedResult by viewModel.activeScannedResult.collectAsState()
    val scannedImagePath by viewModel.scannedImagePath.collectAsState()

    val selectedBillForDetail by viewModel.selectedBillForDetail.collectAsState()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsState()
    val editingBill by viewModel.editingBill.collectAsState()
    val isSettingsDialogVisible by viewModel.isSettingsDialogVisible.collectAsState()

    var showScanBottomSheet by remember { mutableStateOf(false) }
    var showCameraScreen by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle back button when on Settings tab
    BackHandler(enabled = currentTab != NavTab.HOME) {
        currentTab = NavTab.HOME
    }

    // Temporary camera capture URI
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            val bitmap = BillImageStorage.loadBitmapFromUri(context, cameraTempUri!!)
            if (bitmap != null) {
                val savedPath = BillImageStorage.saveBitmapToFile(context, bitmap)
                viewModel.scanReceiptWithAI(bitmap, savedPath)
            } else {
                Toast.makeText(context, "Could not load captured photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Photo Gallery picker (zero permission photo picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val processed = BillImageStorage.processUri(context, uri)
            if (processed != null) {
                viewModel.scanReceiptWithAI(processed.first, processed.second)
            } else {
                Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // PDF / Document picker launcher for bills and warranty invoices
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val processed = BillImageStorage.processUri(context, uri)
            if (processed != null) {
                viewModel.scanReceiptWithAI(processed.first, processed.second)
            } else {
                Toast.makeText(context, "Could not render or read PDF bill. Please check file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Show scan error snackbar if error happens
    scanError?.let { err ->
        scope.launch {
            snackbarHostState.showSnackbar(err)
            viewModel.clearScanError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Material 3 Bottom Navigation Bar with Home, Scan, and Settings
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.HOME,
                    onClick = { currentTab = NavTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = strings.navHome) },
                    label = { Text(strings.navHome, fontWeight = if (currentTab == NavTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_home"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.DASHBOARD,
                    onClick = { currentTab = NavTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Insights, contentDescription = strings.navDashboard) },
                    label = { Text(strings.navDashboard, fontWeight = if (currentTab == NavTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_dashboard"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { showScanBottomSheet = true },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = strings.navScan,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    label = { Text(strings.navScan, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.testTag("nav_item_scan")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.SETTINGS,
                    onClick = { currentTab = NavTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = strings.navSettings) },
                    label = { Text(strings.navSettings, fontWeight = if (currentTab == NavTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_settings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            NavTab.SETTINGS -> {
                // Full Settings Tab Screen
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavTab.DASHBOARD -> {
                // Visual Summary Dashboard (Spending by Store & Warranty Expirations)
                DashboardScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavTab.HOME -> {
                // Home Vault Bills Tab Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                // Top Bar
                TopBarSection(
                    userName = currentUser?.displayName ?: "",
                    userEmail = currentUserEmail,
                    currentLangNative = currentLang.nativeName,
                    onLanguageClick = { viewModel.openSettingsDialog() },
                    tagline = strings.tagline
                )

                // Search Bar
                SearchBarSection(
                    query = searchQuery,
                    hint = strings.searchHint,
                    onQueryChange = { viewModel.setSearchQuery(it) }
                )

                // Stat Cards Row
                StatCardsRow(
                    stats = vaultStats,
                    selectedStatus = selectedStatusFilter,
                    onStatusSelected = { viewModel.setSelectedStatusFilter(it) },
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Category Filter Chips
                CategoryFilterSection(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.setSelectedCategory(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bills List or Empty State
                if (filteredBills.isEmpty()) {
                    EmptyStateSection(
                        hasSearchQuery = searchQuery.isNotBlank() || selectedCategory != "All" || selectedStatusFilter != null,
                        onResetFilter = {
                            viewModel.setSearchQuery("")
                            viewModel.setSelectedCategory("All")
                            viewModel.setSelectedStatusFilter(null)
                        },
                        onScanClick = { showScanBottomSheet = true },
                        onLoadDemoClick = { viewModel.resetToSampleBills() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("bills_lazy_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredBills, key = { it.id }) { bill ->
                            BillCard(
                                bill = bill,
                                onClick = { viewModel.selectBillForDetail(bill) }
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Bottom sheet for scan options
    if (showScanBottomSheet) {
        ScanOptionsBottomSheet(
            sheetState = bottomSheetState,
            onDismiss = { showScanBottomSheet = false },
            onCameraClick = {
                showScanBottomSheet = false
                showCameraScreen = true
            },
            onGalleryClick = {
                showScanBottomSheet = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPdfClick = {
                showScanBottomSheet = false
                pdfPickerLauncher.launch(arrayOf("application/pdf"))
            },
            onManualClick = {
                showScanBottomSheet = false
                viewModel.openAddBillSheet()
            },
            onDemoScanClick = {
                showScanBottomSheet = false
                val demoBitmap = createDemoReceiptBitmap()
                val path = BillImageStorage.saveBitmapToFile(context, demoBitmap)
                viewModel.scanReceiptWithAI(demoBitmap, path)
            }
        )
    }

    // In-app Camera Interface for capturing bills and warranties
    if (showCameraScreen) {
        ReceiptCameraScreen(
            onPhotoCaptured = { bitmap, path ->
                showCameraScreen = false
                viewModel.scanReceiptWithAI(bitmap, path)
            },
            onGalleryPickRequest = {
                showCameraScreen = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDismiss = {
                showCameraScreen = false
            }
        )
    }

    // AI Scanning progress overlay
    if (isScanning) {
        ScanningOverlayDialog()
    }

    // Detail dialog
    selectedBillForDetail?.let { bill ->
        BillDetailDialog(
            bill = bill,
            onDismiss = { viewModel.selectBillForDetail(null) },
            onEdit = {
                viewModel.selectBillForDetail(null)
                viewModel.openEditBillSheet(it)
            },
            onDelete = {
                viewModel.deleteBill(it)
            }
        )
    }

    // Add or Edit Dialog
    if (isAddEditOpen) {
        AddEditBillDialog(
            initialBill = editingBill,
            scannedData = activeScannedResult,
            imagePath = scannedImagePath,
            onDismiss = { viewModel.closeAddEditSheet() },
            onSave = { id, name, brand, store, cat, pDate, months, expDate, price, curr, inv, serial, type, cov, supp, notes, img ->
                viewModel.saveOrUpdateBill(
                    id = id,
                    productName = name,
                    brand = brand,
                    storeOrSeller = store,
                    category = cat,
                    purchaseDate = pDate,
                    warrantyPeriodMonths = months,
                    expiryDate = expDate,
                    price = price,
                    currency = curr,
                    invoiceNumber = inv,
                    serialNumber = serial,
                    warrantyType = type,
                    coverageDetails = cov,
                    supportContact = supp,
                    notes = notes,
                    imageUri = img
                )
            }
        )
    }

    // Settings / Language Dialog (when opened from top language badge)
    if (isSettingsDialogVisible) {
        SettingsDialog(
            currentLanguage = currentLang,
            onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
            currentEmail = currentUserEmail,
            stats = vaultStats,
            onDismiss = { viewModel.closeSettingsDialog() },
            onSaveEmail = { viewModel.setUserEmail(it) },
            onResetSamples = { viewModel.resetToSampleBills() }
        )
    }
}

@Composable
private fun TopBarSection(
    userName: String,
    userEmail: String,
    currentLangNative: String,
    onLanguageClick: () -> Unit,
    tagline: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "BillVault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (userName.isNotBlank() && userName != "Vault User") "Hi, $userName 👋" else tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Language quick switch pill button
        Card(
            onClick = onLanguageClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.testTag("language_toggle_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = currentLangNative,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SearchBarSection(
    query: String,
    hint: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(hint) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("search_bills_input"),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun CategoryFilterSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val categoryList = listOf(
        "All" to strings.categoryAll,
        "Electronics" to strings.categoryElectronics,
        "Home Appliances" to strings.categoryAppliances,
        "Mobile & Gadgets" to strings.categoryMobile,
        "Computers & Laptops" to strings.categoryComputers,
        "Vehicles & Auto" to strings.categoryVehicles,
        "Fashion & Watch" to strings.categoryFashion
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categoryList.forEach { (catKey, catLabel) ->
            FilterChip(
                selected = selectedCategory.equals(catKey, ignoreCase = true),
                onClick = { onCategorySelected(catKey) },
                label = { Text(catLabel, fontSize = 12.sp) },
                modifier = Modifier.testTag("filter_chip_$catKey")
            )
        }
    }
}

@Composable
private fun EmptyStateSection(
    hasSearchQuery: Boolean,
    onResetFilter: () -> Unit,
    onScanClick: () -> Unit,
    onLoadDemoClick: () -> Unit
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (hasSearchQuery) strings.noMatchingBills else strings.emptyTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasSearchQuery) strings.noMatchingBillsSub else strings.emptySub,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (hasSearchQuery) {
            OutlinedButton(onClick = onResetFilter) {
                Text(strings.clearFilters)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onLoadDemoClick) {
                    Text(strings.loadDemoBills)
                }
                androidx.compose.material3.Button(onClick = onScanClick) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.scanBill)
                }
            }
        }
    }
}

// Generates a mock high-contrast invoice bitmap for instant in-app AI testing
private fun createDemoReceiptBitmap(): Bitmap {
    val width = 600
    val height = 800
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    val paint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 28f
        isAntiAlias = true
    }

    canvas.drawText("RELIANCE DIGITAL RETAIL", 120f, 80f, paint)
    paint.textSize = 20f
    canvas.drawText("Tax Invoice / Cash Memo", 170f, 120f, paint)
    canvas.drawText("--------------------------------------------------", 50f, 150f, paint)
    canvas.drawText("Date: 2024-06-20", 50f, 190f, paint)
    canvas.drawText("Invoice No: INV-RD-98214", 50f, 220f, paint)
    canvas.drawText("Item: Samsung 43 Inch Crystal 4K UHD TV", 50f, 270f, paint)
    canvas.drawText("Brand: Samsung", 50f, 305f, paint)
    canvas.drawText("Serial No: SN-SAM-43UHD-7721", 50f, 340f, paint)
    canvas.drawText("Warranty: 24 Months Manufacturer Warranty", 50f, 375f, paint)
    canvas.drawText("Amount Paid: INR 32,990.00", 50f, 415f, paint)
    canvas.drawText("Support: 1800-40-7267864 / support.in@samsung.com", 50f, 460f, paint)
    canvas.drawText("--------------------------------------------------", 50f, 500f, paint)
    canvas.drawText("Keep this bill safely for claiming warranty.", 60f, 540f, paint)

    return bitmap
}

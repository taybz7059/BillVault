package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiReceiptScanner
import com.example.data.ai.ScanResult
import com.example.data.ai.ScannedBillResult
import com.example.data.local.BillDatabase
import com.example.data.local.BillItem
import com.example.data.local.WarrantyStatus
import com.example.data.util.SampleBillsProvider
import com.example.ui.localization.AppLanguage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuthProvider {
    EMAIL,
    GOOGLE,
    APPLE
}

data class UserAccount(
    val email: String,
    val displayName: String,
    val provider: AuthProvider = AuthProvider.EMAIL
)

data class VaultStats(
    val totalBills: Int = 0,
    val activeCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val expiredCount: Int = 0,
    val totalProtectedValue: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class BillVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BillDatabase.getDatabase(application)
    private val billDao = db.billDao()

    // Shared Preferences for persisting language, preferences & auth
    private val prefs = application.getSharedPreferences("billvault_prefs", Application.MODE_PRIVATE)

    private fun loadInitialAuth(): Pair<Boolean, UserAccount> {
        val isLoggedIn = prefs.getBoolean("auth_is_logged_in", true)
        val email = prefs.getString("auth_user_email", "taybzmota15@gmail.com") ?: "taybzmota15@gmail.com"
        val name = prefs.getString("auth_user_name", "Taiyab Mota") ?: "Taiyab Mota"
        val providerStr = prefs.getString("auth_user_provider", AuthProvider.EMAIL.name) ?: AuthProvider.EMAIL.name
        val provider = try {
            AuthProvider.valueOf(providerStr)
        } catch (_: Exception) {
            AuthProvider.EMAIL
        }
        return Pair(isLoggedIn, UserAccount(email, name, provider))
    }

    // Authentication State
    private val initialAuth = loadInitialAuth()
    private val _isLoggedIn = MutableStateFlow(initialAuth.first)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(initialAuth.second)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    // Active user's email for bills querying
    private val _currentUserEmail = MutableStateFlow(initialAuth.second.email)
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<WarrantyStatus?>(null)
    val selectedStatusFilter: StateFlow<WarrantyStatus?> = _selectedStatusFilter.asStateFlow()

    // Scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val _activeScannedResult = MutableStateFlow<ScannedBillResult?>(null)
    val activeScannedResult: StateFlow<ScannedBillResult?> = _activeScannedResult.asStateFlow()

    private val _scannedImagePath = MutableStateFlow<String?>(null)
    val scannedImagePath: StateFlow<String?> = _scannedImagePath.asStateFlow()

    // Selected bill for detail dialog/view
    private val _selectedBillForDetail = MutableStateFlow<BillItem?>(null)
    val selectedBillForDetail: StateFlow<BillItem?> = _selectedBillForDetail.asStateFlow()

    // Add / Edit sheet state
    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _editingBill = MutableStateFlow<BillItem?>(null)
    val editingBill: StateFlow<BillItem?> = _editingBill.asStateFlow()

    // Language state: English, Gujarati, or Hindi
    private val _currentLanguage = MutableStateFlow(loadSavedLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Settings dialog state (contains language selector & profile)
    private val _isSettingsDialogVisible = MutableStateFlow(false)
    val isSettingsDialogVisible: StateFlow<Boolean> = _isSettingsDialogVisible.asStateFlow()

    // Dialog state for switching accounts/email
    private val _isEmailDialogVisible = MutableStateFlow(false)
    val isEmailDialogVisible: StateFlow<Boolean> = _isEmailDialogVisible.asStateFlow()

    // Bills flow for the active user
    val userBills: StateFlow<List<BillItem>> = _currentUserEmail
        .flatMapLatest { email ->
            billDao.getBillsForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered bills based on search query, category, and warranty status
    val filteredBills: StateFlow<List<BillItem>> = combine(
        userBills,
        _searchQuery,
        _selectedCategory,
        _selectedStatusFilter
    ) { bills, query, category, statusFilter ->
        bills.filter { bill ->
            val matchesQuery = query.isBlank() ||
                bill.productName.contains(query, ignoreCase = true) ||
                bill.brand.contains(query, ignoreCase = true) ||
                bill.storeOrSeller.contains(query, ignoreCase = true) ||
                bill.invoiceNumber.contains(query, ignoreCase = true) ||
                bill.serialNumber.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || bill.category.equals(category, ignoreCase = true)

            val matchesStatus = statusFilter == null || bill.getWarrantyStatus() == statusFilter

            matchesQuery && matchesCategory && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall stats for the user's vault
    val vaultStats: StateFlow<VaultStats> = userBills.combine(_currentUserEmail) { bills, _ ->
        var active = 0
        var expiringSoon = 0
        var expired = 0
        var totalValue = 0.0

        bills.forEach { item ->
            totalValue += item.price
            when (item.getWarrantyStatus()) {
                WarrantyStatus.ACTIVE -> active++
                WarrantyStatus.EXPIRING_SOON -> expiringSoon++
                WarrantyStatus.EXPIRED -> expired++
                WarrantyStatus.LIFETIME -> active++
            }
        }

        VaultStats(
            totalBills = bills.size,
            activeCount = active,
            expiringSoonCount = expiringSoon,
            expiredCount = expired,
            totalProtectedValue = totalValue
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultStats())

    init {
        checkAndSeedInitialBills()
    }

    private fun checkAndSeedInitialBills() {
        viewModelScope.launch {
            val email = _currentUserEmail.value
            val count = billDao.getBillCount(email)
            if (count == 0) {
                val samples = SampleBillsProvider.getSampleBills(email)
                billDao.insertBills(samples)
            }
        }
    }

    fun setUserEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isNotBlank() && trimmed != _currentUserEmail.value) {
            val curr = _currentUser.value
            val updated = curr?.copy(email = trimmed) ?: UserAccount(trimmed, trimmed.substringBefore("@"), AuthProvider.EMAIL)
            saveAccountAndSignIn(updated)
        }
        _isEmailDialogVisible.value = false
    }

    fun loginWithEmail(email: String, password: String, displayName: String? = null): Boolean {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return false
        }
        val name = displayName?.trim().takeIf { !it.isNullOrBlank() }
            ?: trimmedEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val account = UserAccount(trimmedEmail, name, AuthProvider.EMAIL)
        saveAccountAndSignIn(account)
        return true
    }

    fun registerWithEmail(email: String, password: String, displayName: String): Boolean {
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return false
        }
        val finalName = if (trimmedName.isNotBlank()) trimmedName else trimmedEmail.substringBefore("@")
        val account = UserAccount(trimmedEmail, finalName, AuthProvider.EMAIL)
        saveAccountAndSignIn(account)
        return true
    }

    fun loginWithApple(appleId: String, fullName: String = "") {
        val trimmedId = appleId.trim()
        val email = if (trimmedId.contains("@")) trimmedId else "$trimmedId@icloud.com"
        val name = if (fullName.isNotBlank()) fullName else email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val account = UserAccount(email, name, AuthProvider.APPLE)
        saveAccountAndSignIn(account)
    }

    fun loginWithGoogle(googleEmail: String, fullName: String = "") {
        val trimmedEmail = googleEmail.trim()
        val email = if (trimmedEmail.contains("@")) trimmedEmail else "$trimmedEmail@gmail.com"
        val name = if (fullName.isNotBlank()) fullName else email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val account = UserAccount(email, name, AuthProvider.GOOGLE)
        saveAccountAndSignIn(account)
    }

    fun logout() {
        prefs.edit().putBoolean("auth_is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    private fun saveAccountAndSignIn(account: UserAccount) {
        prefs.edit()
            .putBoolean("auth_is_logged_in", true)
            .putString("auth_user_email", account.email)
            .putString("auth_user_name", account.displayName)
            .putString("auth_user_provider", account.provider.name)
            .apply()

        _currentUser.value = account
        _currentUserEmail.value = account.email
        _isLoggedIn.value = true
        checkAndSeedInitialBills()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedStatusFilter(status: WarrantyStatus?) {
        _selectedStatusFilter.value = status
    }

    private fun loadSavedLanguage(): AppLanguage {
        val code = prefs.getString("selected_language", "en") ?: "en"
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.ENGLISH
    }

    // App Preferences: Notifications & Haptics
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("pref_notifications", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("pref_haptics", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("pref_notifications", enabled).apply()
    }

    fun toggleHaptics(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        prefs.edit().putBoolean("pref_haptics", enabled).apply()
    }

    fun getExportSummary(): String {
        val list: List<BillItem> = userBills.value
        val sb = StringBuilder()
        sb.append("==========================================\n")
        sb.append("            BILLVAULT EXPORT              \n")
        sb.append("==========================================\n")
        sb.append("Owner: ${currentUser.value?.displayName ?: "Taiyab Mota"}\n")
        sb.append("Email: ${currentUser.value?.email ?: currentUserEmail.value}\n")
        sb.append("Total Protected Items: ${list.size}\n")
        val totalVal = list.sumOf { it.price }
        sb.append("Total Vault Value: ₹ ${"%,.2f".format(totalVal)}\n")
        sb.append("Developer: Taiyab Mota\n")
        sb.append("==========================================\n\n")
        list.forEachIndexed { i, bill ->
            sb.append("${i + 1}. ${bill.productName}\n")
            sb.append("   • Brand: ${bill.brand.ifBlank { "N/A" }}\n")
            sb.append("   • Store / Retailer: ${bill.storeOrSeller.ifBlank { "N/A" }}\n")
            sb.append("   • Category: ${bill.category}\n")
            sb.append("   • Purchase Date: ${bill.purchaseDate}\n")
            sb.append("   • Expiry Date: ${bill.expiryDate}\n")
            sb.append("   • Warranty: ${bill.warrantyType} (${bill.warrantyPeriodMonths} months)\n")
            sb.append("   • Price: ${bill.currency} ${bill.price}\n")
            if (bill.invoiceNumber.isNotBlank()) sb.append("   • Invoice No: ${bill.invoiceNumber}\n")
            if (bill.serialNumber.isNotBlank()) sb.append("   • Serial No: ${bill.serialNumber}\n")
            if (bill.supportContact.isNotBlank()) sb.append("   • Support: ${bill.supportContact}\n")
            sb.append("\n")
        }
        return sb.toString()
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("selected_language", language.code).apply()
    }

    fun openSettingsDialog() {
        _isSettingsDialogVisible.value = true
    }

    fun closeSettingsDialog() {
        _isSettingsDialogVisible.value = false
    }

    fun openEmailDialog() {
        _isEmailDialogVisible.value = true
    }

    fun closeEmailDialog() {
        _isEmailDialogVisible.value = false
    }

    fun openAddBillSheet(imagePath: String? = null, prefilled: ScannedBillResult? = null) {
        _editingBill.value = null
        _scannedImagePath.value = imagePath
        _activeScannedResult.value = prefilled
        _isAddEditOpen.value = true
    }

    fun openEditBillSheet(bill: BillItem) {
        _editingBill.value = bill
        _scannedImagePath.value = bill.imageUri
        _activeScannedResult.value = null
        _isAddEditOpen.value = true
    }

    fun closeAddEditSheet() {
        _isAddEditOpen.value = false
        _editingBill.value = null
        _activeScannedResult.value = null
        _scannedImagePath.value = null
        _scanError.value = null
    }

    fun selectBillForDetail(bill: BillItem?) {
        _selectedBillForDetail.value = bill
    }

    fun scanReceiptWithAI(bitmap: Bitmap, savedImagePath: String) {
        _isScanning.value = true
        _scanError.value = null
        _scannedImagePath.value = savedImagePath

        viewModelScope.launch {
            when (val result = GeminiReceiptScanner.scanReceipt(bitmap)) {
                is ScanResult.Success -> {
                    _isScanning.value = false
                    _activeScannedResult.value = result.data
                    _isAddEditOpen.value = true
                }
                is ScanResult.Error -> {
                    _isScanning.value = false
                    _scanError.value = result.message
                    // Still open the add bill screen so user can fill in details manually with the photo attached!
                    _isAddEditOpen.value = true
                }
            }
        }
    }

    fun saveOrUpdateBill(
        id: Long = 0,
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
    ) {
        viewModelScope.launch {
            val email = _currentUserEmail.value
            val item = BillItem(
                id = id,
                userEmail = email,
                productName = productName.ifBlank { "Untitled Product" },
                brand = brand,
                storeOrSeller = storeOrSeller,
                category = category,
                purchaseDate = purchaseDate,
                warrantyPeriodMonths = warrantyPeriodMonths,
                expiryDate = expiryDate,
                price = price,
                currency = currency,
                invoiceNumber = invoiceNumber,
                serialNumber = serialNumber,
                warrantyType = warrantyType,
                coverageDetails = coverageDetails,
                supportContact = supportContact,
                notes = notes,
                imageUri = imageUri
            )

            if (id == 0L) {
                val newId = billDao.insertBill(item)
                // If detail dialog was viewing this, update it
                val inserted = item.copy(id = newId)
                if (_selectedBillForDetail.value != null) {
                    _selectedBillForDetail.value = inserted
                }
            } else {
                billDao.updateBill(item)
                if (_selectedBillForDetail.value?.id == id) {
                    _selectedBillForDetail.value = item
                }
            }
            closeAddEditSheet()
        }
    }

    fun deleteBill(bill: BillItem) {
        viewModelScope.launch {
            billDao.deleteBill(bill)
            if (_selectedBillForDetail.value?.id == bill.id) {
                _selectedBillForDetail.value = null
            }
        }
    }

    fun resetToSampleBills() {
        viewModelScope.launch {
            val email = _currentUserEmail.value
            val samples = SampleBillsProvider.getSampleBills(email)
            billDao.insertBills(samples)
        }
    }

    fun clearScanError() {
        _scanError.value = null
    }
}

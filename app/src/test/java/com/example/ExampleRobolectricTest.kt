package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BillItem
import com.example.data.local.WarrantyStatus
import com.example.data.util.SampleBillsProvider
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.getStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BillVault", appName)
    }

    @Test
    fun `sample bills provider returns items for email`() {
        val email = "taybzmota15@gmail.com"
        val bills = SampleBillsProvider.getSampleBills(email)
        assertTrue(bills.isNotEmpty())
        assertEquals(email, bills.first().userEmail)
    }

    @Test
    fun `warranty status calculation test`() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Future expiry (1 year from now)
        cal.time = Date()
        cal.add(Calendar.YEAR, 1)
        val activeBill = BillItem(
            userEmail = "test@example.com",
            productName = "Active Product",
            purchaseDate = "2024-01-01",
            expiryDate = sdf.format(cal.time)
        )
        assertEquals(WarrantyStatus.ACTIVE, activeBill.getWarrantyStatus())
        assertTrue(activeBill.getDaysRemaining() > 300)

        // Past expiry (1 year ago)
        cal.time = Date()
        cal.add(Calendar.YEAR, -1)
        val expiredBill = BillItem(
            userEmail = "test@example.com",
            productName = "Expired Product",
            purchaseDate = "2022-01-01",
            expiryDate = sdf.format(cal.time)
        )
        assertEquals(WarrantyStatus.EXPIRED, expiredBill.getWarrantyStatus())
        assertTrue(expiredBill.getDaysRemaining() < 0)
    }

    @Test
    fun `multi language strings test`() {
        val en = getStrings(AppLanguage.ENGLISH)
        val gu = getStrings(AppLanguage.GUJARATI)
        val hi = getStrings(AppLanguage.HINDI)

        assertEquals("BillVault", en.appName)
        assertEquals("BillVault", gu.appName)
        assertEquals("BillVault", hi.appName)

        assertTrue(gu.scanBillFab.contains("સ્કેન"))
        assertTrue(hi.scanBillFab.contains("स्कैन"))
        assertTrue(en.scanBillFab.contains("Scan"))

        assertEquals("ગુજરાતી", AppLanguage.GUJARATI.nativeName)
        assertEquals("हिन्दी", AppLanguage.HINDI.nativeName)
        assertEquals("English", AppLanguage.ENGLISH.nativeName)

        assertEquals("Home", en.navHome)
        assertEquals("Settings", en.navSettings)
        assertEquals("હોમ", gu.navHome)
        assertEquals("સેટિંગ્સ", gu.navSettings)
        assertEquals("होम", hi.navHome)
        assertEquals("सेटिंग्स", hi.navSettings)
    }
}

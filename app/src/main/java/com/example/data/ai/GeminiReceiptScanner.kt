package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ScannedBillResult(
    val productName: String,
    val brand: String = "",
    val storeOrSeller: String = "",
    val category: String = "Electronics",
    val purchaseDate: String = "", // YYYY-MM-DD
    val warrantyPeriodMonths: Int = 12,
    val expiryDate: String = "", // YYYY-MM-DD
    val price: Double = 0.0,
    val currency: String = "₹",
    val invoiceNumber: String = "",
    val serialNumber: String = "",
    val warrantyType: String = "Manufacturer Warranty",
    val coverageDetails: String = "",
    val supportContact: String = "",
    val notes: String = ""
)

sealed class ScanResult {
    data class Success(val data: ScannedBillResult) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

object GeminiReceiptScanner {
    private const val TAG = "GeminiReceiptScanner"
    private const val MODEL = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun scanReceipt(bitmap: Bitmap): ScanResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ScanResult.Error(
                "Gemini API key is not configured. Please set GEMINI_API_KEY in the Secrets panel or .env."
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)

            val prompt = """
                You are an expert OCR system specializing in invoices, cash memos, tax bills, and warranty/guarantee cards from all sources (e-commerce like Amazon/Flipkart, retail stores like Reliance Digital/Croma/Vijay Sales, local hardware/electronics shops, brand service centers, vehicle & battery dealers).

                Carefully inspect this document/photo/receipt. It may be in English, Hindi, Gujarati, or mixed. Every bill has a different format, layout, and terminology.

                Extract all relevant data and return STRICT JSON with these exact fields:
                - "productName": string (primary purchased item, model, or product description)
                - "brand": string (brand, company, or manufacturer name, e.g. Samsung, Apple, Sony, LG, Whirlpool, Bosch, Dell, HP, Havells, Philips, Exide)
                - "storeOrSeller": string (merchant, seller, retail showroom, or platform name from header, logo, or GST details, e.g. Reliance Digital, Amazon, Vijay Sales, Croma, Apple Store)
                - "category": string (classify into one of: "Electronics", "Home Appliances", "Mobile & Gadgets", "Computers & Laptops", "Vehicles & Auto", "Furniture & Home", "Fashion & Watch", "Other")
                - "purchaseDate": string in format "YYYY-MM-DD" (the bill date, invoice date, date of purchase, or date of sale. Convert any date format like 15/10/2024 or 15-Oct-2024 to standard YYYY-MM-DD)
                - "warrantyPeriodMonths": integer (warranty or guarantee duration in months. e.g. "1 year" -> 12, "2 years" -> 24, "36 months" -> 36, "5 years" -> 60. If unspecified or not mentioned, default to 12)
                - "expiryDate": string in format "YYYY-MM-DD" (if explicit "Valid Till" or "Expiry Date" is stated on bill/card, use that in YYYY-MM-DD. Otherwise, calculate as purchaseDate + warrantyPeriodMonths)
                - "price": number (grand total, invoice amount paid, or net payable value. Strip currency symbols and return numeric float/double)
                - "currency": string (currency symbol or code, e.g. "₹", "$", "INR")
                - "invoiceNumber": string (tax invoice number, receipt number, bill number, order ID, or cash memo number)
                - "serialNumber": string (serial number, IMEI, MAC address, chassis/battery number, or service tag if present)
                - "warrantyType": string (e.g. "Standard Manufacturer Warranty", "1 Year Comprehensive + 10 Years Compressor", "Extended Guarantee", "Onsite Warranty")
                - "coverageDetails": string (brief summary of what is covered, e.g. "Complete unit, labor and spare parts", "Panel coverage", "Motor warranty")
                - "supportContact": string (customer care phone number, toll-free number, helpline or email printed on the bill/card)
                - "notes": string (any terms, registration instructions, or proof-of-purchase remarks)

                Return ONLY raw valid JSON. Do NOT wrap in markdown fences or include conversational text.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply {
                        put("text", prompt)
                    })
                    partsArray.put(JSONObject().apply {
                        val inlineDataObj = JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        }
                        put("inlineData", inlineDataObj)
                    })
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                }
                put("generationConfig", generationConfig)
            }

            val requestUrl = "$BASE_URL$MODEL:generateContent?key=$apiKey"
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code: ${response.code}, body: $responseBody")
                return@withContext ScanResult.Error("API error (${response.code}): ${parseErrorMessage(responseBody)}")
            }

            val parsedJsonString = extractJsonFromResponse(responseBody)
            val result = parseBillJson(parsedJsonString)
            ScanResult.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Receipt scan failed", e)
            ScanResult.Error(e.localizedMessage ?: "Failed to scan receipt image")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1600
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (targetWidth, targetHeight) = if (ratio > 1) {
                maxDimension to (maxDimension / ratio).toInt()
            } else {
                (maxDimension * ratio).toInt() to maxDimension
            }
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun extractJsonFromResponse(responseBody: String): String {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return "{}"
        if (candidates.length() == 0) return "{}"
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return "{}"
        val parts = content.optJSONArray("parts") ?: return "{}"
        if (parts.length() == 0) return "{}"
        val rawText = parts.getJSONObject(0).optString("text", "{}")

        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun parseBillJson(jsonString: String): ScannedBillResult {
        return try {
            val json = JSONObject(jsonString)
            val productName = json.optString("productName").ifBlank { "Purchased Product" }
            val brand = json.optString("brand")
            val store = json.optString("storeOrSeller")
            val category = json.optString("category").ifBlank { "Electronics" }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            val rawPurchaseDate = json.optString("purchaseDate")
            val purchaseDate = normalizeDate(rawPurchaseDate).ifBlank { todayStr }
            val warrantyMonths = json.optInt("warrantyPeriodMonths", 12).coerceAtLeast(1)

            val rawExpiryDate = json.optString("expiryDate")
            var expiryDate = normalizeDate(rawExpiryDate)
            if (expiryDate.isBlank()) {
                try {
                    val pDate = sdf.parse(purchaseDate) ?: Date()
                    val cal = Calendar.getInstance().apply {
                        time = pDate
                        add(Calendar.MONTH, warrantyMonths)
                    }
                    expiryDate = sdf.format(cal.time)
                } catch (e: Exception) {
                    val cal = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }
                    expiryDate = sdf.format(cal.time)
                }
            }

            val price = json.optDouble("price", 0.0)
            val currency = json.optString("currency").ifBlank { "₹" }
            val invoiceNumber = json.optString("invoiceNumber")
            val serialNumber = json.optString("serialNumber")
            val warrantyType = json.optString("warrantyType").ifBlank { "Manufacturer Warranty" }
            val coverageDetails = json.optString("coverageDetails")
            val supportContact = json.optString("supportContact")
            val notes = json.optString("notes")

            ScannedBillResult(
                productName = productName,
                brand = brand,
                storeOrSeller = store,
                category = category,
                purchaseDate = purchaseDate,
                warrantyPeriodMonths = warrantyMonths,
                expiryDate = expiryDate,
                price = price,
                currency = currency,
                invoiceNumber = invoiceNumber,
                serialNumber = serialNumber,
                warrantyType = warrantyType,
                coverageDetails = coverageDetails,
                supportContact = supportContact,
                notes = notes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing bill JSON: $jsonString", e)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val nextYear = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time
            val expiry = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(nextYear)
            ScannedBillResult(
                productName = "Product / Bill",
                purchaseDate = today,
                warrantyPeriodMonths = 12,
                expiryDate = expiry
            )
        }
    }

    /**
     * Normalizes dates from various formats (DD/MM/YYYY, DD-MM-YYYY, YYYY/MM/DD, DD-MMM-YYYY)
     * to strict standard "YYYY-MM-DD".
     */
    fun normalizeDate(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""

        val formats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy/MM/dd",
            "d/M/yyyy",
            "dd.MM.yyyy",
            "dd MMM yyyy",
            "MM/dd/yyyy"
        )

        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }
                val parsed = parser.parse(trimmed)
                if (parsed != null) {
                    val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    return targetFormat.format(parsed)
                }
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return if (trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) trimmed else ""
    }

    private fun parseErrorMessage(errorBody: String): String {
        return try {
            val obj = JSONObject(errorBody)
            val error = obj.optJSONObject("error")
            error?.optString("message") ?: errorBody
        } catch (e: Exception) {
            errorBody
        }
    }
}

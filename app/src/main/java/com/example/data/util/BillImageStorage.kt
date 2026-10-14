package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BillImageStorage {
    private const val TAG = "BillImageStorage"

    fun getReceiptsDir(context: Context): File {
        val dir = File(context.filesDir, "receipts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createTempImageUri(context: Context): Uri {
        val receiptsDir = getReceiptsDir(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(receiptsDir, "camera_$timeStamp.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap): String {
        val receiptsDir = getReceiptsDir(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(receiptsDir, "receipt_$timeStamp.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    fun saveUriToFile(context: Context, uri: Uri): String {
        val receiptsDir = getReceiptsDir(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(receiptsDir, "receipt_$timeStamp.jpg")
        context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    /**
     * Intelligently processes either an image or a PDF invoice URI.
     * If PDF: renders the first page using native Android PdfRenderer into a high-res Bitmap.
     * If Image: loads and scales bitmap.
     * Returns a Pair of the Bitmap (for Gemini AI analysis) and local file path (for storage/preview).
     */
    fun processUri(context: Context, uri: Uri): Pair<Bitmap, String>? {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val uriStr = uri.toString().lowercase()
        val isPdf = mimeType.contains("pdf", ignoreCase = true) || uriStr.endsWith(".pdf")

        if (isPdf) {
            return renderPdfPage(context, uri)
        }

        val bitmap = loadBitmapFromUri(context, uri) ?: return null
        val path = saveBitmapToFile(context, bitmap)
        return Pair(bitmap, path)
    }

    private fun renderPdfPage(context: Context, uri: Uri): Pair<Bitmap, String>? {
        return try {
            val receiptsDir = getReceiptsDir(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val tempPdfFile = File(receiptsDir, "temp_doc_$timeStamp.pdf")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempPdfFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val pfd = ParcelFileDescriptor.open(tempPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) {
                renderer.close()
                pfd.close()
                tempPdfFile.delete()
                return null
            }

            val page = renderer.openPage(0)
            // Render at high resolution (approx 1600-2400px height for optimal OCR reading of fine invoice text)
            val targetWidth = (page.width * 2.2f).toInt().coerceIn(1000, 2400)
            val targetHeight = (page.height * 2.2f).toInt().coerceIn(1200, 3200)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // PDF background must be solid white for OCR
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            pfd.close()
            tempPdfFile.delete()

            val savedPath = saveBitmapToFile(context, bitmap)
            Pair(bitmap, savedPath)
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF bill page", e)
            null
        }
    }

    fun loadBitmapFromPath(path: String, maxDimension: Int = 1600): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            var scale = 1
            while (options.outWidth / scale > maxDimension || options.outHeight / scale > maxDimension) {
                scale *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }

    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input.close()

            var scale = 1
            while (options.outWidth / scale > maxDimension || options.outHeight / scale > maxDimension) {
                scale *= 2
            }

            val secondInput = context.contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val bmp = BitmapFactory.decodeStream(secondInput, null, decodeOptions)
            secondInput.close()
            bmp
        } catch (e: Exception) {
            null
        }
    }
}

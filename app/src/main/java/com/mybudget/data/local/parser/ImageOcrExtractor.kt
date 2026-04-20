package com.mybudget.data.local.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ImageOcrExtractor(private val context: Context) {

    /**
     * Renders a PDF deeply mapped as an image directly to Bitmap arrays,
     * and processes them iteratively via Google ML Kit On-Device Text Recognition.
     */
    suspend fun extractTextFromScannedPdf(uri: Uri): String {
        val stringBuilder = java.lang.StringBuilder()
        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null

        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                val pageCount = pdfRenderer.pageCount
                for (i in 0 until pageCount) {
                    val page = pdfRenderer.openPage(i)
                    // Render page into a bitmap
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2, // Scale up for better OCR reading bounds
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Execute ML Kit analysis
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val resultText = try {
                        suspendCoroutine<String> { continuation ->
                            recognizer.process(image)
                                .addOnSuccessListener { result ->
                                    continuation.resume(result.text)
                                }
                                .addOnFailureListener { e ->
                                    continuation.resumeWithException(e)
                                }
                        }
                    } finally {
                        bitmap.recycle()
                    }
                    stringBuilder.append(resultText).append("\n")
                }
            }
        } catch (e: Exception) {
            return "ERROR_OCR: An error occurred during image processing."
        } finally {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }

        return stringBuilder.toString()
    }
}

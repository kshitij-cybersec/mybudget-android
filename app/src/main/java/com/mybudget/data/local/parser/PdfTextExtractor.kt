package com.mybudget.data.local.parser

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

class PdfTextExtractor(private val context: Context) {

    init {
        // Initialize PDFBox once
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Extracts purely text-layered content from a PDF document URI.
     */
    fun extractTextFromUri(uri: Uri): String {
        var extractedText = ""
        var document: PDDocument? = null
        var inputStream: InputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                document = PDDocument.load(inputStream)
                val pdfStripper = PDFTextStripper()
                pdfStripper.sortByPosition = true
                extractedText = pdfStripper.getText(document)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            extractedText = "ERROR_PARSING: ${e.message}"
        } finally {
            document?.close()
            inputStream?.close()
        }

        return extractedText
    }
}

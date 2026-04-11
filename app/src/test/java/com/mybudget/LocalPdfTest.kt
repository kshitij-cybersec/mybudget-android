package com.mybudget

import org.junit.Test
import java.io.File
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class LocalPdfTest {
    @Test
    fun testPdf() {
        val file = File("C:/Users/kkk/Downloads/downloadfile.PDF")
        if (!file.exists()) {
            println("File does not exist")
            return
        }
        try {
            val document = PDDocument.load(file)
            val pdfStripper = PDFTextStripper()
            val extractedText = pdfStripper.getText(document)
            println("EXTRACTED_TEXT:\n\n" + extractedText.take(1000))
            document.close()
        } catch (e: Exception) {
            println("EXCEPTION: ${e.javaClass.name} - ${e.message}")
        }
    }
}

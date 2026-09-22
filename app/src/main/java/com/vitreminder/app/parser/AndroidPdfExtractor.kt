package com.vitreminder.app.parser

import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.ByteArrayInputStream
import java.io.InputStream

class AndroidPdfExtractor : PdfExtractor {
    override fun extractPositions(inputStream: InputStream): List<PdfTextPosition> {
        val result = mutableListOf<PdfTextPosition>()
        val bytes = inputStream.readBytes()
        Log.d("AndroidPdfExtractor", "Read ${bytes.size} bytes from PDF stream")

        if (bytes.isEmpty()) {
            throw IllegalArgumentException("PDF file is empty (0 bytes).")
        }

        val doc = PDDocument.load(ByteArrayInputStream(bytes))
        try {
            val stripper = object : PDFTextStripper() {
                init {
                    sortByPosition = true
                }

                private var currentPage = 1

                override fun processPage(page: PDPage) {
                    currentPage = getCurrentPageNo()
                    super.processPage(page)
                }

                override fun writeString(
                    text: String,
                    textPositions: MutableList<TextPosition>
                ) {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty() && textPositions.isNotEmpty()) {
                        val first = textPositions[0]
                        result.add(
                            PdfTextPosition(
                                text = trimmed,
                                x = first.xDirAdj,
                                y = first.yDirAdj,
                                page = currentPage
                            )
                        )
                    }
                    super.writeString(text, textPositions)
                }
            }
            stripper.getText(doc)
        } catch (e: Throwable) {
            Log.e("AndroidPdfExtractor", "Error extracting text from PDF", e)
            throw e
        } finally {
            try {
                doc.close()
            } catch (e: Exception) {
                // Ignore close exception
            }
        }
        Log.d("AndroidPdfExtractor", "Extracted ${result.size} text tokens from PDF")
        return result
    }
}

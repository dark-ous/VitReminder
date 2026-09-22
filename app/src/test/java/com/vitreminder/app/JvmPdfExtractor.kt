package com.vitreminder.app

import com.vitreminder.app.parser.PdfExtractor
import com.vitreminder.app.parser.PdfTextPosition
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.InputStream

class JvmPdfExtractor : PdfExtractor {
    override fun extractPositions(inputStream: InputStream): List<PdfTextPosition> {
        val result = mutableListOf<PdfTextPosition>()
        val doc = PDDocument.load(inputStream)
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
        } finally {
            doc.close()
        }
        return result
    }
}

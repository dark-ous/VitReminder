package com.vitreminder.app.parser

import java.io.InputStream

data class PdfTextPosition(
    val text: String,
    val x: Float,
    val y: Float,
    val page: Int
)

interface PdfExtractor {
    fun extractPositions(inputStream: InputStream): List<PdfTextPosition>
}

object PdfExtractorFactory {
    private var customExtractor: PdfExtractor? = null

    fun setExtractor(extractor: PdfExtractor) {
        customExtractor = extractor
    }

    fun create(): PdfExtractor {
        customExtractor?.let { return it }

        return try {
            val clazz = Class.forName("com.vitreminder.app.parser.AndroidPdfExtractor")
            clazz.getDeclaredConstructor().newInstance() as PdfExtractor
        } catch (e: Throwable) {
            try {
                val clazz = Class.forName("com.vitreminder.app.JvmPdfExtractor")
                clazz.getDeclaredConstructor().newInstance() as PdfExtractor
            } catch (e2: Throwable) {
                throw IllegalStateException("No suitable PdfExtractor found on classpath", e2)
            }
        }
    }
}

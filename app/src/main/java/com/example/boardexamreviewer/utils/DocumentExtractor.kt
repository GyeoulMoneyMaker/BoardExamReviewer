package com.example.boardexamreviewer.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import java.io.InputStream

/**
 * Utility class to extract text from documents (PDF, DOCX, etc.)
 */
object DocumentExtractor {

    /**
     * Extracts text from the given Uri based on its extension.
     */
    fun extractText(context: Context, uri: Uri): String {
        val fileName = getFileName(context, uri)
        val extension = fileName.substringAfterLast(".", "").lowercase()

        return try {
            when (extension) {
                "pdf" -> extractFromPdf(context, uri)
                "docx" -> extractFromDocx(context, uri)
                "pptx" -> extractFromPptx(context, uri)
                "txt" -> extractFromTxt(context, uri)
                else -> "Unsupported file type: $extension"
            }
        } catch (e: Exception) {
            "Error extracting text: ${e.message}"
        }
    }

    /**
     * Helper to get filename from Uri
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown_file"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    private fun extractFromPdf(context: Context, uri: Uri): String {
        // Initialize PDFBox
        PDFBoxResourceLoader.init(context)

        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return inputStream?.use {
            val document = PDDocument.load(it)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()

            val trimmedText = text?.trim() ?: ""
            if (trimmedText.isEmpty()) "This document appears to be empty or an image-based PDF (OCR not supported)." else trimmedText
        } ?: "Could not open PDF file"
    }

    /**
     * Extracts text from a DOCX file using Apache POI
     */
    private fun extractFromDocx(context: Context, uri: Uri): String {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return inputStream?.use {
            try {
                val doc = org.apache.poi.xwpf.usermodel.XWPFDocument(it)
                val extractor = org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc)
                val text = extractor.text
                extractor.close()
                text.trim()
            } catch (e: Exception) {
                "Error reading DOCX: ${e.message}"
            }
        } ?: "Could not open DOCX file"
    }

    /**
     * Extracts text from PowerPoint (.pptx) files
     */
    private fun extractFromPptx(context: Context, uri: Uri): String {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return inputStream?.use {
            try {
                val ppt = XMLSlideShow(it)
                val sb = StringBuilder()
                for (slide in ppt.slides) {
                    for (shape in slide.shapes) {
                        if (shape is XSLFTextShape) {
                            sb.append(shape.text).append("\n")
                        }
                    }
                }
                sb.toString().trim()
            } catch (e: Exception) {
                "Error reading PPTX: ${e.message}"
            }
        } ?: "Could not open PPTX file"
    }

    /**
     * Extracts text from plain text (.txt) files
     */
    private fun extractFromTxt(context: Context, uri: Uri): String {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return inputStream?.use {
            try {
                it.bufferedReader().use { reader -> reader.readText().trim() }
            } catch (e: Exception) {
                "Error reading notes: ${e.message}"
            }
        } ?: "Could not open notes file"
    }
}


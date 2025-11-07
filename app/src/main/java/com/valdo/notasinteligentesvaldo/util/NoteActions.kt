package com.valdo.notasinteligentesvaldo.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.valdo.notasinteligentesvaldo.R
import java.io.File

/**
 * Utilidades reutilizables para acciones sobre notas (copiar, compartir, PDF).
 */
object NoteActions {
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val label = context.getString(R.string.app_name)
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Contenido copiado", Toast.LENGTH_SHORT).show()
    }

    fun shareNoteContent(context: Context, title: String, content: String) {
        val body = if (title.isNotBlank()) "$title\n\n$content" else content
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { context.getString(R.string.app_name) })
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Compartir nota"))
    }

    fun shareNoteFile(context: Context, title: String, content: String, asMarkdown: Boolean) {
        val safeTitle = title.ifBlank { "nota" }
        val ext = if (asMarkdown) ".md" else ".txt"
        val fileName = safeTitle.lowercase().replace("[^a-z0-9._-]".toRegex(), "_").take(40).ifBlank { "nota" } + ext
        val outFile = File(context.cacheDir, fileName)
        val body = if (asMarkdown && title.isNotBlank()) "# $title\n\n$content" else (if (title.isNotBlank()) "$title\n\n$content" else content)
        outFile.writeText(body, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", outFile)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (asMarkdown) "text/markdown" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, safeTitle)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Compartir archivo"))
    }

    fun shareNoteAsPdf(context: Context, title: String, content: String) {
        try {
            val file = createPdfFile(context, title, content)
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, if (title.isNotBlank()) title else "Nota")
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Compartir PDF"))
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo crear el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createPdfFile(context: Context, title: String, content: String): File {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40
        val contentWidth = pageWidth - margin * 2
        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 12f
        }
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = margin + titlePaint.textSize
        if (title.isNotBlank()) {
            canvas.drawText(title, margin.toFloat(), y, titlePaint)
            y += titlePaint.textSize + 12f
        }
        val lines = content.split('\n')
        lines.forEach { line ->
            val wrapped = wrapText(line, bodyPaint, contentWidth)
            wrapped.forEach { wLine ->
                if (y + bodyPaint.textSize + margin > pageHeight) return@forEach
                canvas.drawText(wLine, margin.toFloat(), y, bodyPaint)
                y += bodyPaint.textSize + 6f
            }
        }
        document.finishPage(page)
        val file = File(context.cacheDir, (title.ifBlank { "nota" } + "_" + System.currentTimeMillis() + ".pdf").replace("[^a-zA-Z0-9._-]".toRegex(), "_"))
        document.writeTo(file.outputStream())
        document.close()
        return file
    }

    private fun wrapText(text: String, paint: TextPaint, maxWidth: Int): List<String> {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val test = if (current.isEmpty()) word else current.toString() + " " + word
            if (paint.measureText(test) > maxWidth) {
                if (current.isNotEmpty()) {
                    lines.add(current.toString())
                    current = StringBuilder(word)
                } else {
                    lines.add(word)
                    current = StringBuilder()
                }
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}

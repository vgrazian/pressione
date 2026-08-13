package com.pressione.iperteso.services

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.pressione.iperteso.domain.model.Category
import com.pressione.iperteso.domain.model.Medication
import com.pressione.iperteso.domain.model.Reading
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Medical-grade PDF report generator using iText7.
 * Produces an A4 document with patient info, BP stats, readings table,
 * ESC/ESH classification breakdown, and medication timeline.
 */
object PdfReportGenerator {

    private val medicalGreen = DeviceRgb(0, 108, 76) // #006C4C
    private val textDark = DeviceRgb(51, 51, 51)
    private val textMuted = DeviceRgb(136, 136, 136)
    private val errorRed = DeviceRgb(186, 26, 26)
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun generate(
        context: Context,
        username: String,
        readings: List<Reading>,
        medications: List<Medication>,
        anonimizza: Boolean = false
    ): File {
        val file = File(context.cacheDir, "report_iperteso_${username}.pdf")
        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val doc = Document(pdf, com.itextpdf.kernel.geom.PageSize.A4)
        doc.setMargins(36f, 36f, 36f, 36f)

        // Header
        doc.add(
            Paragraph("IperTeso — Report Pressione Arteriosa")
                .setFontSize(18f)
                .setBold()
                .setFontColor(medicalGreen)
        )

        if (!anonimizza) {
            doc.add(Paragraph("Paziente: $username").setFontSize(11f).setFontColor(textDark))
        }

        doc.add(
            Paragraph("Generato il ${dateTimeFormat.format(java.time.LocalDateTime.now())}")
                .setFontSize(9f).setFontColor(textMuted)
        )
        doc.add(LineSeparator(SolidLine(0.5f)).setMarginTop(8f).setMarginBottom(8f))

        // Stats
        if (readings.isNotEmpty()) {
            val sys = readings.map { it.systolic }
            val dia = readings.map { it.diastolic }
            val hr = readings.map { it.heartRate }
            val avg = { l: List<Int> -> l.average() }

            doc.add(Paragraph("Statistiche").setFontSize(13f).setBold().setFontColor(medicalGreen))
            doc.add(Paragraph(
                "Media SYS: %.0f mmHg  |  Media DIA: %.0f mmHg  |  Media FC: %.0f BPM  |  Totale: %d misurazioni".format(
                    avg(sys), avg(dia), avg(hr), readings.size
                )
            ).setFontSize(10f))
            doc.add(Paragraph(
                "Min SYS: %d  |  Max SYS: %d  |  Min FC: %d  |  Max FC: %d".format(
                    sys.minOrNull() ?: 0, sys.maxOrNull() ?: 0,
                    hr.minOrNull() ?: 0, hr.maxOrNull() ?: 0
                )
            ).setFontSize(10f))

            // Hypertensive load
            val hypertensiveCount = readings.count { it.systolic > 140 || it.diastolic > 90 }
            val load = if (readings.isNotEmpty()) hypertensiveCount.toFloat() / readings.size * 100 else 0f
            doc.add(Paragraph("Carico Ipertensivo: %.0f%%".format(load)).setFontSize(10f))
            doc.add(LineSeparator(SolidLine(0.3f)).setMarginTop(4f).setMarginBottom(4f))
        }

        // ESC/ESH Classification
        if (readings.isNotEmpty()) {
            doc.add(Paragraph("Classificazione ESC/ESH").setFontSize(13f).setBold().setFontColor(medicalGreen))
            val distribution = readings.groupBy { it.category }.mapValues { it.value.size }
            val table = Table(UnitValue.createPercentArray(floatArrayOf(60f, 20f, 20f)))
            table.setWidth(UnitValue.createPercentValue(100f))
            table.addHeaderCell(Cell().add(Paragraph("Categoria").setBold().setFontSize(9f)))
            table.addHeaderCell(Cell().add(Paragraph("N.").setBold().setFontSize(9f)))
            table.addHeaderCell(Cell().add(Paragraph("%").setBold().setFontSize(9f)))

            for (cat in Category.entries) {
                val count = distribution[cat] ?: 0
                if (count > 0) {
                    val pct = count.toFloat() / readings.size * 100
                    table.addCell(Cell().add(Paragraph(cat.label).setFontSize(9f)))
                    table.addCell(Cell().add(Paragraph("$count").setFontSize(9f)))
                    table.addCell(Cell().add(Paragraph("%.0f%%".format(pct)).setFontSize(9f)))
                }
            }
            doc.add(table)
            doc.add(LineSeparator(SolidLine(0.3f)).setMarginTop(4f).setMarginBottom(4f))
        }

        // Medications
        if (medications.isNotEmpty()) {
            doc.add(Paragraph("Farmaci").setFontSize(13f).setBold().setFontColor(medicalGreen))
            for (med in medications) {
                val status = if (med.isActive) "In corso" else "Interrotto"
                val dates = "${dateFormat.format(med.startDate.atZone(ZoneId.systemDefault()))} — " +
                    if (med.isActive) "in corso"
                    else dateFormat.format(med.endDate!!.atZone(ZoneId.systemDefault()))
                val line = buildString {
                    append(med.name)
                    if (med.activeIngredient.isNotBlank()) append(" (${med.activeIngredient})")
                    if (med.dosage.isNotBlank()) append(" ${med.dosage}")
                    if (med.frequency.isNotBlank()) append(" ${med.frequency}")
                    append(" — $status ($dates)")
                }
                doc.add(Paragraph(line).setFontSize(10f))
            }
            doc.add(LineSeparator(SolidLine(0.3f)).setMarginTop(4f).setMarginBottom(4f))
        }

        // Readings table (last 30)
        doc.add(Paragraph("Ultime misurazioni").setFontSize(13f).setBold().setFontColor(medicalGreen))
        val readingsTable = Table(UnitValue.createPercentArray(floatArrayOf(25f, 20f, 20f, 15f, 20f)))
        readingsTable.setWidth(UnitValue.createPercentValue(100f))
        readingsTable.addHeaderCell(Cell().add(Paragraph("Data/Ora").setBold().setFontSize(8f)))
        readingsTable.addHeaderCell(Cell().add(Paragraph("SYS/DIA").setBold().setFontSize(8f)))
        readingsTable.addHeaderCell(Cell().add(Paragraph("FC").setBold().setFontSize(8f)))
        readingsTable.addHeaderCell(Cell().add(Paragraph("Categoria").setBold().setFontSize(8f)))
        readingsTable.addHeaderCell(Cell().add(Paragraph("Note").setBold().setFontSize(8f)))

        for (r in readings.take(30)) {
            val ts = dateTimeFormat.format(r.timestamp.atZone(ZoneId.systemDefault()))
            readingsTable.addCell(Cell().add(Paragraph(ts).setFontSize(8f)))
            val bpCell = Cell().add(Paragraph("${r.systolic}/${r.diastolic}").setFontSize(8f))
            if (r.category == Category.CRISIS || r.category == Category.GRADE_3) {
                bpCell.setFontColor(errorRed)
            }
            readingsTable.addCell(bpCell)
            readingsTable.addCell(Cell().add(Paragraph("${r.heartRate}").setFontSize(8f)))
            readingsTable.addCell(Cell().add(Paragraph(r.category.label).setFontSize(7f)))
            readingsTable.addCell(Cell().add(Paragraph(r.notes).setFontSize(8f)))
        }
        doc.add(readingsTable)

        // Disclaimer
        doc.add(LineSeparator(SolidLine(0.5f)).setMarginTop(12f).setMarginBottom(6f))
        doc.add(
            Paragraph("⚠️ Questo report è generato automaticamente e non sostituisce il parere medico.")
                .setFontSize(8f).setFontColor(textMuted).setTextAlignment(TextAlignment.CENTER)
        )

        doc.close()
        return file
    }

    fun sharePdf(context: Context, file: File, subject: String = "Report IperTeso") {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Condividi report"))
    }

    fun sharePdfViaEmail(context: Context, file: File, subject: String, body: String) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Invia report via email"))
    }

    fun sharePdfViaWhatsApp(context: Context, file: File, subject: String) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            `package` = "com.whatsapp"
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(Intent.createChooser(intent.apply { `package` = null }, "Invia report via WhatsApp"))
        }
    }
}

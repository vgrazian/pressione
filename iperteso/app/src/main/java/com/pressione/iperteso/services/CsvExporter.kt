package com.pressione.iperteso.services

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pressione.iperteso.domain.model.Reading
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {

    private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun export(context: Context, username: String, readings: List<Reading>): File {
        val file = File(context.cacheDir, "iperteso_${username}.csv")
        file.bufferedWriter().use { writer ->
            writer.write("Data,Ora,Sistolica (mmHg),Diastolica (mmHg),Freq. Cardiaca (BPM),Categoria ESC/ESH,Note")
            writer.newLine()
            for (r in readings.sortedByDescending { it.timestamp.toEpochMilli() }) {
                val dt = dateTimeFormat.format(r.timestamp).split(" ")
                val data = dt[0]
                val ora = dt[1]
                writer.write("$data,$ora,${r.systolic},${r.diastolic},${r.heartRate},${r.category.label},\"${r.notes.replace("\"", "\"\"")}\"")
                writer.newLine()
            }
        }
        return file
    }

    fun shareCsv(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Esportazione IperTeso")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Esporta CSV"))
    }
}

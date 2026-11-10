package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.database.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FinanceExportHelper {

    private fun escapeCsv(text: String): String {
        val sanitized = text.replace("\n", " ").replace("\r", " ")
        return if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains(";")) {
            "\"" + sanitized.replace("\"", "\"\"") + "\""
        } else {
            sanitized
        }
    }

    fun exportToGoogleSheetsCsv(
        context: Context,
        transactions: List<Transaction>,
        monthCalendar: Calendar
    ) {
        try {
            if (transactions.isEmpty()) {
                Toast.makeText(context, "No hay transacciones para exportar.", Toast.LENGTH_SHORT).show()
                return
            }

            val monthFormat = SimpleDateFormat("MMMM_yyyy", Locale("es", "ES"))
            val monthLabel = monthFormat.format(monthCalendar.time).replace(" ", "_")
            val fileName = "Gastos_Hogar_$monthLabel.csv"

            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val file = File(exportDir, fileName)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

            FileOutputStream(file).use { fos ->
                // Write UTF-8 BOM for complete Excel & Google Sheets character compatibility
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // CSV Header
                    writer.write(
                        listOf(
                            "Fecha",
                            "Hora",
                            "Mes Contable",
                            "Tipo",
                            "Descripción / Comercio",
                            "Categoría",
                            "Monto Original",
                            "Moneda",
                            "Tasa BCV (Bs/$)",
                            "Monto en USD ($)",
                            "Recurrente",
                            "Registrado Por",
                            "Desglose de Ítems / Productos"
                        ).joinToString(",") + "\n"
                    )

                    // Write sorted transactions
                    val sortedTx = transactions.sortedBy { if (it.balanceDate > 0) it.balanceDate else it.date }
                    for (tx in sortedTx) {
                        val txDate = Date(if (tx.balanceDate > 0) tx.balanceDate else tx.date)
                        val dateStr = dateFormat.format(txDate)
                        val timeStr = timeFormat.format(txDate)
                        val monthStr = monthNameFormat.format(txDate).replaceFirstChar { it.uppercase() }
                        val typeStr = if (tx.isIncome) "Ingreso" else "Gasto"
                        val descStr = escapeCsv(tx.description)
                        val catStr = escapeCsv(tx.category)
                        val originalAmountStr = String.format(Locale.US, "%.2f", tx.originalAmount)
                        val currencyStr = tx.currency
                        val rateStr = String.format(Locale.US, "%.2f", tx.bcvRateUsed)
                        val usdAmountStr = String.format(Locale.US, "%.2f", tx.amount)
                        val recStr = if (tx.isRecurring) "Sí" else "No"
                        val userStr = escapeCsv(tx.registeredBy)

                        // Format items breakdown
                        val itemsDetail = if (tx.items.isNotEmpty()) {
                            tx.items.joinToString(" | ") { item ->
                                "${item.name} [${item.category}]: ${String.format(Locale.US, "%.2f", item.amount)} $currencyStr"
                            }
                        } else {
                            "Sin desglose"
                        }
                        val itemsDetailEscaped = escapeCsv(itemsDetail)

                        writer.write(
                            listOf(
                                dateStr,
                                timeStr,
                                monthStr,
                                typeStr,
                                descStr,
                                catStr,
                                originalAmountStr,
                                currencyStr,
                                rateStr,
                                usdAmountStr,
                                recStr,
                                userStr,
                                itemsDetailEscaped
                            ).joinToString(",") + "\n"
                        )
                    }
                    writer.flush()
                }
            }

            // Generate content URI with FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            // Open with Google Sheets / Excel / Share Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Exportación de Finanzas - $fileName")
                putExtra(Intent.EXTRA_TEXT, "Adjunto reporte detallado de gastos y finanzas en formato CSV compatible con Hojas de cálculo de Google y Excel.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Abrir en Hoja de Cálculo o Guardar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            Toast.makeText(context, "Error al exportar reporte: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

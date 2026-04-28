package com.mybudget.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.mybudget.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    suspend fun exportAsCsv(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream)
                writer.write("Date,Amount,Type,Category,Currency,Description\n")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                
                transactions.forEach { tx ->
                    val date = dateFormat.format(Date(tx.timestamp))
                    val type = if (tx.isIncome) "Income" else "Expense"
                    val tag = tx.tag.replace(",", " ")
                    val desc = tx.description.replace(",", " ")
                    writer.write("$date,${tx.amount},$type,$tag,${tx.currency},$desc\n")
                }
                writer.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportAsPdf(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>,
        chartType: String,
        timelineString: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Draw Title
            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("Transaction Report", 50f, 50f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Timeline: $timelineString", 50f, 80f, paint)

            val expenses = transactions.filter { !it.isIncome }
            val expensesByCategory = expenses.groupBy { it.tag }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            var currentY = 120f

            if (expensesByCategory.isNotEmpty() && chartType != "None") {
                if (chartType == "Pie Chart") {
                    drawPieChart(canvas, expensesByCategory, 50f, currentY)
                } else if (chartType == "Bar Chart") {
                    drawBarChart(canvas, expensesByCategory, 50f, currentY)
                }
                currentY += 250f
            }

            // Draw Transactions Table Header
            paint.textSize = 14f
            paint.isFakeBoldText = true
            paint.color = Color.BLACK
            canvas.drawText("Date", 50f, currentY, paint)
            canvas.drawText("Category", 180f, currentY, paint)
            canvas.drawText("Type", 300f, currentY, paint)
            canvas.drawText("Amount", 400f, currentY, paint)
            
            currentY += 20f
            paint.strokeWidth = 1f
            canvas.drawLine(50f, currentY, 545f, currentY, paint)
            currentY += 20f

            // Draw Transactions
            paint.isFakeBoldText = false
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            for (tx in transactions) {
                if (currentY > 800f) {
                    // Start new page if needed (simplified for this exercise, just drawing one page)
                    break
                }
                val date = dateFormat.format(Date(tx.timestamp))
                canvas.drawText(date, 50f, currentY, paint)
                canvas.drawText(tx.tag.take(15), 180f, currentY, paint)
                val type = if (tx.isIncome) "Income" else "Expense"
                paint.color = if (tx.isIncome) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                canvas.drawText(type, 300f, currentY, paint)
                canvas.drawText(String.format("%.2f %s", tx.amount, tx.currency), 400f, currentY, paint)
                
                currentY += 20f
                paint.color = Color.BLACK
            }

            document.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun drawPieChart(canvas: Canvas, data: Map<String, Double>, startX: Float, startY: Float) {
        val paint = Paint().apply { isAntiAlias = true }
        val rectF = RectF(startX, startY, startX + 200f, startY + 200f)
        val total = data.values.sum()
        var currentAngle = 0f
        
        val colors = listOf(
            Color.parseColor("#FF6384"),
            Color.parseColor("#36A2EB"),
            Color.parseColor("#FFCE56"),
            Color.parseColor("#4BC0C0"),
            Color.parseColor("#9966FF"),
            Color.parseColor("#FF9F40")
        )

        var colorIndex = 0
        var legendY = startY + 20f

        data.forEach { (category, amount) ->
            val sweepAngle = ((amount / total) * 360f).toFloat()
            paint.color = colors[colorIndex % colors.size]
            canvas.drawArc(rectF, currentAngle, sweepAngle, true, paint)
            currentAngle += sweepAngle

            // Draw Legend
            canvas.drawRect(startX + 250f, legendY - 10f, startX + 265f, legendY + 5f, paint)
            paint.color = Color.BLACK
            paint.textSize = 12f
            canvas.drawText("$category: ${String.format("%.2f", amount)}", startX + 275f, legendY, paint)
            
            legendY += 20f
            colorIndex++
        }
    }

    private fun drawBarChart(canvas: Canvas, data: Map<String, Double>, startX: Float, startY: Float) {
        val paint = Paint().apply { isAntiAlias = true }
        val maxAmount = data.values.maxOrNull() ?: 1.0
        val chartHeight = 200f
        val maxBarWidth = 200f
        
        var currentY = startY + 20f
        val colors = listOf(
            Color.parseColor("#FF6384"),
            Color.parseColor("#36A2EB"),
            Color.parseColor("#FFCE56"),
            Color.parseColor("#4BC0C0"),
            Color.parseColor("#9966FF"),
            Color.parseColor("#FF9F40")
        )

        var colorIndex = 0

        paint.color = Color.BLACK
        paint.textSize = 12f

        data.forEach { (category, amount) ->
            paint.color = Color.BLACK
            canvas.drawText(category.take(10), startX, currentY + 12f, paint)
            
            paint.color = colors[colorIndex % colors.size]
            val barWidth = ((amount / maxAmount) * maxBarWidth).toFloat()
            canvas.drawRect(startX + 80f, currentY, startX + 80f + barWidth, currentY + 15f, paint)
            
            paint.color = Color.BLACK
            canvas.drawText(String.format("%.2f", amount), startX + 90f + barWidth, currentY + 12f, paint)
            
            currentY += 25f
            colorIndex++
        }
    }
}

package com.pressione.iperteso.ui.screens.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.domain.model.Reading
import com.pressione.iperteso.ui.theme.CategoryGrade1

/**
 * Custom Canvas-based line chart for systolic/diastolic trend.
 * No external library needed — pure Compose Canvas.
 */
@Composable
fun BpTrendChart(readings: List<Reading>, modifier: Modifier = Modifier) {
    val sorted = readings.sortedBy { it.timestamp.toEpochMilli() }
    val sysColor = MaterialTheme.colorScheme.error
    val diaColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val targetColor = Color(0xFF2E7D32).copy(alpha = 0.25f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        if (sorted.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val paddingLeft = 8.dp.toPx()
        val paddingRight = 8.dp.toPx()
        val paddingTop = 12.dp.toPx()
        val paddingBottom = 12.dp.toPx()
        val chartW = w - paddingLeft - paddingRight
        val chartH = h - paddingTop - paddingBottom

        val minY = 50f
        val maxY = 220f
        val rangeY = maxY - minY

        fun yFor(v: Float): Float = paddingTop + chartH * (1f - (v - minY) / rangeY)
        fun xFor(i: Int): Float = if (sorted.size <= 1) paddingLeft + chartW / 2
            else paddingLeft + chartW * (i.toFloat() / (sorted.size - 1))

        // Target zone 90-140
        val zoneTop = yFor(140f)
        val zoneBottom = yFor(90f)
        drawRect(
            color = targetColor,
            topLeft = Offset(paddingLeft, zoneTop),
            size = androidx.compose.ui.geometry.Size(chartW, zoneBottom - zoneTop)
        )

        // Grid lines at 60, 90, 120, 140, 180
        listOf(60f, 90f, 120f, 140f, 180f).forEach { v ->
            val y = yFor(v)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartW, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Systolic line
        if (sorted.isNotEmpty()) {
            val sysPath = Path()
            sorted.forEachIndexed { i, r ->
                val x = xFor(i)
                val y = yFor(r.systolic.toFloat())
                if (i == 0) sysPath.moveTo(x, y) else sysPath.lineTo(x, y)
            }
            drawPath(sysPath, color = sysColor, style = Stroke(width = 2.dp.toPx()))
        }

        // Diastolic line
        if (sorted.isNotEmpty()) {
            val diaPath = Path()
            sorted.forEachIndexed { i, r ->
                val x = xFor(i)
                val y = yFor(r.diastolic.toFloat())
                if (i == 0) diaPath.moveTo(x, y) else diaPath.lineTo(x, y)
            }
            drawPath(diaPath, color = diaColor, style = Stroke(width = 2.dp.toPx()))
        }

        // Threshold line at 140
        val thresholdY = yFor(140f)
        drawLine(
            color = CategoryGrade1,
            start = Offset(paddingLeft, thresholdY),
            end = Offset(paddingLeft + chartW, thresholdY),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

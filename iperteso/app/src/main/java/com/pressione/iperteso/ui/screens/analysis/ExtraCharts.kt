package com.pressione.iperteso.ui.screens.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pressione.iperteso.domain.model.Reading
import java.time.ZoneId

/**
 * Canvas-based bar chart for hourly BP derivatives (dP/dt).
 * Red bars = rapid rise (>10 mmHg/h), warning for cardiovascular risk.
 */
@Composable
fun DerivativesBarChart(
    readings: List<Reading>,
    valueOf: (Reading) -> Int,
    modifier: Modifier = Modifier
) {
    val sorted = readings.sortedBy { it.timestamp.toEpochMilli() }
    val derivatives = sorted.zipWithNext { a, b ->
        val hours = (b.timestamp.toEpochMilli() - a.timestamp.toEpochMilli()) / (1000.0 * 3600.0)
        if (hours > 0) (valueOf(b) - valueOf(a)).toFloat() / hours.toFloat() else 0f
    }

    val positiveColor = Color(0xFFD32F2F).copy(alpha = 0.8f)
    val negativeColor = Color(0xFF1976D2).copy(alpha = 0.5f)
    val alarmColor = Color(0xFFD32F2F)
    val zeroLineColor = MaterialTheme.colorScheme.outlineVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 8.sp
    )

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        if (derivatives.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val paddingLeft = 30.dp.toPx()
        val paddingRight = 4.dp.toPx()
        val plotLeft = paddingLeft
        val plotW = (w - paddingLeft - paddingRight).coerceAtLeast(1f)
        val centerY = h / 2f
        val plotHalf = h / 2f - 8.dp.toPx()

        // Nice round scale for the y axis (>= max |dP/dt|, at least 10)
        val rawMax = derivatives.maxOf { kotlin.math.abs(it) }.coerceAtLeast(10f)
        val scale = when {
            rawMax <= 10f -> 10f
            rawMax <= 15f -> 15f
            rawMax <= 20f -> 20f
            rawMax <= 30f -> 30f
            rawMax <= 40f -> 40f
            rawMax <= 50f -> 50f
            else -> kotlin.math.ceil(rawMax / 10f) * 10f
        }

        fun yFor(v: Float): Float = centerY - (v / scale) * plotHalf

        // Y-axis gridlines + value labels
        listOf(-scale, -10f, 0f, 10f, scale).distinct().forEach { v ->
            val y = yFor(v)
            drawLine(
                color = if (v == 0f) zeroLineColor else gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotLeft + plotW, y),
                strokeWidth = 1.dp.toPx()
            )
            val label = if (v == 0f) "0" else "%+.0f".format(v)
            val layout = textMeasurer.measure(label, axisStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(plotLeft - layout.size.width - 4.dp.toPx(), y - layout.size.height / 2f)
            )
        }

        // Bars
        val barW = (plotW / derivatives.size) * 0.7f
        derivatives.forEachIndexed { i, d ->
            val slotW = plotW / derivatives.size
            val x = plotLeft + i * slotW + (slotW - barW) / 2
            val barH = (kotlin.math.abs(d) / scale) * plotHalf
            val color = if (d > 10f) alarmColor else if (d > 0f) positiveColor else negativeColor

            if (d >= 0f) {
                drawRect(
                    color = color,
                    topLeft = Offset(x, centerY - barH),
                    size = Size(barW, barH)
                )
            } else {
                drawRect(
                    color = color,
                    topLeft = Offset(x, centerY),
                    size = Size(barW, barH)
                )
            }
        }

        // Alarm threshold line at +10 mmHg/h (only if within scale)
        if (scale > 10f) {
            drawLine(
                color = alarmColor,
                start = Offset(plotLeft, yFor(10f)),
                end = Offset(plotLeft + plotW, yFor(10f)),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

/**
 * Canvas-based doughnut chart for category distribution.
 */
@Composable
fun CategoryDoughnutChart(
    distribution: Map<com.pressione.iperteso.domain.model.Category, Int>,
    modifier: Modifier = Modifier
) {
    val total = distribution.values.sum()
    val colors = mapOf(
        com.pressione.iperteso.domain.model.Category.OPTIMAL to Color(0xFF2E7D32),
        com.pressione.iperteso.domain.model.Category.NORMAL to Color(0xFF66BB6A),
        com.pressione.iperteso.domain.model.Category.HIGH_NORMAL to Color(0xFFFFA726),
        com.pressione.iperteso.domain.model.Category.GRADE_1 to Color(0xFFEF6C00),
        com.pressione.iperteso.domain.model.Category.GRADE_2 to Color(0xFFD32F2F),
        com.pressione.iperteso.domain.model.Category.GRADE_3 to Color(0xFFBA1A1A),
        com.pressione.iperteso.domain.model.Category.CRISIS to Color(0xFF880E4F)
    )

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        if (total <= 0) return@Canvas

        val stroke = 40.dp.toPx()
        val radius = size.minDimension / 2f - stroke
        val center = Offset(size.width / 2f, size.height / 2f)
        var startAngle = -90f

        com.pressione.iperteso.domain.model.Category.entries.forEach { cat ->
            val count = distribution[cat] ?: 0
            if (count > 0) {
                val sweep = count.toFloat() / total * 360f
                drawArc(
                    color = colors[cat] ?: Color.Gray,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                startAngle += sweep
            }
        }
    }
}

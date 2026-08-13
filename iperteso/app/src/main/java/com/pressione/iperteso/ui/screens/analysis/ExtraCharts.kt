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
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.domain.model.Reading
import java.time.ZoneId

/**
 * Canvas-based bar chart for hourly BP derivatives (dP/dt).
 * Red bars = rapid rise (>10 mmHg/h), warning for cardiovascular risk.
 */
@Composable
fun DerivativesBarChart(readings: List<Reading>, modifier: Modifier = Modifier) {
    val sorted = readings.sortedBy { it.timestamp.toEpochMilli() }
    val derivatives = sorted.zipWithNext { a, b ->
        val hours = (b.timestamp.toEpochMilli() - a.timestamp.toEpochMilli()) / (1000.0 * 3600.0)
        if (hours > 0) (b.systolic - a.systolic).toFloat() / hours.toFloat() else 0f
    }

    val positiveColor = Color(0xFFD32F2F).copy(alpha = 0.8f)
    val negativeColor = Color(0xFF1976D2).copy(alpha = 0.5f)
    val alarmColor = Color(0xFFD32F2F)
    val zeroLineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        if (derivatives.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val maxAbs = maxOf(derivatives.maxOf { kotlin.math.abs(it) }, 1f)
        val barW = (w / derivatives.size) * 0.7f
        val centerY = h / 2f

        derivatives.forEachIndexed { i, d ->
            val x = i * (w / derivatives.size) + (w / derivatives.size - barW) / 2
            val barH = (kotlin.math.abs(d) / maxAbs) * (h / 2f - 8.dp.toPx())
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

        // Zero line
        drawLine(
            color = zeroLineColor,
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = 1.dp.toPx()
        )

        // Alarm threshold label
        drawLine(
            color = alarmColor,
            start = Offset(0f, centerY - (10f / maxAbs) * (h / 2f)),
            end = Offset(w, centerY - (10f / maxAbs) * (h / 2f)),
            strokeWidth = 1.5.dp.toPx()
        )
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

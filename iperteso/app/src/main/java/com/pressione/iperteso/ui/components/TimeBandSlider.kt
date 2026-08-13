package com.pressione.iperteso.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pressione.iperteso.domain.model.TimeBand
import kotlin.math.roundToInt

private val BandColors = listOf(
    Color(0xFFFFB347), // morning
    Color(0xFF87CEEB), // afternoon
    Color(0xFFDDA0DD), // evening
    Color(0xFF3D5A80)  // night
)
private val BandEmoji = listOf("\u2600\uFE0F", "\uD83C\uDF24\uFE0F", "\uD83C\uDF05", "\uD83C\uDF19")

/**
 * Visual 24h time-band editor: drag the dividers to change band boundaries.
 * Port of the web app's TimeBandSlider.vue — adjacent bands share boundaries.
 */
@Composable
fun TimeBandSlider(
    bands: List<TimeBand>,
    onBandsChange: (List<TimeBand>) -> Unit
) {
    var trackWidth by remember { mutableStateOf(0) }
    var dragSnapshot by remember { mutableStateOf<List<TimeBand>?>(null) }
    val currentBands by rememberUpdatedState(bands)
    val currentOnChange by rememberUpdatedState(onBandsChange)

    fun applyDrag(index: Int, deltaPx: Float) {
        if (trackWidth <= 0) return
        val snapshot = dragSnapshot ?: currentBands.toList().also { dragSnapshot = it }
        val deltaHours = (deltaPx / trackWidth * 24f).roundToInt()
        if (deltaHours == 0) return

        val newBands = snapshot.toMutableList()
        val band = snapshot[index]
        val newEnd = ((band.endHour + deltaHours) % 24 + 24) % 24
        if (newEnd == band.startHour) return // no collapse
        val nextIndex = (index + 1) % newBands.size
        val next = snapshot[nextIndex]
        if (newEnd == next.endHour) return // no collapse of adjacent band

        newBands[index] = band.copy(endHour = newEnd)
        newBands[nextIndex] = next.copy(startHour = newEnd)
        currentOnChange(newBands)
    }

    fun endDrag() { dragSnapshot = null }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Hour labels
        Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            if (trackWidth > 0) {
                listOf(0, 6, 12, 18).forEach { h ->
                    Text(
                        "$h",
                        modifier = Modifier.offset { IntOffset((h / 24f * trackWidth).roundToInt() - 8, 0) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .onSizeChanged { trackWidth = it.width }
        ) {
            // Colored segments
            Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val w = size.width
                fun x(hour: Float) = (hour / 24f) * w
                bands.forEachIndexed { i, b ->
                    val segments = if (b.startHour <= b.endHour) {
                        listOf(b.startHour to b.endHour)
                    } else {
                        listOf(0 to b.endHour, b.startHour to 24)
                    }
                    segments.forEach { (s, e) ->
                        drawRect(
                            color = BandColors[i % BandColors.size],
                            topLeft = Offset(x(s.toFloat()), 0f),
                            size = Size(x(e.toFloat()) - x(s.toFloat()), size.height)
                        )
                    }
                }
                // Separator lines
                bands.forEach { b ->
                    val dx = x(b.endHour.toFloat())
                    drawLine(
                        color = Color.White,
                        start = Offset(dx, 0f),
                        end = Offset(dx, size.height),
                        strokeWidth = 3f
                    )
                }
            }

            // Draggable dividers
            if (trackWidth > 0) {
                bands.forEachIndexed { i, b ->
                    val centerX = (b.endHour / 24f * trackWidth).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(centerX - 24, 0) }
                            .width(48.dp)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                var cumulative = 0f
                                detectDragGestures(
                                    onDragStart = { cumulative = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        cumulative += dragAmount.x
                                        applyDrag(i, cumulative)
                                    },
                                    onDragEnd = { endDrag() },
                                    onDragCancel = { endDrag() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        // Legend
        Column(modifier = Modifier.padding(top = 12.dp)) {
            bands.forEachIndexed { i, b ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(BandColors[i % BandColors.size])
                    )
                    Text(
                        "  ${BandEmoji[i % BandEmoji.size]} ${b.label}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "%02d:00 – %02d:00".format(b.startHour, b.endHour),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

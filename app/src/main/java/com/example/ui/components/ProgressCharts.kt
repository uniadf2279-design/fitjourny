package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.MeasurementLog
import com.example.data.model.WorkoutLog

@Composable
fun WeightTrendChart(
    measurements: List<MeasurementLog>,
    title: String,
    emptyHint: String,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val fillBottom = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (measurements.isNotEmpty()) {
                    val latest = measurements.last().weightKg
                    Text(
                        text = "${"%.1f".format(latest)} kg",
                        style = MaterialTheme.typography.labelLarge,
                        color = lineColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (measurements.size < 2) {
                Text(
                    text = emptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val points = measurements.takeLast(12)
                val minW = (points.minOf { it.weightKg } - 1.5).toFloat()
                val maxW = (points.maxOf { it.weightKg } + 1.5).toFloat()
                val span = (maxW - minW).coerceAtLeast(1f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val padX = 16f
                    val padY = 16f
                    val chartW = size.width - padX * 2
                    val chartH = size.height - padY * 2

                    for (i in 0..2) {
                        val y = padY + chartH * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(padX, y),
                            end = Offset(size.width - padX, y),
                            strokeWidth = 2f
                        )
                    }

                    val coords = points.mapIndexed { idx, item ->
                        val x = padX + chartW * (idx.toFloat() / (points.size - 1).coerceAtLeast(1))
                        val normY = ((item.weightKg.toFloat() - minW) / span).coerceIn(0f, 1f)
                        val y = padY + chartH * (1f - normY)
                        Offset(x, y)
                    }

                    val linePath = Path().apply {
                        coords.forEachIndexed { idx, offset ->
                            if (idx == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(coords.last().x, padY + chartH)
                        lineTo(coords.first().x, padY + chartH)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(listOf(fillTop, fillBottom))
                    )
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                    coords.forEach { pt ->
                        drawCircle(color = lineColor, radius = 8f, center = pt)
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyActivityBarChart(
    logs: List<WorkoutLog>,
    title: String,
    emptyHint: String,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            val recent = logs.take(7).reversed()
            if (recent.isEmpty()) {
                Text(
                    text = emptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxMins = recent.maxOf { it.durationMinutes }.coerceAtLeast(30L).toFloat()
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val count = 7
                    val slotW = size.width / count
                    val barW = slotW * 0.48f
                    val chartH = size.height - 16f

                    for (i in 0 until count) {
                        val x = i * slotW + (slotW - barW) / 2f
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(x, 8f),
                            size = Size(barW, chartH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                        val log = recent.getOrNull(i)
                        if (log != null) {
                            val ratio = (log.durationMinutes.toFloat() / maxMins).coerceIn(0.1f, 1f)
                            val bh = chartH * ratio
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, 8f + chartH - bh),
                                size = Size(barW, bh),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }
                }
            }
        }
    }
}

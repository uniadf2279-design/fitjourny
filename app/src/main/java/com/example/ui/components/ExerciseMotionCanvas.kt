package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.LocalFitStrings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExerciseMotionPlayer(
    animationType: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalFitStrings.current
    var isPlaying by remember { mutableStateOf(true) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, speedMultiplier) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = 0L
        while (isPlaying) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    phase = (phase + dt * 0.45f * speedMultiplier) % 1f
                }
                lastNanos = frameNanos
            }
        }
    }

    // Smooth ping-pong [0..1] cycle for movement down/up
    val effort = (0.5f - 0.5f * cos(phase * 2f * PI.toFloat()))
    val phaseLabel = when {
        effort < 0.25f -> if (strings.isArabic) "وضع البداية" else "Start Position"
        effort < 0.75f -> if (strings.isArabic) "مرحلة الأداء" else "Active Phase"
        else -> if (strings.isArabic) "أقصى انقباض" else "Peak Contraction"
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.tertiary
    val floorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val headFill = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = phaseLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryColor
                )
                Text(
                    text = "${speedMultiplier}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val floorY = h * 0.84f

                    // Floor guide line
                    drawLine(
                        color = floorColor,
                        start = Offset(w * 0.1f, floorY),
                        end = Offset(w * 0.9f, floorY),
                        strokeWidth = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )

                    fun drawJoint(center: Offset, radius: Float = 8f, color: Color = accentColor) {
                        drawCircle(color = color, radius = radius, center = center)
                    }

                    fun drawLimb(a: Offset, b: Offset, stroke: Float = 11f, color: Color = primaryColor) {
                        drawLine(
                            color = color,
                            start = a,
                            end = b,
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }

                    when (animationType) {
                        "squat" -> {
                            val cx = w * 0.5f
                            val ankle = Offset(cx + 12f, floorY)
                            val drop = effort * h * 0.24f
                            val knee = Offset(cx + 38f * effort + 16f, floorY - h * 0.22f + drop * 0.2f)
                            val hip = Offset(cx - 28f * effort - 8f, floorY - h * 0.43f + drop)
                            val shoulder = Offset(hip.x + 22f + 16f * effort, hip.y - h * 0.24f)
                            val head = Offset(shoulder.x + 6f, shoulder.y - 22f)
                            val hand = Offset(shoulder.x + 48f * effort + 18f, shoulder.y + 18f - 18f * effort)

                            drawLimb(ankle, knee)
                            drawLimb(knee, hip)
                            drawLimb(hip, shoulder, stroke = 13f)
                            drawLimb(shoulder, hand, stroke = 9f)
                            drawCircle(color = headFill, radius = 16f, center = head)
                            drawCircle(color = primaryColor, radius = 16f, center = head, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                            drawJoint(hip)
                            drawJoint(knee)
                            drawJoint(shoulder)
                        }

                        "pushup" -> {
                            val foot = Offset(w * 0.22f, floorY - 6f)
                            val palm = Offset(w * 0.74f, floorY - 6f)
                            val drop = effort * h * 0.16f
                            val shoulder = Offset(w * 0.68f, floorY - h * 0.30f + drop)
                            val hip = Offset(w * 0.45f, floorY - h * 0.19f + drop * 0.5f)
                            val elbow = Offset(w * 0.78f, (shoulder.y + palm.y) * 0.5f)
                            val head = Offset(shoulder.x + 26f, shoulder.y - 12f)

                            drawLimb(foot, hip)
                            drawLimb(hip, shoulder, stroke = 13f)
                            drawLimb(shoulder, elbow, stroke = 9f)
                            drawLimb(elbow, palm, stroke = 9f)
                            drawCircle(color = headFill, radius = 15f, center = head)
                            drawCircle(color = primaryColor, radius = 15f, center = head, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                            drawJoint(shoulder)
                            drawJoint(elbow)
                            drawJoint(hip)
                        }

                        "plank" -> {
                            val breathe = sin(phase * 2f * PI.toFloat()) * 4f
                            val foot = Offset(w * 0.22f, floorY - 6f)
                            val elbow = Offset(w * 0.69f, floorY - 6f)
                            val wrist = Offset(w * 0.78f, floorY - 6f)
                            val shoulder = Offset(w * 0.68f, floorY - h * 0.25f + breathe)
                            val hip = Offset(w * 0.45f, floorY - h * 0.20f + breathe)
                            val head = Offset(shoulder.x + 28f, shoulder.y - 10f)

                            drawLimb(foot, hip)
                            drawLimb(hip, shoulder, stroke = 13f)
                            drawLimb(shoulder, elbow, stroke = 9f)
                            drawLimb(elbow, wrist, stroke = 9f)
                            drawCircle(color = headFill, radius = 15f, center = head)
                            drawCircle(color = primaryColor, radius = 15f, center = head, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                            drawJoint(shoulder)
                            drawJoint(hip)
                            drawJoint(elbow)
                        }

                        "lunge" -> {
                            val cx = w * 0.5f
                            val drop = effort * h * 0.18f
                            val frontFoot = Offset(cx + 52f, floorY)
                            val backFoot = Offset(cx - 56f, floorY)
                            val hip = Offset(cx - 4f, floorY - h * 0.44f + drop)
                            val frontKnee = Offset(cx + 50f, floorY - h * 0.23f + drop * 0.35f)
                            val backKnee = Offset(cx - 34f, floorY - h * 0.22f + drop * 0.9f)
                            val shoulder = Offset(hip.x, hip.y - h * 0.24f)
                            val head = Offset(shoulder.x, shoulder.y - 22f)

                            drawLimb(backFoot, backKnee)
                            drawLimb(backKnee, hip)
                            drawLimb(frontFoot, frontKnee)
                            drawLimb(frontKnee, hip)
                            drawLimb(hip, shoulder, stroke = 13f)
                            drawCircle(color = headFill, radius = 15f, center = head)
                            drawCircle(color = primaryColor, radius = 15f, center = head, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                            drawJoint(hip)
                            drawJoint(frontKnee)
                            drawJoint(backKnee)
                        }

                        "bridge" -> {
                            val shoulder = Offset(w * 0.30f, floorY - 12f)
                            val head = Offset(w * 0.20f, floorY - 16f)
                            val foot = Offset(w * 0.74f, floorY)
                            val knee = Offset(w * 0.64f, floorY - h * 0.28f)
                            val hipLift = effort * h * 0.20f
                            val hip = Offset(w * 0.48f, floorY - 12f - hipLift)

                            drawLimb(shoulder, hip, stroke = 13f)
                            drawLimb(hip, knee)
                            drawLimb(knee, foot)
                            drawCircle(color = headFill, radius = 15f, center = head)
                            drawCircle(color = primaryColor, radius = 15f, center = head, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                            drawJoint(hip)
                            drawJoint(knee)
                            drawJoint(shoulder)
                        }

                        else -> { // jumping_jack
                            val cx = w * 0.5f
                            val jumpLift = effort * 18f
                            val hip = Offset(cx, floorY - h * 0.38f - jumpLift)
                            val shoulder = Offset(cx, hip.y - h * 0.23f)
                            val head = Offset(cx, shoulder.y - 22f)
                            val footSpread = 14f + effort * 56f
                            val leftFoot = Offset(cx - footSpread, floorY - jumpLift)
                            val rightFoot = Offset(cx + footSpread, floorY - jumpLift)
                            val armAngle = -PI.toFloat() * 0.4f + effort * PI.toFloat() * 0.8f
                            val leftHand = Offset(
                                shoulder.x - cos(armAngle) * 52f,
                                shoulder.y - sin(armAngle) * 52f
                            )
                            val rightHand = Offset(
                                shoulder.x + cos(armAngle) * 52f,
                                shoulder.y - sin(armAngle) * 52f
                            )

                            drawLimb(hip, leftFoot)
                            drawLimb(hip, rightFoot)
                            drawLimb(hip, shoulder, stroke = 13f)
                            drawLimb(shoulder, leftHand, stroke = 9f)
                            drawLimb(shoulder, rightHand, stroke = 9f)
                            drawCircle(color = headFill, radius = 15f, center = head)
                            drawCircle(color = primaryColor, radius = 15f, center = head, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                            drawJoint(hip)
                            drawJoint(shoulder)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { effort },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = primaryColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.testTag("exercise_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) strings.pauseAnimation else strings.playAnimation
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPlaying) strings.pauseAnimation else strings.playAnimation)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5f, 1.0f, 1.5f).forEach { spd ->
                        FilterChip(
                            selected = speedMultiplier == spd,
                            onClick = { speedMultiplier = spd },
                            label = { Text("${spd}x") },
                            modifier = Modifier.testTag("speed_chip_${spd}")
                        )
                    }
                }
            }
        }
    }
}

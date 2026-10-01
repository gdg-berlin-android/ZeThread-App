package de.berlindroid.zethread.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.berlindroid.zethread.ui.theme.TerminalGreen

@Composable
fun CrochetPatchFrameOverlay(
    modifier: Modifier = Modifier,
    cornerLength: Float = 48f,
    strokeWidth: Float = 6f
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left corner
            drawLine(
                color = TerminalGreen,
                start = Offset(0f, 0f),
                end = Offset(cornerLength, 0f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = TerminalGreen,
                start = Offset(0f, 0f),
                end = Offset(0f, cornerLength),
                strokeWidth = strokeWidth
            )

            // Top-right corner
            drawLine(
                color = TerminalGreen,
                start = Offset(width, 0f),
                end = Offset(width - cornerLength, 0f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = TerminalGreen,
                start = Offset(width, 0f),
                end = Offset(width, cornerLength),
                strokeWidth = strokeWidth
            )

            // Bottom-left corner
            drawLine(
                color = TerminalGreen,
                start = Offset(0f, height),
                end = Offset(cornerLength, height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = TerminalGreen,
                start = Offset(0f, height),
                end = Offset(0f, height - cornerLength),
                strokeWidth = strokeWidth
            )

            // Bottom-right corner
            drawLine(
                color = TerminalGreen,
                start = Offset(width, height),
                end = Offset(width - cornerLength, height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = TerminalGreen,
                start = Offset(width, height),
                end = Offset(width, height - cornerLength),
                strokeWidth = strokeWidth
            )

            // Subtle dashed bounding box
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            drawRect(
                color = TerminalGreen.copy(alpha = 0.3f),
                topLeft = Offset(0f, 0f),
                size = size,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = dashPathEffect
                )
            )

            // Center target crosshair
            val centerX = width / 2
            val centerY = height / 2
            val crosshairSize = 16f

            drawLine(
                color = TerminalGreen.copy(alpha = 0.5f),
                start = Offset(centerX - crosshairSize, centerY),
                end = Offset(centerX + crosshairSize, centerY),
                strokeWidth = 1.5f
            )
            drawLine(
                color = TerminalGreen.copy(alpha = 0.5f),
                start = Offset(centerX, centerY - crosshairSize),
                end = Offset(centerX, centerY + crosshairSize),
                strokeWidth = 1.5f
            )
        }

        Text(
            text = "Crochet Patch Frame (1:1)",
            color = TerminalGreen.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )
    }
}

package com.wbtracker.app.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wbtracker.app.domain.model.PricePoint
import com.wbtracker.app.ui.theme.PulseGradientMiddle
import com.wbtracker.app.ui.theme.PulseGradientStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PulseCanvasChart(
    points: List<PricePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = PulseGradientMiddle,
    gradientTopColor: Color = PulseGradientStart.copy(alpha = 0.4f),
    gradientBottomColor: Color = Color.Transparent,
    showGrid: Boolean = true,
    height: Dp = 200.dp,
    onPointSelected: (PricePoint?) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .height(height)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "История цен пуста",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("ru")) }

    Box(
        modifier = modifier
            .height(height)
            .fillMaxSize()
    ) {
        if (points.size == 1) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "История цен формируется (1 фиксация)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 24.dp, start = 12.dp, end = 12.dp)
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val width = size.width.toFloat()
                            if (width > 0 && points.isNotEmpty()) {
                                val stepX = width / (points.size - 1).coerceAtLeast(1)
                                val index = (offset.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                                val newIdx = if (selectedIndex == index) null else index
                                if (newIdx != selectedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                selectedIndex = newIdx
                                onPointSelected(if (newIdx != null) points[index] else null)
                            }
                        }
                    )
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragEnd = {},
                        onDragCancel = {},
                        onDrag = { change, _ ->
                            change.consume()
                            val width = size.width.toFloat()
                            if (width > 0 && points.isNotEmpty()) {
                                val stepX = width / (points.size - 1).coerceAtLeast(1)
                                val index = (change.position.x / stepX).roundToInt().coerceIn(0, points.size - 1)
                                if (selectedIndex != index) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedIndex = index
                                    onPointSelected(points[index])
                                }
                            }
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val prices = points.map { if (it.walletPrice > 0) it.walletPrice else it.sellerPrice }
            val minPrice = prices.minOrNull() ?: 0.0
            val maxPrice = prices.maxOrNull() ?: 0.0
            val priceRange = if (maxPrice == minPrice) 1.0 else (maxPrice - minPrice)

            val screenPoints = points.mapIndexed { index, point ->
                val pPrice = if (point.walletPrice > 0) point.walletPrice else point.sellerPrice
                val x = if (points.size == 1) canvasWidth / 2f
                else index.toFloat() / (points.size - 1) * canvasWidth
                val y = canvasHeight - ((pPrice - minPrice) / priceRange * (canvasHeight * 0.75f) + canvasHeight * 0.125f).toFloat()
                Offset(x, y)
            }

            // 1. Сетка координат (Grid lines)
            if (showGrid) {
                val gridLineColor = Color.White.copy(alpha = 0.06f)
                val steps = 3
                for (i in 0..steps) {
                    val y = canvasHeight * (i.toFloat() / steps)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            if (screenPoints.size == 1) {
                val point = screenPoints[0]
                val baseRadius = 6.dp.toPx()
                drawCircle(
                    color = lineColor.copy(alpha = pulseAlpha),
                    radius = baseRadius * pulseScale,
                    center = point
                )
                drawCircle(
                    color = lineColor,
                    radius = baseRadius,
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f.dp.toPx(),
                    center = point
                )
                return@Canvas
            }

            // 2. Кубические кривые Безье (Cubic Bezier Path)
            val linePath = Path()
            val fillPath = Path()

            linePath.moveTo(screenPoints[0].x, screenPoints[0].y)
            fillPath.moveTo(screenPoints[0].x, canvasHeight)
            fillPath.lineTo(screenPoints[0].x, screenPoints[0].y)

            for (i in 0 until screenPoints.size - 1) {
                val p1 = screenPoints[i]
                val p2 = screenPoints[i + 1]

                val controlX1 = p1.x + (p2.x - p1.x) / 2f
                val controlY1 = p1.y
                val controlX2 = p1.x + (p2.x - p1.x) / 2f
                val controlY2 = p2.y

                linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
            }

            fillPath.lineTo(screenPoints.last().x, canvasHeight)
            fillPath.close()

            // 3. Заливка под графиком (Gradient Area Fill)
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTopColor, gradientBottomColor),
                    startY = 0f,
                    endY = canvasHeight
                )
            )

            // 4. Линия графика (Smooth Cubic Bezier Line)
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 5. Пульсирующий светящийся маркер (Pulse Glowing Marker)
            val activeIndex = selectedIndex ?: (screenPoints.size - 1)
            val activePoint = screenPoints[activeIndex]
            val baseRadius = 6.dp.toPx()

            // Неоновый внешний пульсирующий круг
            drawCircle(
                color = lineColor.copy(alpha = pulseAlpha),
                radius = baseRadius * pulseScale,
                center = activePoint
            )
            // Внутренняя яркая точка
            drawCircle(
                color = lineColor,
                radius = baseRadius,
                center = activePoint
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f.dp.toPx(),
                center = activePoint
            )

            // 6. Интерактивный Tooltip
            if (selectedIndex != null && selectedIndex!! in screenPoints.indices) {
                val idx = selectedIndex!!
                val p = screenPoints[idx]
                val item = points[idx]
                val pPrice = if (item.walletPrice > 0) item.walletPrice else item.sellerPrice

                // Вертикальная линия индикатора
                drawLine(
                    color = lineColor.copy(alpha = 0.5f),
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, canvasHeight),
                    strokeWidth = 1.dp.toPx()
                )

                val priceText = "${pPrice.roundToInt()} ₽"
                val dateText = dateFormat.format(Date(item.timestamp))
                val tooltipText = "$dateText: $priceText"

                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                val textWidth = textPaint.measureText(tooltipText)
                val paddingPx = 8.dp.toPx()
                val rectWidth = textWidth + paddingPx * 2
                val rectHeight = 28.dp.toPx()

                var rectX = p.x - rectWidth / 2f
                rectX = rectX.coerceIn(4.dp.toPx(), canvasWidth - rectWidth - 4.dp.toPx())

                val rectY = (p.y - rectHeight - 12.dp.toPx()).coerceAtLeast(0f)

                // Фон тултипа
                drawRoundRect(
                    color = Color(0xFF14141D),
                    topLeft = Offset(rectX, rectY),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(rectX, rectY),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Отрисовка текста внутри тултипа
                drawContext.canvas.nativeCanvas.drawText(
                    tooltipText,
                    rectX + rectWidth / 2f,
                    rectY + rectHeight / 2f + 4.sp.toPx() / 2f,
                    textPaint
                )
            }
        }
    }
}

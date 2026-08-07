package com.wbtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wbtracker.app.domain.model.PricePoint
import com.wbtracker.app.ui.theme.PriceDropGreen
import com.wbtracker.app.ui.theme.PulseGradientBrush
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HeroSavingsCard(
    totalSavingsRub: Long,
    trackedCount: Int,
    priceDropCount: Int,
    modifier: Modifier = Modifier,
    chartPoints: List<PricePoint> = emptyList(),
    maxDiscountPercent: Int = 0
) {
    val formattedSavings = rememberFormattedNumber(totalSavingsRub)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PulseGradientBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Экономия",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Экономия за месяц",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (maxDiscountPercent > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PriceDropGreen)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "-$maxDiscountPercent%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Savings Amount
            Text(
                text = "$formattedSavings ₽",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroStatBadge(
                    text = "$trackedCount товаров",
                    backgroundColor = Color.Black.copy(alpha = 0.25f)
                )
                if (priceDropCount > 0) {
                    HeroStatBadge(
                        text = "$priceDropCount подешевели",
                        backgroundColor = PriceDropGreen.copy(alpha = 0.85f),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            // Embedded Chart if points provided
            if (chartPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                PulseCanvasChart(
                    points = chartPoints,
                    lineColor = Color.White,
                    gradientTopColor = Color.White.copy(alpha = 0.35f),
                    gradientBottomColor = Color.Transparent,
                    showGrid = false,
                    height = 90.dp
                )
            }
        }
    }
}

@Composable
private fun HeroStatBadge(
    text: String,
    backgroundColor: Color,
    icon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun rememberFormattedNumber(value: Long): String {
    val formatter = remember { NumberFormat.getNumberInstance(Locale("ru", "RU")) }
    return remember(value) { formatter.format(value) }
}

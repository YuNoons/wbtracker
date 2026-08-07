package com.wbtracker.app.ui.screen.analytics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wbtracker.app.ui.components.PulseCanvasChart
import com.wbtracker.app.ui.theme.PriceDropGreen
import com.wbtracker.app.ui.theme.PulseGradientEnd
import com.wbtracker.app.ui.theme.PulseGradientStart
import com.wbtracker.app.ui.theme.ReviewColor
import com.wbtracker.app.ui.theme.WbWalletBlue

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val pricePoints by viewModel.basketPricePoints.collectAsStateWithLifecycle()
    val insights by viewModel.discountInsights.collectAsStateWithLifecycle()
    val ratingBreakdown by viewModel.ratingBreakdown.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Аналитика корзины",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Инсайты по ценам и распределению отзывов",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (products.isEmpty()) {
                item {
                    AnalyticsEmptyState()
                }
            } else {
                // Tab Switcher: "Цены / Отзывы"
                item {
                    AnalyticsTabSegmentedControl(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }

                // Animated Tab Content
                item {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "analytics_tab_transition"
                    ) { tab ->
                        when (tab) {
                            AnalyticsTab.PRICES -> {
                                PricesTabContent(
                                    pricePoints = pricePoints,
                                    insights = insights
                                )
                            }
                            AnalyticsTab.REVIEWS -> {
                                ReviewsTabContent(
                                    breakdown = ratingBreakdown
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(PulseGradientStart.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = PulseGradientStart
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Нет данных для аналитики",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Добавьте товары в отслеживание, чтобы сформировать аналитику цен и динамику отзывов",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AnalyticsTabSegmentedControl(
    selectedTab: AnalyticsTab,
    onTabSelected: (AnalyticsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnalyticsTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val bgBrush = if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(PulseGradientStart, PulseGradientEnd)
                    )
                } else null

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (bgBrush != null) Modifier.background(bgBrush) else Modifier)
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onTabSelected(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == AnalyticsTab.PRICES) "Цены" else "Отзывы",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PricesTabContent(
    pricePoints: List<com.wbtracker.app.domain.model.PricePoint>,
    insights: DiscountInsights,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basket Average Price Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = PulseGradientStart,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Средняя цена корзины",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = PriceDropGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = PriceDropGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Тренд вниз",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PriceDropGreen
                            )
                        }
                    }
                }

                // Native PulseCanvasChart
                PulseCanvasChart(
                    points = pricePoints,
                    lineColor = PulseGradientStart,
                    gradientTopColor = PulseGradientStart.copy(alpha = 0.35f),
                    gradientBottomColor = Color.Transparent,
                    showGrid = true,
                    height = 180.dp
                )
            }
        }

        // Discount Insights Title
        Text(
            text = "Инсайты скидок",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // 2x2 Grid of Insight Cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightMetricCard(
                    title = "Макс. скидка",
                    value = "-${insights.maxDiscountPercent}%",
                    icon = Icons.Outlined.LocalOffer,
                    iconTint = PriceDropGreen,
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Среднее снижение",
                    value = "-${insights.avgDiscountPercent}%",
                    icon = Icons.Default.TrendingDown,
                    iconTint = PulseGradientStart,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightMetricCard(
                    title = "WB Кошелёк",
                    value = "${insights.totalWalletSavingsRub} ₽",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconTint = WbWalletBlue,
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    title = "Экономия за месяц",
                    value = "${insights.totalMonthlySavingsRub} ₽",
                    icon = Icons.Outlined.RateReview,
                    iconTint = PulseGradientEnd,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InsightMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun ReviewsTabContent(
    breakdown: RatingBreakdown,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Динамика за 7 дней",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${breakdown.totalNewReviews7Days} новых отзывов",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    color = ReviewColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = ReviewColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${breakdown.avgRating}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReviewColor
                        )
                    }
                }
            }
        }

        // Rating Histogram Section
        Text(
            text = "Гистограмма новых отзывов (7 дней)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HistogramRow(stars = "5★", percentage = breakdown.stars5Pct, barColor = ReviewColor)
                HistogramRow(stars = "4★", percentage = breakdown.stars4Pct, barColor = PulseGradientStart)
                HistogramRow(stars = "3★", percentage = breakdown.stars3Pct, barColor = WbWalletBlue)
                HistogramRow(stars = "2★", percentage = breakdown.stars2Pct, barColor = Color(0xFFFF9800))
                HistogramRow(stars = "1★", percentage = breakdown.stars1Pct, barColor = Color(0xFFE5484D))
            }
        }
    }
}

@Composable
fun HistogramRow(
    stars: String,
    percentage: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stars,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$percentage%",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )
    }
}

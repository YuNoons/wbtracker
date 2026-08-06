package com.wbtracker.app.ui.screen.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.wbtracker.app.domain.model.PriceStats
import com.wbtracker.app.domain.model.ReviewStats

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.fullWidth
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineSpec
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.of
import com.patrykandpatrick.vico.compose.common.rememberMarkerState
import com.patrykandpatrick.vico.compose.m3.material3.charts.rememberVicoTheme
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasureContext
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.Dimensions
import com.patrykandpatrick.vico.core.common.shape.Corner
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val product by viewModel.product.collectAsStateWithLifecycle()
    val priceStatsState by viewModel.priceStats.collectAsStateWithLifecycle()
    val reviewStatsState by viewModel.reviewStats.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val p = product!!

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить товар") },
            text = { Text("Вы действительно хотите удалить этот товар из отслеживания?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.removeProduct(onRemoved = onNavigateBack)
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = p.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                AsyncImage(
                    model = p.thumbnailUrl,
                    contentDescription = p.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentScale = ContentScale.Fit
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = p.brand,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = p.title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${p.currentPrice.toLong()} ₽",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${p.basicPrice.toLong()} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = "С WB Кошельком: ${p.walletPrice.toLong()} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4CAF50), // Green for wallet
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (p.rating != null) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = p.rating.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (p.reviewsCount != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${p.reviewsCount} отзывов",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                when (val state = priceStatsState) {
                    is PriceStatsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is PriceStatsUiState.Success -> {
                        PriceChartSection(stats = state.stats)
                    }
                    is PriceStatsUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(onClick = { viewModel.loadPriceStats() }) {
                                Text("Повторить загрузку")
                            }
                        }
                    }
                }
            }

            item {
                when (val state = reviewStatsState) {
                    is ReviewStatsUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ReviewStatsUiState.Success -> {
                        ReviewChartSection(stats = state.stats)
                    }
                    is ReviewStatsUiState.Error -> {
                        // Silent error for reviews - just don't show the section
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wildberries.ru/catalog/${p.id}/detail.aspx"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Открыть на Wildberries")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить из отслеживания")
                }
            }
        }
    }
}

@Composable
fun PriceChartSection(stats: PriceStats) {
    val chartModelProducer = remember { CartesianChartModelProducer() }
    val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
    
    LaunchedEffect(stats) {
        if (stats.priceHistory.isNotEmpty()) {
            chartModelProducer.runTransaction {
                lineSeries {
                    series(
                        x = stats.priceHistory.mapIndexed { index, _ -> index.toFloat() },
                        y = stats.priceHistory.map { it.sellerPrice.toFloat() }
                    )
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "График цен",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Минимум", style = MaterialTheme.typography.labelMedium)
                Text("${stats.minPrice.toLong()} ₽", style = MaterialTheme.typography.titleMedium)
            }
            Column {
                Text("Максимум", style = MaterialTheme.typography.labelMedium)
                Text("${stats.maxPrice.toLong()} ₽", style = MaterialTheme.typography.titleMedium)
            }
            Column {
                Text("Среднее", style = MaterialTheme.typography.labelMedium)
                Text("${stats.avgPrice.toLong()} ₽", style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (stats.priceHistory.isNotEmpty()) {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lines = listOf(
                            rememberLineSpec(
                                layerStyle = LineCartesianLayer.LineStyle.single(
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                areaFill = LineCartesianLayer.AreaFill.single(
                                    fill = fill(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                )
                            )
                        )
                    ),
                    startAxis = rememberStartAxis(
                        label = rememberAxisLabelComponent(),
                        title = rememberTextComponent(text = "Цена (₽)")
                    ),
                    bottomAxis = rememberBottomAxis(
                        label = rememberAxisLabelComponent(),
                        valueFormatter = { value, _, _ ->
                            val index = value.toInt()
                            if (index >= 0 && index < stats.priceHistory.size) {
                                dateFormat.format(Date(stats.priceHistory[index].timestamp))
                            } else {
                                ""
                            }
                        }
                    ),
                    marker = rememberVicoMarker(
                        stats = stats,
                        dateFormat = dateFormat
                    )
                ),
                modelProducer = chartModelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                scrollState = rememberVicoScrollState(),
                zoomState = rememberVicoZoomState(
                    zoomEnabled = true,
                    initialZoom = remember { Zoom.zoomToFit() }
                )
            )
        } else {
            Text(
                text = "Нет данных для отображения графика",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun rememberVicoMarker(
    stats: PriceStats,
    dateFormat: SimpleDateFormat
): DefaultCartesianMarker {
    return remember(stats) {
        DefaultCartesianMarker(
            label = rememberTextComponent(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                padding = Dimensions.of(8.dp, 4.dp),
                background = rememberShapeComponent(
                    shape = Corner.shape(cornerRadius = 4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ),
                textConverter = { context, measurableText, _ ->
                    val index = measurableText.toIntOrNull() ?: 0
                    if (index >= 0 && index < stats.priceHistory.size) {
                        val point = stats.priceHistory[index]
                        val dateStr = dateFormat.format(Date(point.timestamp))
                        "$dateStr\n${point.sellerPrice.toLong()} ₽"
                    } else {
                        ""
                    }
                }
            ),
            indicatorSizeDp = 8f,
            clippingEnabled = true
        )
    }
}

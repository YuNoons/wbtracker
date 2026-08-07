package com.wbtracker.app.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wbtracker.app.ui.screen.addproduct.AddProductState
import com.wbtracker.app.ui.screen.addproduct.AddProductViewModel
import com.wbtracker.app.ui.theme.PriceRiseRed
import com.wbtracker.app.ui.theme.PulseGradientEnd
import com.wbtracker.app.ui.theme.PulseGradientStart
import com.wbtracker.app.util.WbArticleExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTargetPriceSheet(
    onDismissRequest: () -> Unit,
    onSaveTargetPrice: (urlOrId: String, targetPriceRub: Double, notifyOnAnyDrop: Boolean) -> Unit,
    initialUrlOrId: String = "",
    initialPriceRub: Double = 0.0,
    modifier: Modifier = Modifier,
    viewModel: AddProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var inputUrl by remember { mutableStateOf(initialUrlOrId) }
    var targetPriceText by remember { mutableStateOf(if (initialPriceRub > 0) initialPriceRub.toInt().toString() else "") }
    var notifyOnAnyDrop by remember { mutableStateOf(true) }
    var selectedDiscountChip by remember { mutableStateOf<Int?>(null) }
    var showScanSnackbar by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = state is AddProductState.Loading

    val discountChips = listOf(10, 20, 30, 50)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state) {
        when (val s = state) {
            is AddProductState.Success -> {
                val priceVal = targetPriceText.toDoubleOrNull() ?: 0.0
                onSaveTargetPrice(inputUrl, priceVal, notifyOnAnyDrop)
                Toast.makeText(context, "Товар успешно добавлен в отслеживание!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onDismissRequest()
            }
            is AddProductState.Error -> {
                localError = s.message
            }
            else -> {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetState()
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(PulseGradientStart, PulseGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Настройка целевой цены",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Установите желаемую цену или % скидки",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Error Banner if present
            AnimatedVisibility(visible = localError != null) {
                if (localError != null) {
                    Surface(
                        color = PriceRiseRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = PriceRiseRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = localError!!,
                                color = PriceRiseRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // URL or Article Input
            OutlinedTextField(
                value = inputUrl,
                onValueChange = {
                    inputUrl = it
                    localError = null
                },
                label = { Text("Ссылка на WB или артикул") },
                placeholder = { Text("Например: 12345678 или wb.ru...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Scan Barcode Button
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showScanSnackbar = true
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Сканировать штрих-код",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Сканировать штрих-код",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (showScanSnackbar) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Камера готова к сканированию штрих-кода WB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Discount Chips Row
            Text(
                text = "Быстрый выбор скидки:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                discountChips.forEach { discountPct ->
                    val isSelected = selectedDiscountChip == discountPct
                    val chipBg = if (isSelected) PulseGradientStart else MaterialTheme.colorScheme.surfaceVariant
                    val chipTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) PulseGradientStart else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isLoading) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDiscountChip = if (isSelected) null else discountPct
                                if (initialPriceRub > 0) {
                                    val calc = initialPriceRub * (1.0 - (discountPct / 100.0))
                                    targetPriceText = calc.toInt().toString()
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-$discountPct%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipTextColor
                        )
                    }
                }
            }

            // Target Price Input
            OutlinedTextField(
                value = targetPriceText,
                onValueChange = {
                    targetPriceText = it
                    selectedDiscountChip = null
                },
                label = { Text("Желаемая цена (₽)") },
                placeholder = { Text("1 990") },
                suffix = { Text("₽", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Switch: Notify on any price drop
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = PulseGradientStart,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Уведомлять при любом падении",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = notifyOnAnyDrop,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        notifyOnAnyDrop = it
                    },
                    enabled = !isLoading,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PulseGradientStart
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Save / Loading Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    localError = null
                    val extracted = WbArticleExtractor.extractArticleId(inputUrl)
                    if (extracted == null || extracted <= 0L) {
                        localError = "Введите корректную ссылку WB или артикул товара"
                        return@Button
                    }
                    viewModel.addProductByInput(inputUrl)
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PulseGradientStart, PulseGradientEnd)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Загружаем товар с Wildberries...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Text(
                            text = "Сохранить таргет",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

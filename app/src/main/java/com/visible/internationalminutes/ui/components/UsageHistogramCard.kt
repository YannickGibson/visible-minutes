package com.visible.internationalminutes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.visible.internationalminutes.model.CycleDailyUsage
import com.visible.internationalminutes.model.DailyUsage
import com.visible.internationalminutes.model.MonthlyUsage
import com.visible.internationalminutes.ui.theme.VisibleAmber
import com.visible.internationalminutes.ui.theme.VisibleBlue
import com.visible.internationalminutes.ui.theme.VisibleCyan
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
fun UsageHistogramCard(
    dailyUsages: List<DailyUsage>,
    cycleDailyUsages: List<CycleDailyUsage> = emptyList(),
    monthlyUsages: List<MonthlyUsage> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Daily by Billing Cycle Slider, 1 = Monthly Overview
    var selectedDaily by remember { mutableStateOf<DailyUsage?>(null) }
    var selectedMonthly by remember { mutableStateOf<MonthlyUsage?>(null) }

    // Discard any older historical billing cycles with zero counted minutes prior to the first active cycle
    val safeCycles = remember(cycleDailyUsages, dailyUsages) {
        if (cycleDailyUsages.isNotEmpty()) {
            val firstActive = cycleDailyUsages.indexOfFirst { it.totalCountedMinutes > 0 }
            if (firstActive >= 0) {
                cycleDailyUsages.subList(firstActive, cycleDailyUsages.size)
            } else {
                listOf(cycleDailyUsages.last())
            }
        } else {
            listOf(
                CycleDailyUsage(
                    billingCycle = com.visible.internationalminutes.model.BillingCycle.calculate(1),
                    cycleLabel = "Current Billing Cycle",
                    formattedDateRange = "Current Cycle",
                    dailyUsages = dailyUsages,
                    totalCountedMinutes = dailyUsages.sumOf { it.countedMinutes },
                    totalUnlimitedMinutes = 0,
                    totalCallsCount = dailyUsages.sumOf { it.callsCount },
                    isCurrentCycle = true
                )
            )
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val initialIndex = (safeCycles.size - 1).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { safeCycles.size }
    )

    // Snap to the latest/current cycle when data loads
    LaunchedEffect(safeCycles.size) {
        if (safeCycles.isNotEmpty() && pagerState.currentPage != safeCycles.size - 1) {
            pagerState.scrollToPage(safeCycles.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row with Title and Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = VisibleBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "International Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Legend
                LegendIndicator(color = VisibleBlue, label = "500m Pool")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily Slider vs Monthly Overview Tabs
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VisibleBlue,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            selectedMonthly = null
                        },
                        text = {
                            Text(
                                text = "Daily Cycle Slider",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTab == 0) VisibleBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            selectedDaily = null
                        },
                        text = {
                            Text(
                                text = "Monthly History",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTab == 1) VisibleBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Cycle Navigation Slider Bar with Chevrons and Date Indicator
                val currentCycleItem = safeCycles.getOrNull(pagerState.currentPage) ?: safeCycles.last()

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Cycle",
                                tint = if (pagerState.currentPage > 0) VisibleBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentCycleItem.formattedDateRange,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (currentCycleItem.isCurrentCycle) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = VisibleBlue.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Current",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VisibleBlue
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${currentCycleItem.totalCountedMinutes} of 500 minutes used",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < safeCycles.size - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < safeCycles.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Cycle",
                                tint = if (pagerState.currentPage < safeCycles.size - 1) VisibleBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fixed-height Pre-allocated Tooltip Slot (prevents layout jumping/pushing down)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (selectedDaily != null) {
                            val day = selectedDaily!!
                            val todayTag = if (day.isToday) " • Today" else ""
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${day.dayLabel} (${day.dayOfWeekLabel}$todayTag)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${day.countedMinutes} min counted (${day.callsCount} calls)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Tap any bar to inspect daily usage details",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Pager for Snapping through Billing Cycles
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { pageIndex ->
                    val cycleItem = safeCycles[pageIndex]
                    DailyFitHistogram(
                        dailyUsages = cycleItem.dailyUsages,
                        selectedDay = selectedDaily,
                        onSelectDay = { selectedDaily = it }
                    )
                }

                // Page indicator dots (only if multiple active cycles exist)
                if (safeCycles.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        safeCycles.indices.forEach { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isSelected) 8.dp else 5.dp)
                                    .background(
                                        color = if (isSelected) VisibleBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            } else {
                // Monthly Overview Tab Content
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (selectedMonthly != null) {
                            val m = selectedMonthly!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${m.cycleLabel} (${m.shortDateRange})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${m.countedMinutes} of 500 min used",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Tap any monthly bar to inspect cycle details",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                MonthlyFitHistogram(
                    monthlyUsages = monthlyUsages,
                    selectedMonthly = selectedMonthly,
                    onSelectMonthly = { selectedMonthly = it }
                )
            }
        }
    }
}

/**
 * Daily Histogram chart that fits the entire month across the screen with Y-axis markers.
 */
@Composable
private fun DailyFitHistogram(
    dailyUsages: List<DailyUsage>,
    selectedDay: DailyUsage?,
    onSelectDay: (DailyUsage) -> Unit
) {
    if (dailyUsages.isEmpty()) {
        EmptyChartPlaceholder(message = "No international calls recorded for this billing cycle")
        return
    }

    val maxMinutesRaw = dailyUsages.maxOfOrNull { it.countedMinutes } ?: 0
    val maxY = when {
        maxMinutesRaw <= 10 -> 10
        maxMinutesRaw <= 20 -> 20
        maxMinutesRaw <= 30 -> 30
        maxMinutesRaw <= 60 -> 60
        else -> (ceil(maxMinutesRaw / 30.0) * 30).toInt()
    }

    val yTicks = listOf(maxY, (maxY * 2) / 3, maxY / 3, 0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
    ) {
        // Y-Axis Labels Column
        Column(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yTicks.forEach { tick ->
                Text(
                    text = "${tick}m",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Chart Canvas + X-Axis Date Labels fitting full available width
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val count = dailyUsages.size.coerceAtLeast(1)
            val barSpacingRatio = 0.35f
            val itemWidthPx = totalWidthPx / count.toFloat()
            val barWidthPx = itemWidthPx * (1f - barSpacingRatio)
            val barSpacingPx = itemWidthPx * barSpacingRatio

            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

            Column(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .pointerInput(dailyUsages) {
                            detectTapGestures { offset ->
                                val idx = (offset.x / itemWidthPx).toInt()
                                if (idx in dailyUsages.indices) {
                                    onSelectDay(dailyUsages[idx])
                                }
                            }
                        }
                ) {
                    val canvasH = size.height
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    // Draw Y-axis horizontal dashed grid lines
                    yTicks.forEachIndexed { i, _ ->
                        val y = (canvasH * i) / (yTicks.size - 1)
                        drawLine(
                            color = surfaceVariant.copy(alpha = 0.8f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (i < yTicks.size - 1) dashEffect else null
                        )
                    }

                    // Draw quota bars for all days of the month
                    dailyUsages.forEachIndexed { i, day ->
                        val x = i * itemWidthPx + (barSpacingPx / 2f)
                        val isSelected = selectedDay == day

                        val countedH = (day.countedMinutes.toFloat() / maxY) * canvasH

                        // Background pillar slot (exact same subtle height for all days)
                        drawRoundRect(
                            color = if (isSelected) surfaceVariant.copy(alpha = 0.85f) else surfaceVariant.copy(alpha = 0.3f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidthPx, canvasH),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )

                        // Filled usage bar: Distinct Indigo Blue for Today, soft Slate Blue for past days
                        if (countedH > 0f) {
                            val countedY = canvasH - countedH
                            val barColor = if (day.isToday) {
                                Color(0xFF4F46E5) // Vibrant Indigo Blue (distinct and punchy for Today)
                            } else {
                                Color(0xFF7E9FE8) // Soft Slate Blue for past historical days
                            }
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, countedY),
                                size = Size(barWidthPx, countedH),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }
                }

                // X-Axis Date Range Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dailyUsages.firstOrNull()?.dayLabel ?: "Start",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dailyUsages.size > 14) {
                        Text(
                            text = dailyUsages[dailyUsages.size / 2].dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dailyUsages.lastOrNull()?.dayLabel ?: "End",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Monthly Historical Histogram chart showing active billing cycles
 * with a fixed 600m Y-axis and a 500m quota dashed reference line.
 */
@Composable
private fun MonthlyFitHistogram(
    monthlyUsages: List<MonthlyUsage>,
    selectedMonthly: MonthlyUsage?,
    onSelectMonthly: (MonthlyUsage) -> Unit
) {
    // Discard all older months with zero counted minutes prior to the first active month
    val filteredMonthlyUsages = remember(monthlyUsages) {
        if (monthlyUsages.isEmpty()) return@remember emptyList()
        val firstActiveIndex = monthlyUsages.indexOfFirst { it.countedMinutes > 0 }
        if (firstActiveIndex >= 0) {
            monthlyUsages.subList(firstActiveIndex, monthlyUsages.size)
        } else {
            // If all months have 0 calls, show only the current cycle
            listOf(monthlyUsages.last())
        }
    }

    if (filteredMonthlyUsages.isEmpty()) {
        EmptyChartPlaceholder(message = "No monthly history recorded yet")
        return
    }

    val maxMinsRaw = filteredMonthlyUsages.maxOfOrNull { it.countedMinutes } ?: 0
    // Fix Y-axis to 600m (or dynamically expand if usage ever exceeds 600m)
    val maxY = if (maxMinsRaw > 600) ((ceil(maxMinsRaw / 100.0)) * 100).toInt() else 600

    val yTicks = listOf(maxY, 400, 200, 0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
    ) {
        // Y-Axis Labels
        Column(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            yTicks.forEach { tick ->
                Text(
                    text = "${tick}m",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Monthly Bars Canvas + Month Labels
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val density = LocalDensity.current
            val totalWidthPx = constraints.maxWidth.toFloat()
            val count = filteredMonthlyUsages.size.coerceAtLeast(1)
            val itemWidthPx = totalWidthPx / count.toFloat()
            val maxBarWidthPx = with(density) { 46.dp.toPx() }
            val barWidthPx = (itemWidthPx * 0.45f).coerceAtMost(maxBarWidthPx)

            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

            Column(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .pointerInput(filteredMonthlyUsages) {
                            detectTapGestures { offset ->
                                val idx = (offset.x / itemWidthPx).toInt()
                                if (idx in filteredMonthlyUsages.indices) {
                                    onSelectMonthly(filteredMonthlyUsages[idx])
                                }
                            }
                        }
                ) {
                    val canvasH = size.height
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    // Y-axis grid lines (for standard ticks)
                    yTicks.forEachIndexed { i, _ ->
                        val y = (canvasH * i) / (yTicks.size - 1)
                        drawLine(
                            color = surfaceVariant.copy(alpha = 0.7f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (i < yTicks.size - 1) dashEffect else null
                        )
                    }

                    // 500m Quota Reference Guideline (- - - - line)
                    if (maxY >= 500) {
                        val y500 = canvasH * (1f - (500f / maxY.toFloat()))
                        val limitDashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        drawLine(
                            color = VisibleAmber,
                            start = Offset(0f, y500),
                            end = Offset(size.width, y500),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = limitDashEffect
                        )
                    }

                    // Monthly Bars (Counted 500m Quota Only)
                    filteredMonthlyUsages.forEachIndexed { i, m ->
                        val centerX = i * itemWidthPx + (itemWidthPx / 2f)
                        val x = centerX - (barWidthPx / 2f)
                        val isSelected = selectedMonthly == m

                        val countedH = (m.countedMinutes.toFloat() / maxY) * canvasH

                        // Background slot
                        drawRoundRect(
                            color = if (isSelected) surfaceVariant.copy(alpha = 0.9f) else surfaceVariant.copy(alpha = 0.35f),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidthPx, canvasH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Counted (Visible Blue)
                        if (countedH > 0f) {
                            val countedY = canvasH - countedH
                            drawRoundRect(
                                color = VisibleBlue,
                                topLeft = Offset(x, countedY),
                                size = Size(barWidthPx, countedH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                // Month Labels Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    filteredMonthlyUsages.forEach { m ->
                        Text(
                            text = m.cycleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (m.isCurrentCycle) FontWeight.Bold else FontWeight.Normal,
                            color = if (m.isCurrentCycle) VisibleBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

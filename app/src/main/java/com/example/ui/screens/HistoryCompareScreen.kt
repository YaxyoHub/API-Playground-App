package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RequestHistoryEntity
import com.example.ui.ApiPlaygroundViewModel
import com.example.ui.components.FormattedJsonViewer
import com.example.ui.components.MethodBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.localization.AppStrings
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryCompareScreen(
    viewModel: ApiPlaygroundViewModel,
    onRequestLoaded: () -> Unit,
    strings: AppStrings
) {
    val historyList by viewModel.historyList.collectAsState()
    val compareA by viewModel.compareItemA.collectAsState()
    val compareB by viewModel.compareItemB.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = History, 1 = Compare
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Tab Selector: History vs Side-by-Side Compare
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CyanGlow,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "${strings.requestHistoryTab} (${historyList.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) CyanGlow else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.testTag("tab_history_list")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    val indicator = if (compareA != null && compareB != null) "●" else ""
                    Text(
                        "${strings.compareTab} $indicator",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) CyanGlow else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.testTag("tab_history_compare")
            )
        }

        if (selectedTab == 0) {
            // HISTORY LIST TAB
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.recentExecutions,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (historyList.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseDanger)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.clearAll, fontSize = 12.sp, color = RoseDanger)
                    }
                }
            }

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Text(strings.noHistory, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(strings.noHistorySubtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyList, key = { it.id }) { item ->
                        val isA = compareA?.id == item.id
                        val isB = compareB?.id == item.id

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (isA || isB) 1.5.dp else 1.dp,
                                    color = if (isA) CyanGlow else if (isB) VioletAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MethodBadge(method = item.method, isSmall = true)
                                    StatusBadge(statusCode = item.statusCode, statusText = item.statusText)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "${item.durationMs} ms",
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = if (item.durationMs < 300) EmeraldSuccess else if (item.durationMs < 1000) AmberWarning else RoseDanger
                                    )
                                    Text(
                                        timeFormat.format(Date(item.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // URL
                            Text(
                                text = item.url,
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )

                            // Action buttons: Load into builder, Set as Compare A / B
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.loadHistoryIntoBuilder(item)
                                        onRequestLoaded()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("load_history_item_${item.id}")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanGlow)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.openInBuilder, fontSize = 11.sp, color = CyanGlow)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = isA,
                                        onClick = { viewModel.setCompareItemA(if (isA) null else item) },
                                        label = { Text(if (isA) "✓ Compare A" else strings.setCompareA, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = CyanGlow
                                        ),
                                        modifier = Modifier.testTag("compare_a_chip_${item.id}")
                                    )

                                    FilterChip(
                                        selected = isB,
                                        onClick = { viewModel.setCompareItemB(if (isB) null else item) },
                                        label = { Text(if (isB) "✓ Compare B" else strings.setCompareB, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VioletAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = VioletAccent
                                        ),
                                        modifier = Modifier.testTag("compare_b_chip_${item.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // SIDE-BY-SIDE COMPARE TAB
            val a = compareA
            val b = compareB

            if (a == null || b == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(54.dp), tint = VioletAccent)
                        Text(strings.compareHint, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (a != null) CyanGlow else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    if (a != null) "✓ Slot A: ${a.method} ${a.statusCode}" else "Slot A: (None)",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp),
                                    color = if (a != null) CyanGlow else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (b != null) VioletAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    if (b != null) "✓ Slot B: ${b.method} ${b.statusCode}" else "Slot B: (None)",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp),
                                    color = if (b != null) VioletAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Diff Metrics Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(strings.diffOverview, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

                            // Status Comparison
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.statusCode, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(a.statusCode, a.statusText)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    StatusBadge(b.statusCode, b.statusText)
                                }
                            }

                            // Latency Delta
                            val latencyDeltaMs = b.durationMs - a.durationMs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.latencyDelta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val latencyColor = if (latencyDeltaMs < 0) EmeraldSuccess else if (latencyDeltaMs > 0) RoseDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                val latencySign = if (latencyDeltaMs > 0) "+$latencyDeltaMs ms" else "$latencyDeltaMs ms"
                                Text(
                                    "${a.durationMs}ms → ${b.durationMs}ms ($latencySign)",
                                    fontSize = 12.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = latencyColor
                                )
                            }

                            // Payload Size Delta
                            val sizeDelta = b.responseBody.length - a.responseBody.length
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.payloadDelta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val sign = if (sizeDelta > 0) "+$sizeDelta chars" else "$sizeDelta chars"
                                Text(
                                    sign,
                                    fontSize = 12.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = if (sizeDelta == 0) EmeraldSuccess else AmberWarning
                                )
                            }
                        }
                    }

                    // Side-by-Side Bodies
                    item {
                        Text(strings.responsePayloads, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = CyanAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    "${strings.baselineOutput} (${a.method} ${a.url})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanGlow,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            FormattedJsonViewer(rawText = a.responseBody.ifBlank { "(Empty)" }, maxLines = 16)
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = VioletAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    "${strings.targetOutput} (${b.method} ${b.url})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VioletAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            FormattedJsonViewer(rawText = b.responseBody.ifBlank { "(Empty)" }, maxLines = 16)
                        }
                    }
                }
            }
        }
    }
}

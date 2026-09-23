package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.ApiPlaygroundViewModel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.util.localization.AppStrings

@Composable
fun HealthMonitorScreen(
    viewModel: ApiPlaygroundViewModel,
    strings: AppStrings
) {
    val endpoints by viewModel.monitoredEndpoints.collectAsState()
    val isPingingAll by viewModel.isPingingAll.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var serviceName by remember { mutableStateOf("") }
    var serviceUrl by remember { mutableStateOf("") }
    var expectedCode by remember { mutableStateOf("200") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    serviceName = ""
                    serviceUrl = ""
                    expectedCode = "200"
                    showAddDialog = true
                },
                containerColor = CyanAccent,
                contentColor = Slate950,
                modifier = Modifier.testTag("fab_add_monitor_endpoint")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addEndpointTitle)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Status Page Header with Live Pulse Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        strings.statusPageTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val operationalCount = endpoints.count { it.lastStatusCode == it.expectedStatus }
                    Text(
                        text = if (endpoints.isNotEmpty() && operationalCount == endpoints.size) strings.allOperational else "$operationalCount / ${endpoints.size} ${strings.endpointsOperational}",
                        fontSize = 12.sp,
                        color = if (operationalCount == endpoints.size && endpoints.isNotEmpty()) EmeraldSuccess else AmberWarning
                    )
                }

                Button(
                    onClick = { viewModel.pingAllMonitored() },
                    enabled = !isPingingAll && endpoints.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("ping_all_button")
                ) {
                    if (isPingingAll) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Slate950)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(strings.pingAll, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (endpoints.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Text(strings.noMonitored, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(strings.noMonitoredSubtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(endpoints, key = { it.id }) { endpoint ->
                        val isHealthy = endpoint.lastStatusCode == endpoint.expectedStatus
                        val hasRun = endpoint.lastCheckedTimestamp != null

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (!hasRun) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f) else if (isHealthy) EmeraldSuccess.copy(alpha = 0.35f) else RoseDanger.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(
                                                if (!hasRun) Slate400 else if (isHealthy) EmeraldSuccess else RoseDanger
                                            )
                                    )
                                    Text(
                                        text = endpoint.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (hasRun) {
                                        StatusBadge(statusCode = endpoint.lastStatusCode ?: 0, statusText = if (isHealthy) "Healthy" else "Degraded")
                                    } else {
                                        Text("Pending", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = { viewModel.pingSingleEndpoint(endpoint) },
                                        modifier = Modifier.size(30.dp).testTag("ping_single_${endpoint.id}")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Ping", tint = CyanGlow, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteMonitored(endpoint.id) },
                                        modifier = Modifier.size(30.dp).testTag("delete_monitor_${endpoint.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Text(
                                text = endpoint.url,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )

                            if (hasRun) {
                                val latency = endpoint.lastLatencyMs ?: 0L
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Latency: $latency ms",
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = if (latency < 300) EmeraldSuccess else if (latency < 800) AmberWarning else RoseDanger
                                    )
                                    Text(
                                        "Expected HTTP ${endpoint.expectedStatus}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Monitored Endpoint Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addEndpointTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = serviceName,
                        onValueChange = { serviceName = it },
                        label = { Text(strings.serviceNameLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("service_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = serviceUrl,
                        onValueChange = { serviceUrl = it },
                        label = { Text(strings.healthCheckUrlLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("service_url_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = expectedCode,
                        onValueChange = { expectedCode = it },
                        label = { Text(strings.expectedStatusLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("expected_status_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = expectedCode.toIntOrNull() ?: 200
                        if (serviceName.isNotBlank() && serviceUrl.isNotBlank()) {
                            viewModel.addMonitoredEndpoint(serviceName, serviceUrl, code)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    modifier = Modifier.testTag("confirm_add_endpoint")
                ) {
                    Text(strings.addEndpointBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

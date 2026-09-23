package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CollectionEntity
import com.example.data.model.AuthType
import com.example.data.model.HttpMethod
import com.example.data.model.RequestBodyType
import com.example.ui.ApiPlaygroundViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.localization.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestBuilderScreen(
    viewModel: ApiPlaygroundViewModel,
    collections: List<CollectionEntity>,
    onNavigateToEnvironments: () -> Unit,
    strings: AppStrings
) {
    val state by viewModel.builderState.collectAsState()
    val response = state.latestResponse
    val selectedEnv by viewModel.selectedEnvironment.collectAsState()
    val scrollState = rememberScrollState()

    var showMethodDropdown by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var saveReqName by remember { mutableStateOf("") }
    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    var snapshotTitle by remember { mutableStateOf("") }

    val methods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Top Header: Active Environment Pill + Save to Collection button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Environment Selector Pill
            Surface(
                onClick = onNavigateToEnvironments,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("env_selector_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selectedEnv != null) EmeraldSuccess else Slate400)
                    )
                    Text(
                        text = selectedEnv?.name ?: strings.noEnvironment,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Env",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Save Request button
            OutlinedButton(
                onClick = {
                    saveReqName = state.requestName
                    selectedCollectionId = collections.firstOrNull()?.id
                    showSaveDialog = true
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanGlow),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("save_request_button")
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.save, fontSize = 12.sp)
            }
        }

        // Request Name
        Text(
            text = state.requestName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // URL & Method Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Method Dropdown Button
            Box {
                Surface(
                    onClick = { showMethodDropdown = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.testTag("method_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MethodBadge(method = state.method)
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Method",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMethodDropdown,
                    onDismissRequest = { showMethodDropdown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    methods.forEach { m ->
                        DropdownMenuItem(
                            text = { MethodBadge(method = m) },
                            onClick = {
                                viewModel.updateMethod(m)
                                showMethodDropdown = false
                            },
                            modifier = Modifier.testTag("method_option_$m")
                        )
                    }
                }
            }

            // URL input field
            OutlinedTextField(
                value = state.url,
                onValueChange = { viewModel.updateUrl(it) },
                placeholder = {
                    Text(
                        "{{baseUrl}}/endpoint",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("url_input_field"),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 13.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = CyanGlow
                )
            )

            // Send Button
            Button(
                onClick = { viewModel.sendRequest() },
                enabled = !state.isLoading,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = Slate950
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("send_request_button")
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Slate950
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.send,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Request Configuration Tabs (Params, Headers, Body, Auth)
        val tabTitles = listOf(
            "${strings.paramsTab} (${state.params.size})",
            "${strings.headersTab} (${state.headers.size})",
            "${strings.bodyTab} (${state.bodyType.name})",
            "${strings.authTab} (${state.authType.name})"
        )

        SecondaryTabRow(
            selectedTabIndex = state.selectedReqTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CyanGlow,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedReqTab == index,
                    onClick = { viewModel.selectReqTab(index) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (state.selectedReqTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (state.selectedReqTab == index) CyanGlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("req_tab_$index")
                )
            }
        }

        // Tab Content Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            when (state.selectedReqTab) {
                0 -> {
                    KeyValueEditor(
                        pairs = state.params,
                        onPairsChanged = { viewModel.updateParams(it) },
                        keyPlaceholder = "Param name",
                        valuePlaceholder = "Param value",
                        emptyMessage = "No URL query parameters added."
                    )
                }
                1 -> {
                    KeyValueEditor(
                        pairs = state.headers,
                        onPairsChanged = { viewModel.updateHeaders(it) },
                        keyPlaceholder = "Header name",
                        valuePlaceholder = "Header value",
                        emptyMessage = "No custom headers added."
                    )
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RequestBodyType.values().forEach { bType ->
                                    FilterChip(
                                        selected = state.bodyType == bType,
                                        onClick = { viewModel.updateBodyType(bType) },
                                        label = { Text(bType.name, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VioletAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = VioletAccent
                                        ),
                                        modifier = Modifier.testTag("body_type_${bType.name}")
                                    )
                                }
                            }

                            if (state.bodyType == RequestBodyType.JSON) {
                                TextButton(
                                    onClick = { viewModel.formatJsonBody() },
                                    modifier = Modifier.testTag("format_json_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.prettify, fontSize = 11.sp, color = CyanGlow)
                                }
                            }
                        }

                        if (state.bodyType == RequestBodyType.NONE) {
                            Text(
                                text = strings.noBody,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            OutlinedTextField(
                                value = state.bodyContent,
                                onValueChange = { viewModel.updateBodyContent(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 260.dp)
                                    .testTag("body_content_editor"),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 12.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    lineHeight = 18.sp
                                ),
                                placeholder = { Text("Enter request body...", fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanAccent,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        }
                    }
                }
                3 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AuthType.values().forEach { aType ->
                                FilterChip(
                                    selected = state.authType == aType,
                                    onClick = { viewModel.updateAuthType(aType) },
                                    label = { Text(aType.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberWarning.copy(alpha = 0.2f),
                                        selectedLabelColor = AmberWarning
                                    ),
                                    modifier = Modifier.testTag("auth_type_${aType.name}")
                                )
                            }
                        }

                        when (state.authType) {
                            AuthType.NONE -> {
                                Text("No authentication attached to this request.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                            AuthType.BEARER -> {
                                OutlinedTextField(
                                    value = state.authConfig.bearerToken,
                                    onValueChange = { viewModel.updateAuthConfig(state.authConfig.copy(bearerToken = it)) },
                                    label = { Text("Bearer Token (supports {{token}})") },
                                    modifier = Modifier.fillMaxWidth().testTag("bearer_token_input"),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp)
                                )
                            }
                            AuthType.API_KEY -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = state.authConfig.apiKeyName,
                                        onValueChange = { viewModel.updateAuthConfig(state.authConfig.copy(apiKeyName = it)) },
                                        label = { Text("Key Name (e.g. x-api-key)") },
                                        modifier = Modifier.fillMaxWidth().testTag("api_key_name_input"),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = state.authConfig.apiKeyValue,
                                        onValueChange = { viewModel.updateAuthConfig(state.authConfig.copy(apiKeyValue = it)) },
                                        label = { Text("Key Value (supports {{apiKey}})") },
                                        modifier = Modifier.fillMaxWidth().testTag("api_key_value_input"),
                                        singleLine = true
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Add To: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        listOf("HEADER", "QUERY").forEach { mode ->
                                            FilterChip(
                                                selected = state.authConfig.apiKeyAddTo == mode,
                                                onClick = { viewModel.updateAuthConfig(state.authConfig.copy(apiKeyAddTo = mode)) },
                                                label = { Text(mode, fontSize = 11.sp) },
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            AuthType.BASIC -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = state.authConfig.basicUsername,
                                        onValueChange = { viewModel.updateAuthConfig(state.authConfig.copy(basicUsername = it)) },
                                        label = { Text("Username") },
                                        modifier = Modifier.fillMaxWidth().testTag("basic_user_input"),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = state.authConfig.basicPassword,
                                        onValueChange = { viewModel.updateAuthConfig(state.authConfig.copy(basicPassword = it)) },
                                        label = { Text("Password") },
                                        modifier = Modifier.fillMaxWidth().testTag("basic_pass_input"),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Response Section
        if (response != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Response Header bar: Status + Duration + Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        statusCode = response.statusCode,
                        statusText = response.statusText
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetricChip(
                            label = strings.timeLabel,
                            value = "${response.durationMs} ms",
                            icon = Icons.Default.Timer
                        )
                        val sizeFormatted = if (response.sizeBytes > 1024) "${response.sizeBytes / 1024} KB" else "${response.sizeBytes} B"
                        MetricChip(
                            label = strings.sizeLabel,
                            value = sizeFormatted,
                            icon = Icons.Default.DataUsage
                        )
                    }
                }

                // Response Tabs: Body, Headers, Snapshot action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.selectedResTab == 0,
                            onClick = { viewModel.selectResTab(0) },
                            label = { Text(strings.bodyTab, fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = state.selectedResTab == 1,
                            onClick = { viewModel.selectResTab(1) },
                            label = { Text("${strings.headersTab} (${response.headers.size})", fontSize = 12.sp) }
                        )
                    }

                    // Save Snapshot button
                    TextButton(
                        onClick = {
                            snapshotTitle = "Snapshot: ${state.method} ${response.statusCode}"
                            showSnapshotDialog = true
                        },
                        modifier = Modifier.testTag("save_snapshot_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanGlow)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.snapshot, fontSize = 12.sp, color = CyanGlow)
                    }
                }

                if (state.selectedResTab == 0) {
                    if (response.errorDetails != null && response.statusCode == 0) {
                        Surface(
                            color = RoseDanger.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoseDanger.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(strings.connectionFailed, color = RoseDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(response.errorDetails, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily)
                            }
                        }
                    } else {
                        FormattedJsonViewer(rawText = response.body.ifBlank { "(Empty response)" })
                    }
                } else {
                    // Response Headers Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (response.headers.isEmpty()) {
                            Text("No headers returned.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        } else {
                            response.headers.forEach { (name, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, color = CyanGlow, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily, modifier = Modifier.weight(1f))
                                    Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily, modifier = Modifier.weight(1.5f))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Save to Collection Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(strings.saveRequestTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = saveReqName,
                        onValueChange = { saveReqName = it },
                        label = { Text(strings.requestNameLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_request_name_input"),
                        singleLine = true
                    )

                    Text(strings.saveToCollection, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (collections.isEmpty()) {
                        Text("No collections yet. A default one will be created.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        collections.forEach { col ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCollectionId = col.id }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedCollectionId == col.id,
                                    onClick = { selectedCollectionId = col.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(col.name, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentRequest(selectedCollectionId, saveReqName)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    modifier = Modifier.testTag("confirm_save_request_button")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Snapshot Dialog
    if (showSnapshotDialog && response != null) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = false },
            title = { Text(strings.snapshotTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.snapshotSubtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = snapshotTitle,
                        onValueChange = { snapshotTitle = it },
                        label = { Text("Snapshot Title") },
                        modifier = Modifier.fillMaxWidth().testTag("snapshot_title_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSnapshot(snapshotTitle, response)
                        showSnapshotDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    modifier = Modifier.testTag("confirm_save_snapshot_button")
                ) {
                    Text(strings.snapshot)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSnapshotDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

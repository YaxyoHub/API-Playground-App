package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CollectionEntity
import com.example.data.local.RequestEntity
import com.example.ui.ApiPlaygroundViewModel
import com.example.ui.components.MethodBadge
import com.example.ui.theme.*
import com.example.util.localization.AppStrings
import kotlinx.coroutines.launch

@Composable
fun CollectionsScreen(
    viewModel: ApiPlaygroundViewModel,
    onRequestSelected: () -> Unit,
    strings: AppStrings
) {
    val collections by viewModel.collections.collectAsState()
    val allRequests by viewModel.allRequests.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonContent by remember { mutableStateOf("") }
    var exportCollectionName by remember { mutableStateOf("") }

    var newColName by remember { mutableStateOf("") }
    var newColDesc by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }

    // Map of expanded collection IDs
    val expandedCollections = remember { mutableStateMapOf<Long, Boolean>() }

    // Initialize all expanded by default
    LaunchedEffect(collections) {
        collections.forEach { col ->
            if (!expandedCollections.containsKey(col.id)) {
                expandedCollections[col.id] = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newColName = ""
                    newColDesc = ""
                    showCreateDialog = true
                },
                containerColor = CyanAccent,
                contentColor = Slate950,
                modifier = Modifier.testTag("fab_add_collection")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.newCollection)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Action Header: Import Postman Collection button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.collectionsTitle} (${collections.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Button(
                    onClick = {
                        importJsonText = ""
                        showImportDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = CyanGlow
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("import_postman_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.importPostman, fontSize = 12.sp)
                }
            }

            if (collections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            strings.noCollections,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            strings.noCollectionsSubtitle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(collections, key = { it.id }) { collection ->
                        val requests = allRequests.filter { it.collectionId == collection.id }
                        val isExpanded = expandedCollections[collection.id] ?: true

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            // Collection Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCollections[collection.id] = !isExpanded
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = VioletAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = collection.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (collection.description.isNotBlank()) {
                                            Text(
                                                text = collection.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${requests.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Export Postman button
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                exportCollectionName = collection.name
                                                exportJsonContent = viewModel.exportPostmanCollection(collection.id)
                                                showExportDialog = true
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("export_col_${collection.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = strings.exportPostman,
                                            tint = CyanGlow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Delete Collection button
                                    IconButton(
                                        onClick = { viewModel.deleteCollection(collection.id) },
                                        modifier = Modifier.size(32.dp).testTag("delete_col_${collection.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Collection",
                                            tint = RoseDanger.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Expandable Requests list
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (requests.isEmpty()) {
                                        Text(
                                            text = "No saved endpoints in this collection.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    } else {
                                        requests.forEach { req ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        viewModel.loadRequestIntoBuilder(req)
                                                        onRequestSelected()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                                    .testTag("collection_request_item_${req.id}"),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    MethodBadge(method = req.method, isSmall = true)
                                                    Column {
                                                        Text(
                                                            text = req.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = req.url,
                                                            fontSize = 11.sp,
                                                            fontFamily = JetBrainsMonoFontFamily,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteRequest(req.id) },
                                                    modifier = Modifier.size(28.dp).testTag("delete_req_${req.id}")
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Collection Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(strings.newCollection, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newColName,
                        onValueChange = { newColName = it },
                        label = { Text(strings.collectionNameLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("new_col_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newColDesc,
                        onValueChange = { newColDesc = it },
                        label = { Text(strings.collectionDescLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("new_col_desc_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newColName.isNotBlank()) {
                            viewModel.createCollection(newColName, newColDesc)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    modifier = Modifier.testTag("confirm_create_collection")
                ) {
                    Text(strings.create)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Import Postman Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(strings.importPostman, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste a Postman Collection JSON (v2.0 or v2.1 format) to import all requests automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            importJsonText = """
                            {
                              "info": {
                                "name": "Rick and Morty API",
                                "description": "Imported characters and locations API"
                              },
                              "item": [
                                {
                                  "name": "Get All Characters",
                                  "request": {
                                    "method": "GET",
                                    "url": "https://rickandmortyapi.com/api/character"
                                  }
                                },
                                {
                                  "name": "Get Character by ID",
                                  "request": {
                                    "method": "GET",
                                    "url": "https://rickandmortyapi.com/api/character/1"
                                  }
                                }
                              ]
                            }
                            """.trimIndent()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("load_sample_postman_button")
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.loadSamplePostman, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Postman JSON") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("postman_import_text_input"),
                        textStyle = LocalTextStyle.current.copy(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp),
                        placeholder = { Text("{ \"info\": { \"name\": \"...\" }, \"item\": [...] }", fontSize = 11.sp) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            viewModel.importPostman(importJsonText)
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text(strings.importBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Export Postman Dialog
    if (showExportDialog) {
        var copied by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("${strings.exportPostman} '$exportCollectionName'", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Standard Postman v2.1.0 JSON format for desktop/Postman import:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = exportJsonContent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportJsonContent))
                        copied = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    modifier = Modifier.testTag("copy_export_json_button")
                ) {
                    Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (copied) strings.copied else strings.copyJson)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text(strings.close) }
            }
        )
    }
}

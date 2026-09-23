package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.EnvironmentEntity
import com.example.data.model.KeyValuePair
import com.example.ui.ApiPlaygroundViewModel
import com.example.ui.components.KeyValueEditor
import com.example.ui.theme.*
import com.example.util.JsonHelper
import com.example.util.localization.AppStrings

@Composable
fun EnvironmentsScreen(
    viewModel: ApiPlaygroundViewModel,
    strings: AppStrings
) {
    val environments by viewModel.environments.collectAsState()
    val selectedEnv by viewModel.selectedEnvironment.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()

    var editingEnv by remember { mutableStateOf<EnvironmentEntity?>(null) }
    var currentVariables by remember { mutableStateOf<List<KeyValuePair>>(emptyList()) }
    var showNewEnvDialog by remember { mutableStateOf(false) }
    var newEnvName by remember { mutableStateOf("") }
    var showUnlockVaultDialog by remember { mutableStateOf(false) }
    var vaultPinInput by remember { mutableStateOf("") }

    // Keep editingEnv synced to selectedEnv on first load
    LaunchedEffect(selectedEnv) {
        if (editingEnv == null && selectedEnv != null) {
            editingEnv = selectedEnv
            currentVariables = JsonHelper.parseKeyValuePairs(selectedEnv?.variablesJson)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newEnvName = ""
                    showNewEnvDialog = true
                },
                containerColor = CyanAccent,
                contentColor = Slate950,
                modifier = Modifier.testTag("fab_add_environment")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.newEnvironmentTitle)
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

            // Header & Vault Security Lock Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isVaultUnlocked) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isVaultUnlocked) EmeraldSuccess.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                            contentDescription = "Biometric Vault",
                            tint = if (isVaultUnlocked) EmeraldSuccess else CyanGlow,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (isVaultUnlocked) strings.vaultUnlocked else strings.vaultProtected,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isVaultUnlocked) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isVaultUnlocked) strings.vaultVisibleNotice else strings.vaultLockedNotice,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isVaultUnlocked) {
                                viewModel.lockVault()
                            } else {
                                vaultPinInput = ""
                                showUnlockVaultDialog = true
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVaultUnlocked) MaterialTheme.colorScheme.surfaceVariant else CyanAccent,
                            contentColor = if (isVaultUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else Slate950
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("vault_toggle_button")
                    ) {
                        Text(if (isVaultUnlocked) strings.lock else strings.unlock, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Environment Switcher Horizontal Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.selectActiveEnv, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    environments.forEach { env ->
                        val isSelected = selectedEnv?.id == env.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectEnvironment(env.id)
                                editingEnv = env
                                currentVariables = JsonHelper.parseKeyValuePairs(env.variablesJson)
                            },
                            label = {
                                Text(
                                    text = env.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanGlow,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) CyanGlow else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("env_chip_${env.id}")
                        )
                    }
                }
            }

            // Variable Editor Card
            val current = editingEnv
            if (current != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${current.name} ${strings.envVariablesTitle}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.envUsageNotice,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Save Button
                            Button(
                                onClick = {
                                    viewModel.saveEnvironment(current.id, current.name, currentVariables)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("save_env_variables_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.save, fontSize = 12.sp)
                            }

                            if (environments.size > 1) {
                                IconButton(
                                    onClick = {
                                        viewModel.deleteEnvironment(current.id)
                                        editingEnv = environments.firstOrNull { it.id != current.id }
                                    },
                                    modifier = Modifier.size(32.dp).testTag("delete_env_${current.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            KeyValueEditor(
                                pairs = currentVariables,
                                onPairsChanged = { currentVariables = it },
                                keyPlaceholder = "Variable name (e.g. baseUrl)",
                                valuePlaceholder = "Value (e.g. https://api.io)",
                                allowSecretToggle = true,
                                emptyMessage = "No environment variables defined yet."
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select or create an environment to manage variables.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
    }

    // New Environment Dialog
    if (showNewEnvDialog) {
        AlertDialog(
            onDismissRequest = { showNewEnvDialog = false },
            title = { Text(strings.newEnvironmentTitle, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newEnvName,
                    onValueChange = { newEnvName = it },
                    label = { Text(strings.envNameLabel) },
                    modifier = Modifier.fillMaxWidth().testTag("new_env_name_input"),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEnvName.isNotBlank()) {
                            viewModel.saveEnvironment(0, newEnvName, listOf(KeyValuePair(key = "baseUrl", value = "https://api.example.com")))
                            showNewEnvDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    modifier = Modifier.testTag("confirm_create_env")
                ) {
                    Text(strings.create)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewEnvDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Unlock Vault Dialog (Biometrics / PIN simulation)
    if (showUnlockVaultDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockVaultDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyanGlow)
                    Text("Biometric Secrets Vault", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        strings.bioPrompt,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vaultPinInput,
                        onValueChange = { vaultPinInput = it },
                        label = { Text(strings.masterPinLabel) },
                        modifier = Modifier.fillMaxWidth().testTag("vault_pin_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unlockVault()
                        showUnlockVaultDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    modifier = Modifier.testTag("confirm_unlock_vault")
                ) {
                    Text(strings.authenticate)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockVaultDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KeyValuePair
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoseDanger

@Composable
fun KeyValueEditor(
    pairs: List<KeyValuePair>,
    onPairsChanged: (List<KeyValuePair>) -> Unit,
    modifier: Modifier = Modifier,
    keyPlaceholder: String = "Key",
    valuePlaceholder: String = "Value",
    allowSecretToggle: Boolean = false,
    emptyMessage: String = "No parameters added"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pairs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            pairs.forEachIndexed { index, pair ->
                var showSecret by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = pair.enabled,
                        onCheckedChange = { checked ->
                            val updated = pairs.toMutableList()
                            updated[index] = pair.copy(enabled = checked)
                            onPairsChanged(updated)
                        },
                        modifier = Modifier.size(24.dp).testTag("kv_checkbox_$index")
                    )

                    OutlinedTextField(
                        value = pair.key,
                        onValueChange = { newKey ->
                            val updated = pairs.toMutableList()
                            updated[index] = pair.copy(key = newKey)
                            onPairsChanged(updated)
                        },
                        placeholder = { Text(keyPlaceholder, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kv_key_input_$index"),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    OutlinedTextField(
                        value = pair.value,
                        onValueChange = { newVal ->
                            val updated = pairs.toMutableList()
                            updated[index] = pair.copy(value = newVal)
                            onPairsChanged(updated)
                        },
                        placeholder = { Text(valuePlaceholder, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("kv_value_input_$index"),
                        singleLine = true,
                        visualTransformation = if (pair.isSecret && !showSecret) PasswordVisualTransformation() else VisualTransformation.None,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )

                    if (allowSecretToggle) {
                        IconButton(
                            onClick = {
                                if (pair.isSecret) {
                                    showSecret = !showSecret
                                } else {
                                    val updated = pairs.toMutableList()
                                    updated[index] = pair.copy(isSecret = true)
                                    onPairsChanged(updated)
                                }
                            },
                            modifier = Modifier.size(28.dp).testTag("kv_secret_toggle_$index")
                        ) {
                            Icon(
                                imageVector = if (!pair.isSecret) Icons.Default.Lock else if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Secret Vault Lock",
                                tint = if (pair.isSecret) CyanAccent else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val updated = pairs.toMutableList()
                            updated.removeAt(index)
                            onPairsChanged(updated)
                        },
                        modifier = Modifier.size(28.dp).testTag("kv_delete_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Row",
                            tint = RoseDanger.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                val updated = pairs.toMutableList()
                updated.add(KeyValuePair())
                onPairsChanged(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_kv_row_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CyanAccent
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Parameter", fontSize = 13.sp)
        }
    }
}

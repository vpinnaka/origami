package com.origami.assistant.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToModelSetup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showTavily by remember { mutableStateOf(false) }
    var showComposio by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Model section
            SettingsSectionHeader("Model")
            ListItem(
                headlineContent = { Text("Gemma 4 E2B") },
                supportingContent = {
                    Text(
                        if (state.modelPath.isNotBlank()) "Loaded: ${state.modelPath.substringAfterLast("/")}"
                        else "Not configured"
                    )
                },
                leadingContent = { Icon(Icons.Default.Memory, null) },
                trailingContent = {
                    TextButton(onClick = onNavigateToModelSetup) { Text("Configure") }
                }
            )

            ListItem(
                headlineContent = { Text("Inference Backend") },
                supportingContent = { Text("CPU recommended for <12GB RAM devices") },
                leadingContent = { Icon(Icons.Default.Speed, null) },
                trailingContent = {
                    Row {
                        listOf("cpu", "gpu").forEach { b ->
                            FilterChip(
                                selected = state.inferenceBackend == b,
                                onClick = { viewModel.setBackend(b) },
                                label = { Text(b.uppercase()) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            )

            HorizontalDivider()
            SettingsSectionHeader("Web Search")

            ApiKeyField(
                label = "Tavily API Key",
                value = state.tavilyKey,
                onValueChange = viewModel::setTavilyKey,
                visible = showTavily,
                onToggleVisibility = { showTavily = !showTavily },
                icon = Icons.Default.Search
            )

            HorizontalDivider()
            SettingsSectionHeader("App Integrations")

            ApiKeyField(
                label = "Composio API Key",
                value = state.composioKey,
                onValueChange = viewModel::setComposioKey,
                visible = showComposio,
                onToggleVisibility = { showComposio = !showComposio },
                icon = Icons.Default.Extension
            )

            HorizontalDivider()
            SettingsSectionHeader("Memory")

            ListItem(
                headlineContent = { Text("Memory") },
                supportingContent = { Text("${state.memoryCount} stored memories") },
                leadingContent = { Icon(Icons.Default.Psychology, null) },
                trailingContent = {
                    Switch(checked = state.memoryEnabled, onCheckedChange = viewModel::setMemoryEnabled)
                }
            )

            ListItem(
                headlineContent = { Text("Context window") },
                supportingContent = { Text("${state.maxContextTokens} tokens max") },
                leadingContent = { Icon(Icons.Default.Token, null) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf(2048, 4096, 8192).forEach { t ->
                            FilterChip(
                                selected = state.maxContextTokens == t,
                                onClick = { viewModel.setMaxTokens(t) },
                                label = { Text("${t / 1024}K") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Clear All Memories", color = MaterialTheme.colorScheme.error) },
                supportingContent = { Text("Permanently delete all stored memories") },
                leadingContent = {
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickableCompat { viewModel.showClearMemoryDialog() }
            )

            HorizontalDivider()
            SettingsSectionHeader("About")
            ListItem(
                headlineContent = { Text("Origami") },
                supportingContent = { Text("v1.0.0 · Powered by Gemma 4 E2B · All computation on-device") },
                leadingContent = { Text("✦") }
            )
        }
    }

    if (state.showClearMemoryDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearMemoryDialog,
            title = { Text("Clear All Memories?") },
            text = { Text("This will permanently delete all ${state.memoryCount} stored memories. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = viewModel::clearAllMemories,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearMemoryDialog) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ListItem(
        headlineContent = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null
                        )
                    }
                }
            )
        },
        leadingContent = { Icon(icon, null) }
    )
}

// Extension to make ListItem clickable without the clickable modifier on content
private fun Modifier.clickableCompat(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))

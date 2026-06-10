package com.origami.assistant.ui.assistants

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.origami.assistant.data.db.entity.AssistantEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantsScreen(
    onNavigateToChat: (String) -> Unit,
    viewModel: AssistantsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistants") },
                actions = {
                    IconButton(onClick = viewModel::startCreating) {
                        Icon(Icons.Default.Add, "Create assistant")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isCreating) {
            CreateAssistantSheet(
                state = state,
                onNameChange = viewModel::onNameChange,
                onDescChange = viewModel::onDescChange,
                onPromptChange = viewModel::onPromptChange,
                onScheduleChange = viewModel::onScheduleChange,
                onEmojiChange = viewModel::onEmojiChange,
                onSave = viewModel::save,
                onCancel = viewModel::cancelCreating
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.assistants.isEmpty()) {
                    item {
                        EmptyAssistants(onCreateClick = viewModel::startCreating)
                    }
                }
                items(state.assistants, key = { it.id }) { assistant ->
                    AssistantCard(
                        assistant = assistant,
                        onChat = {
                            viewModel.startChat(assistant.id) { convId ->
                                onNavigateToChat(convId)
                            }
                        },
                        onRunNow = { viewModel.runNow(assistant.id) },
                        onEdit = { viewModel.startEditing(assistant) },
                        onDelete = { viewModel.delete(assistant.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantCard(
    assistant: AssistantEntity,
    onChat: () -> Unit,
    onRunNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(assistant.avatarEmoji, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(assistant.name, style = MaterialTheme.typography.titleMedium)
                    if (assistant.description.isNotBlank()) {
                        Text(
                            assistant.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Run Now") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                            onClick = { showMenu = false; onRunNow() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(assistant.scheduleType.capitalize(), style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            if (assistant.scheduleType == "manual") Icons.Default.TouchApp else Icons.Default.Schedule,
                            null, modifier = Modifier.size(12.dp)
                        )
                    }
                )
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onChat, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Chat", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun CreateAssistantSheet(
    state: AssistantsUiState,
    onNameChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onScheduleChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.editingAssistant != null) "Edit Assistant" else "New Assistant",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, "Cancel")
            }
        }

        OutlinedTextField(
            value = state.draftName,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.draftDescription,
            onValueChange = onDescChange,
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = state.draftPrompt,
            onValueChange = onPromptChange,
            label = { Text("System Prompt") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("You are a helpful assistant that…") }
        )

        Column {
            Text("Schedule", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("manual", "daily", "weekly").forEach { schedule ->
                    FilterChip(
                        selected = state.draftSchedule == schedule,
                        onClick = { onScheduleChange(schedule) },
                        label = { Text(schedule.capitalize()) }
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onSave,
            enabled = state.draftName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.editingAssistant != null) "Save Changes" else "Create Assistant")
        }
    }
}

@Composable
private fun EmptyAssistants(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🤖", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text("No assistants yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a specialized assistant with a custom persona and schedule.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onCreateClick) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Create Assistant")
        }
    }
}

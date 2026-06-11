package com.origami.assistant.ui.modelsetup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.origami.assistant.inference.ModelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSetupScreen(
    onComplete: () -> Unit,
    viewModel: ModelSetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.modelState) {
        if (state.modelState is ModelState.Ready) {
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Set Up Model") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🧠", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                "Gemma 4 E2B",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "A 2.6 GB multimodal model running entirely on your device. No data leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(32.dp))

            // Backend selection
            Text("Inference Backend", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("cpu" to "CPU (Safe)", "gpu" to "GPU (12GB+ RAM)").forEach { (key, label) ->
                    FilterChip(
                        selected = state.selectedBackend == key,
                        onClick = { viewModel.onBackendChange(key) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            // State display
            when (val ms = state.modelState) {
                is ModelState.Uninitialized -> {
                    DownloadSection(
                        isDownloaded = state.isModelDownloaded,
                        onOpenKaggle = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(GEMMA_KAGGLE_URL))
                            )
                        },
                        onLoadDefault = viewModel::loadDefault
                    )
                }
                is ModelState.Downloading -> {
                    DownloadProgress(ms.progress)
                }
                is ModelState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Loading model…", style = MaterialTheme.typography.bodyMedium)
                        Text("This may take 30–60 seconds", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                is ModelState.Error -> {
                    ErrorCard(ms.message, viewModel::dismissError)
                    Spacer(Modifier.height(16.dp))
                    DownloadSection(
                        isDownloaded = state.isModelDownloaded,
                        onOpenKaggle = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(GEMMA_KAGGLE_URL))
                            )
                        },
                        onLoadDefault = viewModel::loadDefault
                    )
                }
                is ModelState.Ready -> {
                    // Auto-navigates via LaunchedEffect
                    CircularProgressIndicator()
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Local file option
            Text("Have the model file?", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.localPathInput,
                onValueChange = viewModel::onLocalPathChange,
                label = { Text("Path to .task file") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("/sdcard/gemma4-e2b.task") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::loadFromPath,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.localPathInput.isNotBlank() && state.modelState !is ModelState.Loading
            ) {
                Text("Load from Path")
            }

            state.error?.let { err ->
                Spacer(Modifier.height(16.dp))
                ErrorCard(err, viewModel::dismissError)
            }
        }
    }
}

@Composable
private fun DownloadSection(
    isDownloaded: Boolean,
    onOpenKaggle: () -> Unit,
    onLoadDefault: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isDownloaded) {
            Button(onClick = onLoadDefault, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Load Model")
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How to get the model",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Tap the button below to open Kaggle\n" +
                        "2. Sign in and accept the Gemma license\n" +
                        "3. Download the .task file (~2.6 GB)\n" +
                        "4. Note the file path (e.g. /sdcard/Download/…)\n" +
                        "5. Enter that path in the field below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenKaggle, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.OpenInBrowser, null)
                Spacer(Modifier.width(8.dp))
                Text("Get Gemma 2B from Kaggle")
            }
        }
    }
}

@Composable
private fun DownloadProgress(progress: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Downloading Gemma 4 E2B…", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("$progress%", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Keep the app open during download",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Error", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error)
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

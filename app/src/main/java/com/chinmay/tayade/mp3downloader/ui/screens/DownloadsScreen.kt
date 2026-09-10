@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chinmay.tayade.mp3downloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.data.model.DownloadRecord
import com.chinmay.tayade.mp3downloader.data.model.DownloadStatus
import com.chinmay.tayade.mp3downloader.ui.components.AppTopBar
import com.chinmay.tayade.mp3downloader.ui.components.EmptyState
import com.chinmay.tayade.mp3downloader.ui.theme.SuccessGreen
import com.chinmay.tayade.mp3downloader.util.formatBytes
import com.chinmay.tayade.mp3downloader.util.formatEta
import com.chinmay.tayade.mp3downloader.util.formatSpeed
import com.chinmay.tayade.mp3downloader.util.openMediaFile
import com.chinmay.tayade.mp3downloader.util.shareMediaFile
import com.chinmay.tayade.mp3downloader.viewmodel.DownloadsViewModel
import kotlinx.coroutines.launch

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val records by viewModel.records.collectAsStateWithLifecycle()

    val active = records.filter { it.isActive }
    val finished = records.filter { !it.isActive }

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.downloads), onBack = onBack) {
                if (finished.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearFinished) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.clear_finished))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            if (records.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Download,
                    title = stringResource(R.string.no_downloads_title),
                    subtitle = stringResource(R.string.no_downloads_body),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (active.isNotEmpty()) {
                        item { GroupHeader(stringResource(R.string.in_progress)) }
                        items(active, key = { it.id }) { record ->
                            ActiveCard(record, onCancel = { viewModel.cancel(record.id) })
                        }
                    }
                    if (finished.isNotEmpty()) {
                        item { GroupHeader(stringResource(R.string.completed)) }
                        items(finished, key = { it.id }) { record ->
                            FinishedCard(
                                record = record,
                                onOpen = {
                                    val path = record.filePath
                                    if (path == null || !openMediaFile(context, path)) {
                                        scope.launch { snackbar.showSnackbar(context.getString(R.string.file_missing)) }
                                    }
                                },
                                onShare = {
                                    val path = record.filePath
                                    if (path == null || !shareMediaFile(context, path)) {
                                        scope.launch { snackbar.showSnackbar(context.getString(R.string.file_missing)) }
                                    }
                                },
                                onRetry = { viewModel.retry(record.id) },
                                onDelete = { viewModel.delete(record.id, alsoRemoveFile = true) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun CardShell(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun Thumb(url: String) {
    AsyncImage(
        model = url.ifBlank { null },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(width = 72.dp, height = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun ActiveCard(record: DownloadRecord, onCancel: () -> Unit) {
    CardShell {
        Thumb(record.thumbnail)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                record.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            val fraction = record.progress?.fraction
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            val progress = record.progress
            val detail = when {
                record.status == DownloadStatus.QUEUED -> stringResource(R.string.queued)
                progress == null -> stringResource(R.string.starting)
                else -> buildString {
                    append(formatSpeed(progress.speedBytesPerSec))
                    if (progress.etaSeconds > 0) append(" · ").also { append(formatEta(progress.etaSeconds)) }
                }
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
        }
    }
}

@Composable
private fun FinishedCard(
    record: DownloadRecord,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val success = record.status == DownloadStatus.COMPLETED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable(enabled = success, onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(record.thumbnail)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                record.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint, label) = when (record.status) {
                    DownloadStatus.COMPLETED -> Triple(Icons.Filled.CheckCircle, SuccessGreen, formatBytes(record.sizeBytes))
                    DownloadStatus.CANCELLED -> Triple(Icons.Filled.Close, MaterialTheme.colorScheme.onSurfaceVariant, stringResource(R.string.cancelled))
                    else -> Triple(Icons.Filled.ErrorOutline, MaterialTheme.colorScheme.error, record.errorMessage ?: stringResource(R.string.failed))
                }
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${record.formatLabel} · $label",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (success) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open)) },
                        onClick = { menuOpen = false; onOpen() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = { menuOpen = false; onShare() },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.retry)) },
                        onClick = { menuOpen = false; onRetry() },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.chinmay.tayade.mp3downloader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.data.model.FormatOptions
import com.chinmay.tayade.mp3downloader.data.model.MediaSelection
import com.chinmay.tayade.mp3downloader.ui.components.AppTopBar
import com.chinmay.tayade.mp3downloader.ui.components.SectionLabel
import com.chinmay.tayade.mp3downloader.ui.components.VideoInfoCard
import com.chinmay.tayade.mp3downloader.viewmodel.FormatsViewModel

@Composable
fun FormatsScreen(
    url: String,
    onBack: () -> Unit,
    onEnqueued: () -> Unit,
    viewModel: FormatsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val enqueued by viewModel.enqueued.collectAsStateWithLifecycle()

    LaunchedEffect(url) { viewModel.load(url) }
    LaunchedEffect(enqueued) { if (enqueued) onEnqueued() }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.choose_format), onBack = onBack) },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (val s = state) {
                is FormatsViewModel.UiState.Loading -> LoadingBlock()
                is FormatsViewModel.UiState.Error -> ErrorBlock(s.message, onRetry = viewModel::retry, onBack = onBack)
                is FormatsViewModel.UiState.Ready -> ReadyBlock(
                    options = s.options,
                    onDownload = { selection -> viewModel.startDownload(url, selection) },
                )
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.reading_video), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        TextButton(onClick = onBack) { Text(stringResource(R.string.go_back)) }
    }
}

@Composable
private fun ReadyBlock(options: FormatOptions, onDownload: (MediaSelection) -> Unit) {
    val scroll = rememberScrollState()
    var selection by remember(options) {
        mutableStateOf<MediaSelection>(
            when {
                options.videoHeights.isNotEmpty() -> MediaSelection.Video(options.videoHeights.first())
                options.audioExtensions.isNotEmpty() -> MediaSelection.Audio(options.audioExtensions.first())
                else -> MediaSelection.Auto
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        VideoInfoCard(options.info)

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.video_quality))
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                label = MediaSelection.Auto.label,
                icon = Icons.Filled.Hd,
                selected = selection is MediaSelection.Auto,
                onClick = { selection = MediaSelection.Auto },
            )
            options.videoHeights.forEach { h ->
                ChoiceChip(
                    label = "${h}p",
                    icon = Icons.Filled.Hd,
                    selected = (selection as? MediaSelection.Video)?.height == h,
                    onClick = { selection = MediaSelection.Video(h) },
                )
            }
        }

        if (options.audioExtensions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.audio_only))
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.audioExtensions.forEach { ext ->
                    ChoiceChip(
                        label = ext.uppercase(),
                        icon = Icons.Filled.Audiotrack,
                        selected = (selection as? MediaSelection.Audio)?.ext == ext,
                        onClick = { selection = MediaSelection.Audio(ext) },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.format_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onDownload(selection) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.download_as, selection.label),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

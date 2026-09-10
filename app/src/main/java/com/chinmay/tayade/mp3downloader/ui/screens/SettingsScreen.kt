@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chinmay.tayade.mp3downloader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.data.ThemeMode
import com.chinmay.tayade.mp3downloader.ui.components.AppTopBar
import com.chinmay.tayade.mp3downloader.ui.components.SectionLabel
import com.chinmay.tayade.mp3downloader.util.StoragePaths
import com.chinmay.tayade.mp3downloader.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        val resolved = StoragePaths.treeUriToPath(uri)
        if (resolved != null && StoragePaths.isWritable(resolved)) {
            viewModel.setDownloadDir(resolved)
        } else if (uri != null) {
            viewModel.resetDownloadDir()
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.settings), onBack = onBack) },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            SectionLabel(stringResource(R.string.download_location))
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    .clickable { runCatching { folderPicker.launch(null) } }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    settings?.downloadDir ?: stringResource(R.string.loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = viewModel::resetDownloadDir) {
                Text(stringResource(R.string.reset_to_default))
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.appearance))
            Spacer(Modifier.height(6.dp))
            Column(Modifier.selectableGroup()) {
                ThemeMode.values().forEach { mode ->
                    RadioRow(
                        label = when (mode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        },
                        selected = settings?.themeMode == mode,
                        onClick = { viewModel.setTheme(mode) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.preferred_audio_format))
            Spacer(Modifier.height(6.dp))
            Column(Modifier.selectableGroup()) {
                listOf("m4a", "opus", "mp3").forEach { ext ->
                    RadioRow(
                        label = ext.uppercase(),
                        selected = settings?.preferredAudioExt == ext,
                        onClick = { viewModel.setAudioExt(ext) },
                    )
                }
            }
            Text(
                stringResource(R.string.audio_format_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.legal_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

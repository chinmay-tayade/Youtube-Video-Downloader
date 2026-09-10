@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chinmay.tayade.mp3downloader.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chinmay.tayade.mp3downloader.R
import com.chinmay.tayade.mp3downloader.ui.components.AppTopBar
import com.chinmay.tayade.mp3downloader.ui.components.SectionLabel
import com.chinmay.tayade.mp3downloader.util.StoragePaths
import com.chinmay.tayade.mp3downloader.util.hasNotificationPermission
import com.chinmay.tayade.mp3downloader.util.isWebUrl
import com.chinmay.tayade.mp3downloader.util.rememberResumeAwareFlag
import com.chinmay.tayade.mp3downloader.util.hasStorageAccess
import com.chinmay.tayade.mp3downloader.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    initialUrl: String?,
    onFetch: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    fun toast(message: String) = scope.launch { snackbar.showSnackbar(message) }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var link by rememberSaveable { mutableStateOf(initialUrl.orEmpty()) }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank() && link.isBlank()) link = initialUrl
    }

    val hasStorage = rememberResumeAwareFlag { hasStorageAccess(it) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val legacyStorage = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasStorage.value = granted }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        val resolved = StoragePaths.treeUriToPath(uri)
        when {
            uri == null -> toast(context.getString(R.string.pick_a_folder))
            resolved != null && StoragePaths.isWritable(resolved) -> viewModel.setDownloadDir(resolved)
            else -> {
                viewModel.resetToDefaultDir()
                toast(context.getString(R.string.folder_not_writable))
            }
        }
    }

    fun requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        .setData(Uri.fromParts("package", context.packageName, null)),
                )
            }.onFailure {
                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            legacyStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun fetch() {
        val trimmed = link.trim()
        when {
            trimmed.isEmpty() -> toast(context.getString(R.string.enter_a_url))
            !isWebUrl(trimmed) -> toast(context.getString(R.string.invalid_url))
            !hasStorage.value -> toast(context.getString(R.string.storage_required))
            else -> onFetch(trimmed)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(title = stringResource(R.string.app_name)) {
                Row {
                    IconButton(onClick = onOpenDownloads) {
                        Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.downloads))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                stringResource(R.string.subtitle_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.video_link))
            Spacer(Modifier.height(10.dp))
            LinkField(
                value = link,
                onValueChange = { link = it },
                onPaste = {
                    val text = clipboardText(context)
                    if (text == null) toast(context.getString(R.string.clipboard_empty)) else link = text
                },
                onClear = { link = "" },
            )

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.destination_folder))
            Spacer(Modifier.height(10.dp))
            DestinationField(
                path = settings?.downloadDir.orEmpty(),
                onClick = {
                    runCatching { folderPicker.launch(null) }.onFailure {
                        viewModel.resetToDefaultDir()
                        toast(context.getString(R.string.using_default_folder))
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            InfoRow(stringResource(R.string.destination_hint))

            if (!hasStorage.value) {
                Spacer(Modifier.height(20.dp))
                StorageAccessCard(onGrant = ::requestStorage)
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = ::fetch,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.fetch_details), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LinkField(
    value: String,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        placeholder = { Text(stringResource(R.string.paste_your_link_here)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        leadingIcon = {
            IconButton(onClick = onPaste) {
                Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.paste_from_clipboard))
            }
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
    )
}

@Composable
private fun DestinationField(path: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
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
            text = path.ifBlank { stringResource(R.string.tap_to_choose_folder) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InfoRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StorageAccessCard(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.storage_needed_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.storage_needed_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onGrant, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.allow_access))
        }
    }
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    val clip = clipboard?.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0)?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
}

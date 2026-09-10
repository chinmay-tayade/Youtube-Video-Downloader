@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chinmay.tayade.mp3downloader.Screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chinmay.tayade.mp3downloader.data.DownloadUiState
import com.chinmay.tayade.mp3downloader.data.DownloadViewModel
import com.chinmay.tayade.mp3downloader.ui.theme.Mp3DownloaderTheme
import com.chinmay.tayade.mp3downloader.ui.theme.SuccessGreen
import com.chinmay.tayade.mp3downloader.util.formatCountAbbreviated

class DownloadingScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val link = intent?.getStringExtra("youtube_link").orEmpty()
        val destination = intent?.getStringExtra("location_uri").orEmpty()

        setContent {
            Mp3DownloaderTheme {
                val context = LocalContext.current
                val viewModel: DownloadViewModel = viewModel()

                LaunchedEffect(Unit) {
                    if (link.isBlank()) {
                        Toast.makeText(context, "No video link was received", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        viewModel.start(link, destination)
                    }
                }

                val state by viewModel.state.collectAsStateWithLifecycle()
                DownloadingContent(state)
            }
        }
    }
}

@Composable
private fun DownloadingContent(state: DownloadUiState) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                "Downloading",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            val showVideoCard = state.fatalError == null || state.info != null
            if (showVideoCard) {
                Spacer(Modifier.height(24.dp))
                VideoCard(state)
            }

            Spacer(Modifier.height(20.dp))
            StatusCard(state)
        }
    }
}

@Composable
private fun VideoCard(state: DownloadUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        AsyncImage(
            model = state.info?.thumbnail,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = state.info?.title ?: "Loading…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stat(
                icon = { Icon(Icons.Filled.Visibility, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                text = state.info?.let { "${formatCountAbbreviated(it.views)} views" } ?: "—",
            )
            Spacer(Modifier.width(20.dp))
            Stat(
                icon = { Icon(Icons.Filled.ThumbUp, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                text = state.info?.let { "${formatCountAbbreviated(it.likes)} likes" } ?: "—",
            )
        }
    }
}

@Composable
private fun Stat(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusCard(state: DownloadUiState) {
    val accent: Color = when {
        state.fatalError != null || (state.finished && !state.success) -> MaterialTheme.colorScheme.error
        state.finished && state.success -> SuccessGreen
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                when {
                    state.finished && state.success ->
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = accent)
                    state.finished ->
                        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(18.dp), tint = accent)
                    else -> {}
                }
                if (state.finished) Spacer(Modifier.width(8.dp))
                Text(
                    state.fatalError ?: state.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        if (state.inProgress) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        } else {
            LinearProgressIndicator(
                progress = 1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

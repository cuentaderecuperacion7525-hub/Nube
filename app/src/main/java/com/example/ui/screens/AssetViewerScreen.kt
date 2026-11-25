package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AssetDto
import com.example.data.network.ImmichClientFactory
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.formatBytes
import com.example.ui.components.formatIsoDateLabel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetViewerScreen(
    assetState: UiState<AssetDto>,
    serverUrl: String,
    imageLoader: ImageLoader,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: (AssetDto) -> Unit,
) {
    val context = LocalContext.current
    var showExifSheet by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (assetState) {
            is UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }

            is UiState.Error -> {
                ErrorStateCard(
                    message = assetState.message,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is UiState.Success -> {
                val asset = assetState.data
                val previewUrl = ImmichClientFactory.thumbnailUrl(serverUrl, asset.id, size = "preview")

                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(previewUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = asset.originalFileName.ifBlank { "Vista previa de foto" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.4f
                                    }
                                },
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                offset = if (newScale > 1f) {
                                    offset + pan
                                } else {
                                    Offset.Zero
                                }
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    },
                )

                // Bottom caption bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                            ),
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Column {
                        Text(
                            text = asset.originalFileName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val dateText = formatIsoDateLabel(asset.localDateTime ?: asset.fileCreatedAt)
                        val locationText = listOfNotNull(
                            asset.exifInfo?.city?.takeIf { it.isNotBlank() },
                            asset.exifInfo?.country?.takeIf { it.isNotBlank() },
                        ).joinToString(", ")
                        Text(
                            text = if (locationText.isNotBlank()) "$dateText · $locationText" else dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                    }
                }

                if (showExifSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showExifSheet = false },
                    ) {
                        ExifDetailsSheetContent(asset = asset)
                    }
                }
            }
        }

        // Top Overlay Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("viewer_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White,
                )
            }

            if (assetState is UiState.Success) {
                val asset = assetState.data
                Row {
                    IconButton(
                        onClick = { onToggleFavorite(asset) },
                        modifier = Modifier.testTag("viewer_favorite_button"),
                    ) {
                        Icon(
                            imageVector = if (asset.isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (asset.isFavorite) Color(0xFFFF6B6B) else Color.White,
                        )
                    }
                    IconButton(
                        onClick = { showExifSheet = true },
                        modifier = Modifier.testTag("viewer_info_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.cd_info),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExifDetailsSheetContent(asset: AssetDto) {
    val exif = asset.exifInfo
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.exif_details_title),
            style = MaterialTheme.typography.titleLarge,
        )

        ExifRow(
            icon = Icons.Default.Schedule,
            label = "Fecha de captura",
            value = formatIsoDateLabel(exif?.dateTimeOriginal ?: asset.localDateTime ?: asset.fileCreatedAt),
        )

        val dims = if (exif?.exifImageWidth != null && exif.exifImageHeight != null) {
            "${exif.exifImageWidth} × ${exif.exifImageHeight} px"
        } else null
        val sizeStr = exif?.resolvedFileSize?.let { formatBytes(it) }
        val fileSummary = listOfNotNull(dims, sizeStr, asset.originalMimeType).joinToString(" · ")
        if (fileSummary.isNotBlank()) {
            ExifRow(
                icon = Icons.Default.PhotoSizeSelectLarge,
                label = asset.originalFileName,
                value = fileSummary,
            )
        }

        val cameraName = listOfNotNull(
            exif?.make?.takeIf { it.isNotBlank() },
            exif?.model?.takeIf { it.isNotBlank() },
        ).joinToString(" ")
        if (cameraName.isNotBlank()) {
            val shootingSpecs = listOfNotNull(
                exif?.fNumber?.let { "f/$it" },
                exif?.exposureTime?.let { "${it}s" },
                exif?.focalLength?.let { "${it}mm" },
                exif?.iso?.let { "ISO $it" },
            ).joinToString("  ·  ")
            ExifRow(
                icon = Icons.Default.CameraAlt,
                label = cameraName,
                value = shootingSpecs.ifBlank { exif?.lensModel ?: "Metadatos de cámara" },
            )
        }

        val location = listOfNotNull(
            exif?.city?.takeIf { it.isNotBlank() },
            exif?.state?.takeIf { it.isNotBlank() },
            exif?.country?.takeIf { it.isNotBlank() },
        ).joinToString(", ")
        if (location.isNotBlank()) {
            ExifRow(
                icon = Icons.Default.LocationOn,
                label = "Ubicación",
                value = location,
            )
        }

        HorizontalDivider()
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Asset ID: ${asset.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExifRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AssetDto
import com.example.data.model.GitHubReleaseDto
import com.example.data.model.GitHubRepoDto
import com.example.data.network.ImmichClientFactory
import com.example.ui.viewmodel.GitHubUiState
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun AssetGridTile(
    asset: AssetDto,
    serverUrl: String,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val thumbUrl = ImmichClientFactory.thumbnailUrl(serverUrl, asset.id, size = "thumbnail")

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .testTag("asset_tile_${asset.id}"),
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbUrl)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = asset.originalFileName.ifBlank { "Foto de Immich" },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            },
        )

        // Subtle bottom gradient when badges exist
        if (asset.isFavorite || asset.type.equals("VIDEO", ignoreCase = true)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        ),
                    ),
            )
        }

        if (asset.isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorito",
                tint = Color(0xFFFF6B6B),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(16.dp),
            )
        }

        if (asset.type.equals("VIDEO", ignoreCase = true)) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                    val durationText = formatDuration(asset.duration)
                    if (durationText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag("retry_button"),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.retry_button))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GitHubEcosystemSection(
    gitHubState: GitHubUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.github_repos_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Datos en vivo desde github.com/immich-app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("github_refresh_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh),
                )
            }
        }

        OutlinedTextField(
            value = gitHubState.searchQuery,
            onValueChange = onSearchChanged,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Filtrar repositorios (ej. immich, charts, geo)…") },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("github_search_input"),
        )

        when (val state = gitHubState.state) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                ErrorStateCard(
                    message = state.message,
                    onRetry = onRefresh,
                )
            }

            is UiState.Success -> {
                val (repos, latestRelease) = state.data
                if (latestRelease != null) {
                    LatestReleaseBanner(
                        release = latestRelease,
                        onOpen = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestRelease.htmlUrl))
                            context.startActivity(intent)
                        },
                    )
                }

                val filtered = repos.filter { repo ->
                    val q = gitHubState.searchQuery.trim()
                    q.isEmpty() ||
                        repo.name.contains(q, ignoreCase = true) ||
                        (repo.description?.contains(q, ignoreCase = true) == true) ||
                        (repo.language?.contains(q, ignoreCase = true) == true)
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = "No se encontraron repositorios para \"${gitHubState.searchQuery}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    filtered.forEach { repo ->
                        GitHubRepoCard(
                            repo = repo,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl))
                                context.startActivity(intent)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestReleaseBanner(
    release: GitHubReleaseDto,
    onOpen: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.large,
            )
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .testTag("github_latest_release_card"),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Última versión de Immich",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = release.tagName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(R.string.cd_open_github),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GitHubRepoCard(
    repo: GitHubRepoDto,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large,
            )
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("github_repo_${repo.name}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = repo.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.cd_open_github),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (!repo.description.isNullOrBlank()) {
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetricPill(
                    icon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                    text = formatCompactNumber(repo.stargazersCount),
                )
                MetricPill(
                    icon = {
                        Icon(
                            Icons.Outlined.CallSplit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                    text = formatCompactNumber(repo.forksCount),
                )
                if (!repo.language.isNullOrBlank()) {
                    MetricPill(icon = null, text = repo.language)
                }
                repo.license?.spdxId?.takeIf { it.isNotBlank() && it != "NOASSERTION" }?.let { lic ->
                    MetricPill(icon = null, text = lic)
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: (@Composable () -> Unit)?,
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

fun formatDuration(raw: String?): String {
    if (raw.isNullOrBlank() || raw == "0:00:00.000000") return ""
    val clean = raw.substringBefore(".")
    val parts = clean.split(":")
    return if (parts.size == 3) {
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        val s = parts[2].toIntOrNull() ?: 0
        if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    } else clean
}

fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "0 B"
    val unit = 1024.0
    if (bytes < unit) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceAtMost(4)
    val pre = "KMGT"[exp - 1]
    return String.format(Locale.US, "%.1f %sB", bytes / unit.pow(exp.toDouble()), pre)
}

fun formatCompactNumber(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000.0)
        else -> value.toString()
    }
}

fun formatIsoDateLabel(isoString: String): String {
    if (isoString.isBlank()) return "Sin fecha"
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
    )
    for (pattern in formats) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = parser.parse(isoString) ?: continue
            val cal = Calendar.getInstance().apply { time = parsed }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            return when {
                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hoy"
                cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Ayer"
                else -> SimpleDateFormat("d 'de' MMMM yyyy", Locale("es", "ES")).format(parsed)
            }
        } catch (_: Exception) {
        }
    }
    return isoString.take(10)
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.R
import com.example.data.model.AssetDto
import com.example.ui.components.AssetGridTile
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.formatIsoDateLabel
import com.example.ui.viewmodel.TimelineFilter
import com.example.ui.viewmodel.TimelineUiState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    state: TimelineUiState,
    serverUrl: String,
    imageLoader: ImageLoader,
    onFilterSelected: (TimelineFilter) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onAssetClick: (AssetDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 8
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimelineFilter.entries.forEach { filter ->
                val icon = when (filter) {
                    TimelineFilter.ALL -> Icons.Default.PhotoLibrary
                    TimelineFilter.FAVORITES -> Icons.Default.Favorite
                    TimelineFilter.IMAGES -> Icons.Default.Photo
                    TimelineFilter.VIDEOS -> Icons.Default.Videocam
                }
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier.testTag("timeline_filter_${filter.name.lowercase()}"),
                )
            }
        }

        // Upload Progress Banner
        AnimatedVisibility(visible = state.isUploading) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(MaterialTheme.shapes.medium),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.uploadProgressText ?: "Subiendo archivos…",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val assetsState = state.assetsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorStateCard(
                            message = assetsState.message,
                            onRetry = { onRefresh() },
                        )
                    }
                }

                is UiState.Success -> {
                    val items = assetsState.data
                    if (items.isEmpty()) {
                        EmptyTimelinePlaceholder()
                    } else {
                        val grouped = remember(items) {
                            items.groupBy { formatIsoDateLabel(it.localDateTime ?: it.fileCreatedAt) }
                        }

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 112.dp),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 4.dp,
                                bottom = 96.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("timeline_grid"),
                        ) {
                            grouped.forEach { (dateLabel, dateAssets) ->
                                item(
                                    key = "header_$dateLabel",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 14.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = dateLabel,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = RoundedCornerShape(99.dp),
                                        ) {
                                            Text(
                                                text = "${dateAssets.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }

                                items(
                                    items = dateAssets,
                                    key = { it.id },
                                ) { asset ->
                                    AssetGridTile(
                                        asset = asset,
                                        serverUrl = serverUrl,
                                        imageLoader = imageLoader,
                                        onClick = { onAssetClick(asset) },
                                    )
                                }
                            }

                            if (state.isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimelinePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_vault),
            contentDescription = "Bóveda vacía",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 220.dp, height = 165.dp)
                .clip(MaterialTheme.shapes.large),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.empty_photos_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_photos_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

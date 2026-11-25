package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.R
import com.example.data.model.AlbumDto
import com.example.data.model.AssetDto
import com.example.data.preferences.SessionCredentials
import com.example.ui.viewmodel.GitHubUiState
import com.example.ui.viewmodel.SearchMode
import com.example.ui.viewmodel.SearchUiState
import com.example.ui.viewmodel.ServerHubData
import com.example.ui.viewmodel.TimelineFilter
import com.example.ui.viewmodel.TimelineUiState
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.flow.SharedFlow

enum class VaultTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    PHOTOS(R.string.nav_photos, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    ALBUMS(R.string.nav_albums, Icons.Filled.PhotoAlbum, Icons.Outlined.PhotoAlbum),
    SEARCH(R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    HUB(R.string.nav_hub, Icons.Filled.CloudQueue, Icons.Outlined.CloudQueue),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVaultScaffold(
    session: SessionCredentials,
    imageLoader: ImageLoader,
    timelineState: TimelineUiState,
    albumsState: UiState<List<AlbumDto>>,
    searchState: SearchUiState,
    serverHubState: UiState<ServerHubData>,
    gitHubState: GitHubUiState,
    snackbarEvents: SharedFlow<String>,
    onTimelineFilterSelected: (TimelineFilter) -> Unit,
    onRefreshTimeline: () -> Unit,
    onLoadMoreTimeline: () -> Unit,
    onUploadMediaUris: (List<android.net.Uri>) -> Unit,
    onRefreshAlbums: () -> Unit,
    onCreateAlbum: (String, String?) -> Unit,
    onAlbumClick: (AlbumDto) -> Unit,
    onAssetClick: (AssetDto) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchModeChanged: (SearchMode) -> Unit,
    onExecuteSearch: (String?) -> Unit,
    onRefreshServerHub: () -> Unit,
    onGitHubSearchChanged: (String) -> Unit,
    onRefreshGitHub: () -> Unit,
    onLogout: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(VaultTab.PHOTOS) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarEvents) {
        snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
    ) { uris ->
        if (uris.isNotEmpty()) {
            onUploadMediaUris(uris)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Spacer(Modifier.weight(1f))
                    VaultTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes),
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.navigationBars,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (selectedTab) {
                                    VaultTab.PHOTOS -> stringResource(R.string.app_name)
                                    VaultTab.ALBUMS -> stringResource(R.string.albums_title)
                                    VaultTab.SEARCH -> stringResource(R.string.nav_search)
                                    VaultTab.HUB -> "Servidor y GitHub"
                                },
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    when (selectedTab) {
                                        VaultTab.PHOTOS -> onRefreshTimeline()
                                        VaultTab.ALBUMS -> onRefreshAlbums()
                                        VaultTab.SEARCH -> onExecuteSearch(null)
                                        VaultTab.HUB -> {
                                            onRefreshServerHub()
                                            onRefreshGitHub()
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("topbar_refresh_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            VaultTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag("nav_bar_${tab.name.lowercase()}"),
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    when (selectedTab) {
                        VaultTab.PHOTOS -> {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("fab_upload_photo"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = stringResource(R.string.cd_upload),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.upload_photo))
                            }
                        }

                        VaultTab.ALBUMS -> {
                            ExtendedFloatingActionButton(
                                onClick = { showCreateAlbumDialog = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("fab_create_album"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.create_album))
                            }
                        }

                        else -> Unit
                    }
                },
            ) { innerPadding ->
                when (selectedTab) {
                    VaultTab.PHOTOS -> TimelineScreen(
                        state = timelineState,
                        serverUrl = session.serverUrl,
                        imageLoader = imageLoader,
                        onFilterSelected = onTimelineFilterSelected,
                        onRefresh = onRefreshTimeline,
                        onLoadMore = onLoadMoreTimeline,
                        onAssetClick = onAssetClick,
                        modifier = Modifier.padding(innerPadding),
                    )

                    VaultTab.ALBUMS -> AlbumsScreen(
                        albumsState = albumsState,
                        serverUrl = session.serverUrl,
                        imageLoader = imageLoader,
                        onRetry = onRefreshAlbums,
                        onAlbumClick = onAlbumClick,
                        modifier = Modifier.padding(innerPadding),
                    )

                    VaultTab.SEARCH -> SearchScreen(
                        state = searchState,
                        serverUrl = session.serverUrl,
                        imageLoader = imageLoader,
                        onQueryChanged = onSearchQueryChanged,
                        onModeChanged = onSearchModeChanged,
                        onExecuteSearch = onExecuteSearch,
                        onAssetClick = onAssetClick,
                        modifier = Modifier.padding(innerPadding),
                    )

                    VaultTab.HUB -> HubScreen(
                        session = session,
                        serverHubState = serverHubState,
                        gitHubState = gitHubState,
                        onRetryServerHub = onRefreshServerHub,
                        onGitHubSearchChanged = onGitHubSearchChanged,
                        onRefreshGitHub = onRefreshGitHub,
                        onLogout = onLogout,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateAlbumDialog = false },
            onConfirm = { name, desc ->
                showCreateAlbumDialog = false
                onCreateAlbum(name, desc)
            },
        )
    }
}

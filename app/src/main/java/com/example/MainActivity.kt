package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.navigation.AlbumDetailRoute
import com.example.ui.navigation.AssetViewerRoute
import com.example.ui.navigation.ConnectRoute
import com.example.ui.navigation.GitHubExplorerRoute
import com.example.ui.navigation.MainVaultRoute
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.AssetViewerScreen
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.GitHubExplorerScreen
import com.example.ui.screens.MainVaultScaffold
import com.example.ui.theme.ImmichVaultTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImmichVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ImmichVaultApp()
                }
            }
        }
    }
}

@Composable
fun ImmichVaultApp(
    viewModel: MainViewModel = viewModel(),
) {
    val sessionReady by viewModel.sessionReady.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val connectState by viewModel.connectState.collectAsStateWithLifecycle()
    val timelineState by viewModel.timelineState.collectAsStateWithLifecycle()
    val albumsState by viewModel.albumsState.collectAsStateWithLifecycle()
    val albumDetailState by viewModel.albumDetailState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val viewerAssetState by viewModel.viewerAssetState.collectAsStateWithLifecycle()
    val isDownloadingOriginal by viewModel.isDownloadingOriginal.collectAsStateWithLifecycle()
    val serverHubState by viewModel.serverHubState.collectAsStateWithLifecycle()
    val gitHubState by viewModel.gitHubState.collectAsStateWithLifecycle()

    if (!sessionReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()

    LaunchedEffect(session.isAuthenticated) {
        if (session.isAuthenticated) {
            navController.navigate(MainVaultRoute) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(ConnectRoute) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination: Any = if (session.isAuthenticated) MainVaultRoute else ConnectRoute

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<ConnectRoute> {
            ConnectScreen(
                state = connectState,
                onServerUrlChanged = viewModel::onServerUrlChanged,
                onEmailChanged = viewModel::onEmailChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onApiKeyChanged = viewModel::onApiKeyChanged,
                onAuthModeSelected = viewModel::onAuthModeSelected,
                onProbeServer = viewModel::probeServer,
                onConnect = viewModel::connectToServer,
                onExploreGitHub = {
                    navController.navigate(GitHubExplorerRoute)
                },
            )
        }

        composable<GitHubExplorerRoute> {
            GitHubExplorerScreen(
                gitHubState = gitHubState,
                onSearchChanged = viewModel::onGitHubSearchChanged,
                onRefresh = viewModel::loadGitHubRepos,
                onBack = { navController.popBackStack() },
            )
        }

        composable<MainVaultRoute> {
            MainVaultScaffold(
                session = session,
                imageLoader = viewModel.imageLoader,
                timelineState = timelineState,
                albumsState = albumsState,
                searchState = searchState,
                serverHubState = serverHubState,
                gitHubState = gitHubState,
                snackbarEvents = viewModel.snackbarEvents,
                onTimelineFilterSelected = viewModel::setTimelineFilter,
                onRefreshTimeline = { viewModel.loadTimeline(refresh = true) },
                onLoadMoreTimeline = viewModel::loadMoreTimeline,
                onUploadMediaUris = viewModel::uploadSelectedMedia,
                onRefreshAlbums = viewModel::loadAlbums,
                onCreateAlbum = viewModel::createAlbum,
                onAlbumClick = { album ->
                    viewModel.loadAlbumDetail(album.id)
                    navController.navigate(AlbumDetailRoute(albumId = album.id, albumName = album.albumName))
                },
                onAssetClick = { asset ->
                    viewModel.loadAssetDetail(asset.id)
                    navController.navigate(AssetViewerRoute(assetId = asset.id))
                },
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSearchModeChanged = viewModel::onSearchModeChanged,
                onExecuteSearch = viewModel::executeSearch,
                onRefreshServerHub = viewModel::loadServerHub,
                onGitHubSearchChanged = viewModel::onGitHubSearchChanged,
                onRefreshGitHub = viewModel::loadGitHubRepos,
                onLogout = viewModel::logout,
            )
        }

        composable<AlbumDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AlbumDetailRoute>()
            AlbumDetailScreen(
                albumTitle = route.albumName,
                albumDetailState = albumDetailState,
                serverUrl = session.serverUrl,
                imageLoader = viewModel.imageLoader,
                onBack = { navController.popBackStack() },
                onRetry = { viewModel.loadAlbumDetail(route.albumId) },
                onAssetClick = { asset ->
                    viewModel.loadAssetDetail(asset.id)
                    navController.navigate(AssetViewerRoute(assetId = asset.id))
                },
            )
        }

        composable<AssetViewerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AssetViewerRoute>()
            AssetViewerScreen(
                assetState = viewerAssetState,
                serverUrl = session.serverUrl,
                imageLoader = viewModel.imageLoader,
                isDownloading = isDownloadingOriginal,
                snackbarEvents = viewModel.snackbarEvents,
                onBack = { navController.popBackStack() },
                onRetry = { viewModel.loadAssetDetail(route.assetId) },
                onToggleFavorite = viewModel::toggleFavorite,
                onDownloadOriginal = viewModel::downloadOriginalAsset,
            )
        }
    }
}

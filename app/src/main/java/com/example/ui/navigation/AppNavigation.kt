package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ConnectRoute

@Serializable
data object MainVaultRoute

@Serializable
data object GitHubExplorerRoute

@Serializable
data class AlbumDetailRoute(
    val albumId: String,
    val albumName: String,
)

@Serializable
data class AssetViewerRoute(
    val assetId: String,
)

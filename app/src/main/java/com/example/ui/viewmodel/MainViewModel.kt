package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.example.data.model.AlbumDto
import com.example.data.model.AssetDto
import com.example.data.model.GitHubReleaseDto
import com.example.data.model.GitHubRepoDto
import com.example.data.model.ServerAboutResponse
import com.example.data.model.ServerFeaturesResponse
import com.example.data.model.UserMeResponse
import com.example.data.network.GitHubApiService
import com.example.data.network.ImmichClientFactory
import com.example.data.network.ImmichImageLoader
import com.example.data.preferences.SessionCredentials
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.GitHubRepository
import com.example.data.repository.ImmichRepository
import com.example.data.repository.ServerProbeResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    CREDENTIALS,
    API_KEY,
}

enum class TimelineFilter(val label: String, val typeParam: String?, val favoriteOnly: Boolean?) {
    ALL("Todos", null, null),
    FAVORITES("Favoritos", null, true),
    IMAGES("Fotos", "IMAGE", null),
    VIDEOS("Videos", "VIDEO", null),
}

enum class SearchMode {
    SMART_AI,
    METADATA,
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ConnectUiState(
    val serverUrl: String = "",
    val email: String = "",
    val password: String = "",
    val apiKey: String = "",
    val authMode: AuthMode = AuthMode.CREDENTIALS,
    val isProbing: Boolean = false,
    val probeResult: ServerProbeResult? = null,
    val probeError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
)

data class TimelineUiState(
    val filter: TimelineFilter = TimelineFilter.ALL,
    val assetsState: UiState<List<AssetDto>> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val isUploading: Boolean = false,
    val uploadProgressText: String? = null,
)

data class SearchUiState(
    val query: String = "",
    val mode: SearchMode = SearchMode.SMART_AI,
    val hasSearched: Boolean = false,
    val resultsState: UiState<List<AssetDto>> = UiState.Success(emptyList()),
)

data class ServerHubData(
    val user: UserMeResponse,
    val about: ServerAboutResponse?,
    val features: ServerFeaturesResponse?,
)

data class GitHubUiState(
    val searchQuery: String = "",
    val state: UiState<Pair<List<GitHubRepoDto>, GitHubReleaseDto?>> = UiState.Loading,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val clientFactory = ImmichClientFactory()
    private val preferences = SessionPreferences(application)
    private val immichRepository = ImmichRepository(clientFactory, preferences)
    private val gitHubRepository = GitHubRepository(GitHubApiService.create())

    val imageLoader: ImageLoader = ImmichImageLoader.create(application, clientFactory.okHttpClient)

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    private val _session = MutableStateFlow(SessionCredentials())
    val session: StateFlow<SessionCredentials> = _session.asStateFlow()

    private val _connectState = MutableStateFlow(ConnectUiState())
    val connectState: StateFlow<ConnectUiState> = _connectState.asStateFlow()

    private val _timelineState = MutableStateFlow(TimelineUiState())
    val timelineState: StateFlow<TimelineUiState> = _timelineState.asStateFlow()

    private val _albumsState = MutableStateFlow<UiState<List<AlbumDto>>>(UiState.Loading)
    val albumsState: StateFlow<UiState<List<AlbumDto>>> = _albumsState.asStateFlow()

    private val _albumDetailState = MutableStateFlow<UiState<AlbumDto>>(UiState.Loading)
    val albumDetailState: StateFlow<UiState<AlbumDto>> = _albumDetailState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _viewerAssetState = MutableStateFlow<UiState<AssetDto>>(UiState.Loading)
    val viewerAssetState: StateFlow<UiState<AssetDto>> = _viewerAssetState.asStateFlow()

    private val _isDownloadingOriginal = MutableStateFlow(false)
    val isDownloadingOriginal: StateFlow<Boolean> = _isDownloadingOriginal.asStateFlow()

    private val _serverHubState = MutableStateFlow<UiState<ServerHubData>>(UiState.Loading)
    val serverHubState: StateFlow<UiState<ServerHubData>> = _serverHubState.asStateFlow()

    private val _gitHubState = MutableStateFlow(GitHubUiState())
    val gitHubState: StateFlow<GitHubUiState> = _gitHubState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            immichRepository.sessionFlow.collect { creds ->
                val wasAuth = _session.value.isAuthenticated
                val oldUrl = _session.value.serverUrl
                _session.value = creds
                immichRepository.updateActiveAuth(creds)
                if (_connectState.value.serverUrl.isBlank() && creds.serverUrl.isNotBlank()) {
                    _connectState.update { it.copy(serverUrl = creds.serverUrl) }
                }
                _sessionReady.value = true
                if (creds.isAuthenticated && (!wasAuth || oldUrl != creds.serverUrl)) {
                    refreshAllVaultData()
                }
            }
        }
        loadGitHubRepos()
    }

    // --- Connect / Auth Actions ---

    fun onServerUrlChanged(value: String) {
        _connectState.update {
            it.copy(serverUrl = value, probeError = null, submitError = null, probeResult = null)
        }
    }

    fun onEmailChanged(value: String) {
        _connectState.update { it.copy(email = value, submitError = null) }
    }

    fun onPasswordChanged(value: String) {
        _connectState.update { it.copy(password = value, submitError = null) }
    }

    fun onApiKeyChanged(value: String) {
        _connectState.update { it.copy(apiKey = value, submitError = null) }
    }

    fun onAuthModeSelected(mode: AuthMode) {
        _connectState.update { it.copy(authMode = mode, submitError = null) }
    }

    fun probeServer() {
        val url = _connectState.value.serverUrl
        viewModelScope.launch {
            _connectState.update { it.copy(isProbing = true, probeError = null, probeResult = null) }
            immichRepository.probeServer(url)
                .onSuccess { result ->
                    _connectState.update {
                        it.copy(
                            serverUrl = result.normalizedRootUrl,
                            isProbing = false,
                            probeResult = result,
                            probeError = null,
                        )
                    }
                }
                .onFailure { err ->
                    _connectState.update {
                        it.copy(
                            isProbing = false,
                            probeError = err.message ?: "No se pudo verificar el servidor.",
                        )
                    }
                }
        }
    }

    fun connectToServer() {
        val state = _connectState.value
        viewModelScope.launch {
            _connectState.update { it.copy(isSubmitting = true, submitError = null) }
            val result = when (state.authMode) {
                AuthMode.CREDENTIALS -> immichRepository.loginWithCredentials(
                    rawUrl = state.serverUrl,
                    email = state.email,
                    password = state.password,
                )
                AuthMode.API_KEY -> immichRepository.loginWithApiKey(
                    rawUrl = state.serverUrl,
                    apiKey = state.apiKey,
                )
            }
            result
                .onSuccess {
                    _connectState.update { it.copy(isSubmitting = false, password = "", submitError = null) }
                }
                .onFailure { err ->
                    _connectState.update {
                        it.copy(
                            isSubmitting = false,
                            submitError = err.message ?: "Error al autenticar con Immich.",
                        )
                    }
                }
        }
    }

    fun logout() {
        val url = _session.value.serverUrl
        viewModelScope.launch {
            immichRepository.logout(url)
            _timelineState.value = TimelineUiState()
            _albumsState.value = UiState.Loading
            _searchState.value = SearchUiState()
            _serverHubState.value = UiState.Loading
        }
    }

    // --- Vault Data Loading ---

    fun refreshAllVaultData() {
        loadTimeline(refresh = true)
        loadAlbums()
        loadServerHub()
    }

    fun setTimelineFilter(filter: TimelineFilter) {
        if (_timelineState.value.filter == filter) return
        _timelineState.update { it.copy(filter = filter, assetsState = UiState.Loading) }
        loadTimeline(refresh = false)
    }

    fun loadTimeline(refresh: Boolean = false) {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        val filter = _timelineState.value.filter

        viewModelScope.launch {
            if (refresh) {
                _timelineState.update { it.copy(isRefreshing = true) }
            } else {
                _timelineState.update { it.copy(assetsState = UiState.Loading) }
            }
            immichRepository.getTimelineAssets(
                serverUrl = serverUrl,
                page = 1,
                size = 60,
                isFavorite = filter.favoriteOnly,
                type = filter.typeParam,
            ).onSuccess { items ->
                _timelineState.update {
                    it.copy(
                        assetsState = UiState.Success(items),
                        isRefreshing = false,
                        currentPage = 1,
                        canLoadMore = items.size >= 60,
                    )
                }
            }.onFailure { err ->
                _timelineState.update {
                    it.copy(
                        assetsState = UiState.Error(err.message ?: "No se pudieron cargar las fotos."),
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    fun loadMoreTimeline() {
        val state = _timelineState.value
        val currentItems = (state.assetsState as? UiState.Success)?.data ?: return
        if (state.isLoadingMore || !state.canLoadMore) return
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return

        val nextPage = state.currentPage + 1
        viewModelScope.launch {
            _timelineState.update { it.copy(isLoadingMore = true) }
            immichRepository.getTimelineAssets(
                serverUrl = serverUrl,
                page = nextPage,
                size = 60,
                isFavorite = state.filter.favoriteOnly,
                type = state.filter.typeParam,
            ).onSuccess { newItems ->
                val merged = (currentItems + newItems).distinctBy { it.id }
                _timelineState.update {
                    it.copy(
                        assetsState = UiState.Success(merged),
                        isLoadingMore = false,
                        currentPage = nextPage,
                        canLoadMore = newItems.size >= 60,
                    )
                }
            }.onFailure {
                _timelineState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun uploadSelectedMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        val resolver = getApplication<Application>().contentResolver

        viewModelScope.launch {
            var uploadedCount = 0
            var duplicateCount = 0
            var failedCount = 0

            uris.forEachIndexed { index, uri ->
                _timelineState.update {
                    it.copy(
                        isUploading = true,
                        uploadProgressText = "Subiendo ${index + 1} de ${uris.size}…",
                    )
                }
                immichRepository.uploadPhotoFromUri(serverUrl, resolver, uri)
                    .onSuccess { res ->
                        val isDup = res.duplicate || res.status.equals("duplicate", ignoreCase = true)
                        if (isDup) duplicateCount++ else uploadedCount++
                    }
                    .onFailure {
                        failedCount++
                    }
            }

            _timelineState.update { it.copy(isUploading = false, uploadProgressText = null) }
            val summary = buildString {
                if (uploadedCount > 0) append("$uploadedCount subido(s). ")
                if (duplicateCount > 0) append("$duplicateCount ya existía(n). ")
                if (failedCount > 0) append("$failedCount error(es) al subir.")
            }.trim()
            if (summary.isNotEmpty()) {
                _snackbarEvents.emit(summary)
            }
            if (uploadedCount > 0) {
                loadTimeline(refresh = true)
            }
        }
    }

    // --- Albums ---

    fun loadAlbums() {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        viewModelScope.launch {
            _albumsState.value = UiState.Loading
            immichRepository.getAlbums(serverUrl)
                .onSuccess { albums ->
                    _albumsState.value = UiState.Success(albums)
                }
                .onFailure { err ->
                    _albumsState.value = UiState.Error(err.message ?: "Error al cargar álbumes.")
                }
        }
    }

    fun loadAlbumDetail(albumId: String) {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        viewModelScope.launch {
            _albumDetailState.value = UiState.Loading
            immichRepository.getAlbumDetail(serverUrl, albumId)
                .onSuccess { album ->
                    _albumDetailState.value = UiState.Success(album)
                }
                .onFailure { err ->
                    _albumDetailState.value = UiState.Error(err.message ?: "Error al cargar el álbum.")
                }
        }
    }

    fun createAlbum(name: String, description: String?) {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        viewModelScope.launch {
            immichRepository.createAlbum(serverUrl, name, description)
                .onSuccess {
                    _snackbarEvents.emit("Álbum \"${it.albumName}\" creado.")
                    loadAlbums()
                }
                .onFailure { err ->
                    _snackbarEvents.emit(err.message ?: "No se pudo crear el álbum.")
                }
        }
    }

    // --- Search ---

    fun onSearchQueryChanged(query: String) {
        _searchState.update { it.copy(query = query) }
    }

    fun onSearchModeChanged(mode: SearchMode) {
        _searchState.update { it.copy(mode = mode) }
        if (_searchState.value.query.isNotBlank()) {
            executeSearch()
        }
    }

    fun executeSearch(overrideQuery: String? = null) {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        val q = (overrideQuery ?: _searchState.value.query).trim()
        if (q.isEmpty()) return

        _searchState.update {
            it.copy(
                query = q,
                hasSearched = true,
                resultsState = UiState.Loading,
            )
        }

        val mode = _searchState.value.mode
        viewModelScope.launch {
            val result = when (mode) {
                SearchMode.SMART_AI -> immichRepository.searchSmart(serverUrl, q)
                SearchMode.METADATA -> immichRepository.searchByMetadata(serverUrl, q)
            }
            result
                .onSuccess { items ->
                    _searchState.update { it.copy(resultsState = UiState.Success(items)) }
                }
                .onFailure { err ->
                    _searchState.update {
                        it.copy(
                            resultsState = UiState.Error(
                                err.message ?: "No se pudo completar la búsqueda."
                            )
                        )
                    }
                }
        }
    }

    // --- Asset Viewer & Favorite ---

    fun loadAssetDetail(assetId: String) {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        // Pre-populate from cached timeline/search/album if available for instant render
        val cached = findCachedAsset(assetId)
        if (cached != null) {
            _viewerAssetState.value = UiState.Success(cached)
        } else {
            _viewerAssetState.value = UiState.Loading
        }
        viewModelScope.launch {
            immichRepository.getAsset(serverUrl, assetId)
                .onSuccess { fullAsset ->
                    _viewerAssetState.value = UiState.Success(fullAsset)
                }
                .onFailure { err ->
                    if (cached == null) {
                        _viewerAssetState.value = UiState.Error(
                            err.message ?: "No se pudo cargar el archivo."
                        )
                    }
                }
        }
    }

    fun toggleFavorite(asset: AssetDto) {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        val newFav = !asset.isFavorite
        viewModelScope.launch {
            immichRepository.toggleFavorite(serverUrl, asset.id, newFav)
                .onSuccess { updated ->
                    _viewerAssetState.value = UiState.Success(updated)
                    propagateAssetUpdate(updated)
                }
                .onFailure { err ->
                    _snackbarEvents.emit(err.message ?: "No se pudo actualizar favorito.")
                }
        }
    }

    fun downloadOriginalAsset(asset: AssetDto) {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank() || _isDownloadingOriginal.value) return
        viewModelScope.launch {
            _isDownloadingOriginal.value = true
            immichRepository.downloadOriginalToDevice(
                serverUrl = serverUrl,
                context = getApplication(),
                asset = asset,
            ).onSuccess { msg ->
                _snackbarEvents.emit(msg)
            }.onFailure { err ->
                _snackbarEvents.emit(err.message ?: "No se pudo descargar el archivo.")
            }
            _isDownloadingOriginal.value = false
        }
    }

    private fun findCachedAsset(assetId: String): AssetDto? {
        val timelineItems = (_timelineState.value.assetsState as? UiState.Success)?.data.orEmpty()
        timelineItems.firstOrNull { it.id == assetId }?.let { return it }
        val searchItems = (_searchState.value.resultsState as? UiState.Success)?.data.orEmpty()
        searchItems.firstOrNull { it.id == assetId }?.let { return it }
        val albumItems = (_albumDetailState.value as? UiState.Success)?.data?.assets.orEmpty()
        return albumItems.firstOrNull { it.id == assetId }
    }

    private fun propagateAssetUpdate(updated: AssetDto) {
        val tl = _timelineState.value.assetsState
        if (tl is UiState.Success) {
            _timelineState.update { state ->
                state.copy(
                    assetsState = UiState.Success(
                        tl.data.map { if (it.id == updated.id) updated else it }
                    )
                )
            }
        }
        val sr = _searchState.value.resultsState
        if (sr is UiState.Success) {
            _searchState.update { state ->
                state.copy(
                    resultsState = UiState.Success(
                        sr.data.map { if (it.id == updated.id) updated else it }
                    )
                )
            }
        }
        val ad = _albumDetailState.value
        if (ad is UiState.Success) {
            _albumDetailState.value = UiState.Success(
                ad.data.copy(assets = ad.data.assets.map { if (it.id == updated.id) updated else it })
            )
        }
    }

    // --- Server Hub & GitHub Ecosystem ---

    fun loadServerHub() {
        val serverUrl = _session.value.serverUrl
        if (serverUrl.isBlank()) return
        viewModelScope.launch {
            _serverHubState.value = UiState.Loading
            immichRepository.getServerProfileAndStats(serverUrl)
                .onSuccess { (me, about, features) ->
                    _serverHubState.value = UiState.Success(
                        ServerHubData(user = me, about = about, features = features)
                    )
                }
                .onFailure { err ->
                    _serverHubState.value = UiState.Error(
                        err.message ?: "No se pudo obtener la información del servidor."
                    )
                }
        }
    }

    fun onGitHubSearchChanged(query: String) {
        _gitHubState.update { it.copy(searchQuery = query) }
    }

    fun loadGitHubRepos() {
        viewModelScope.launch {
            _gitHubState.update { it.copy(state = UiState.Loading) }
            gitHubRepository.fetchImmichEcosystem()
                .onSuccess { data ->
                    _gitHubState.update {
                        it.copy(state = UiState.Success(data.repos to data.latestRelease))
                    }
                }
                .onFailure { err ->
                    _gitHubState.update {
                        it.copy(
                            state = UiState.Error(
                                err.message ?: "No se pudieron cargar los repositorios de GitHub."
                            )
                        )
                    }
                }
        }
    }
}

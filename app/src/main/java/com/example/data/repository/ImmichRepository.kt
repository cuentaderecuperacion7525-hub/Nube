package com.example.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.AlbumDto
import com.example.data.model.AssetDto
import com.example.data.model.CreateAlbumRequest
import com.example.data.model.LoginRequest
import com.example.data.model.SearchMetadataRequest
import com.example.data.model.SearchSmartRequest
import com.example.data.model.ServerAboutResponse
import com.example.data.model.ServerFeaturesResponse
import com.example.data.model.UpdateAssetRequest
import com.example.data.model.UploadAssetResponse
import com.example.data.model.UserMeResponse
import com.example.data.network.ImmichClientFactory
import com.example.data.preferences.SessionCredentials
import com.example.data.preferences.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class ServerProbeResult(
    val normalizedRootUrl: String,
    val about: ServerAboutResponse?,
)

class ImmichRepository(
    private val clientFactory: ImmichClientFactory,
    private val preferences: SessionPreferences,
) {
    val sessionFlow: Flow<SessionCredentials> = preferences.sessionFlow

    fun updateActiveAuth(credentials: SessionCredentials) {
        clientFactory.authHolder.credentials = credentials
    }

    suspend fun probeServer(rawUrl: String): Result<ServerProbeResult> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = ImmichClientFactory.normalizeRootUrl(rawUrl)
            require(normalized.isNotBlank()) { "Ingresa la dirección de tu servidor Immich." }
            val service = clientFactory.getService(normalized)
            val ping = service.ping()
            require(ping.res.equals("pong", ignoreCase = true)) {
                "El servidor respondió, pero no parece ser una instancia de Immich."
            }
            val about = runCatching { service.getServerAbout() }.getOrNull()
            ServerProbeResult(normalizedRootUrl = normalized, about = about)
        }.mapFailure()
    }

    suspend fun loginWithCredentials(
        rawUrl: String,
        email: String,
        password: String,
    ): Result<SessionCredentials> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = ImmichClientFactory.normalizeRootUrl(rawUrl)
            require(normalized.isNotBlank()) { "Ingresa la URL del servidor." }
            require(email.isNotBlank() && password.isNotBlank()) {
                "Ingresa correo y contraseña."
            }
            clientFactory.authHolder.credentials = SessionCredentials(serverUrl = normalized)
            val service = clientFactory.getService(normalized)
            val response = service.login(LoginRequest(email.trim(), password))
            val creds = SessionCredentials(
                serverUrl = normalized,
                accessToken = response.accessToken,
                apiKey = "",
                userId = response.userId,
                userName = response.name,
                userEmail = response.userEmail,
                isAdmin = response.isAdmin,
            )
            clientFactory.authHolder.credentials = creds
            preferences.saveSession(creds)
            creds
        }.mapFailure()
    }

    suspend fun loginWithApiKey(
        rawUrl: String,
        apiKey: String,
    ): Result<SessionCredentials> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = ImmichClientFactory.normalizeRootUrl(rawUrl)
            require(normalized.isNotBlank()) { "Ingresa la URL del servidor." }
            require(apiKey.isNotBlank()) { "Ingresa una API Key válida." }
            val tempCreds = SessionCredentials(serverUrl = normalized, apiKey = apiKey.trim())
            clientFactory.authHolder.credentials = tempCreds
            val service = clientFactory.getService(normalized)
            val me = service.getMe()
            val creds = tempCreds.copy(
                userId = me.id,
                userName = me.name,
                userEmail = me.email,
                isAdmin = me.isAdmin,
            )
            clientFactory.authHolder.credentials = creds
            preferences.saveSession(creds)
            creds
        }.mapFailure()
    }

    suspend fun logout(serverUrl: String) = withContext(Dispatchers.IO) {
        runCatching {
            if (serverUrl.isNotBlank()) {
                clientFactory.getService(serverUrl).logout()
            }
        }
        clientFactory.authHolder.credentials = SessionCredentials(serverUrl = serverUrl)
        preferences.clearSession(keepServerUrl = true)
    }

    suspend fun getTimelineAssets(
        serverUrl: String,
        page: Int = 1,
        size: Int = 60,
        isFavorite: Boolean? = null,
        type: String? = null,
    ): Result<List<AssetDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = clientFactory.getService(serverUrl).searchMetadata(
                SearchMetadataRequest(
                    page = page,
                    size = size,
                    order = "desc",
                    isFavorite = isFavorite,
                    type = type,
                    withExif = true,
                )
            )
            response.assets.items.filterNot { it.isTrashed }
        }.mapFailure()
    }

    suspend fun searchSmart(
        serverUrl: String,
        query: String,
    ): Result<List<AssetDto>> = withContext(Dispatchers.IO) {
        runCatching {
            clientFactory.getService(serverUrl)
                .searchSmart(SearchSmartRequest(query = query.trim(), size = 60))
                .assets.items
        }.mapFailure()
    }

    suspend fun searchByMetadata(
        serverUrl: String,
        query: String,
    ): Result<List<AssetDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = query.trim()
            clientFactory.getService(serverUrl)
                .searchMetadata(
                    SearchMetadataRequest(
                        page = 1,
                        size = 60,
                        originalFileName = trimmed,
                        withExif = true,
                    )
                ).assets.items
        }.mapFailure()
    }

    suspend fun getAsset(serverUrl: String, assetId: String): Result<AssetDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                clientFactory.getService(serverUrl).getAsset(assetId)
            }.mapFailure()
        }

    suspend fun toggleFavorite(
        serverUrl: String,
        assetId: String,
        isFavorite: Boolean,
    ): Result<AssetDto> = withContext(Dispatchers.IO) {
        runCatching {
            clientFactory.getService(serverUrl).updateAsset(
                id = assetId,
                request = UpdateAssetRequest(isFavorite = isFavorite),
            )
        }.mapFailure()
    }

    suspend fun getAlbums(serverUrl: String): Result<List<AlbumDto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                clientFactory.getService(serverUrl).getAlbums()
            }.mapFailure()
        }

    suspend fun getAlbumDetail(serverUrl: String, albumId: String): Result<AlbumDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                clientFactory.getService(serverUrl).getAlbum(albumId)
            }.mapFailure()
        }

    suspend fun createAlbum(
        serverUrl: String,
        name: String,
        description: String?,
    ): Result<AlbumDto> = withContext(Dispatchers.IO) {
        runCatching {
            require(name.isNotBlank()) { "El nombre del álbum no puede estar vacío." }
            clientFactory.getService(serverUrl).createAlbum(
                CreateAlbumRequest(
                    albumName = name.trim(),
                    description = description?.trim()?.takeIf { it.isNotEmpty() },
                )
            )
        }.mapFailure()
    }

    suspend fun getServerProfileAndStats(
        serverUrl: String,
    ): Result<Triple<UserMeResponse, ServerAboutResponse?, ServerFeaturesResponse?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val service = clientFactory.getService(serverUrl)
                val me = service.getMe()
                val about = runCatching { service.getServerAbout() }.getOrNull()
                val features = runCatching { service.getServerFeatures() }.getOrNull()
                Triple(me, about, features)
            }.mapFailure()
        }

    suspend fun uploadPhotoFromUri(
        serverUrl: String,
        contentResolver: ContentResolver,
        uri: Uri,
    ): Result<UploadAssetResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val fileName = queryFileName(contentResolver, uri) ?: "upload_${System.currentTimeMillis()}.jpg"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("No se pudo leer el archivo seleccionado.")

            val filePart = MultipartBody.Part.createFormData(
                name = "assetData",
                filename = fileName,
                body = bytes.toRequestBody(mimeType.toMediaTypeOrNull()),
            )
            val nowIso = isoUtcNow()
            val textPlain = "text/plain".toMediaTypeOrNull()
            val deviceAssetId = "${fileName}-${bytes.size}-${UUID.randomUUID().toString().take(8)}"

            clientFactory.getService(serverUrl).uploadAsset(
                assetData = filePart,
                deviceAssetId = deviceAssetId.toRequestBody(textPlain),
                deviceId = "ImmichVault-Android".toRequestBody(textPlain),
                fileCreatedAt = nowIso.toRequestBody(textPlain),
                fileModifiedAt = nowIso.toRequestBody(textPlain),
            )
        }.mapFailure()
    }

    private fun queryFileName(contentResolver: ContentResolver, uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
    }

    private fun isoUtcNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun <T> Result<T>.mapFailure(): Result<T> {
        val ex = exceptionOrNull() ?: return this
        val friendly = when (ex) {
            is HttpException -> when (ex.code()) {
                401 -> "Sesión expirada o credenciales inválidas (401)."
                403 -> "Acceso denegado por el servidor Immich (403)."
                404 -> "Endpoint no encontrado (404). Verifica la URL base de Immich."
                else -> "Error del servidor Immich (${ex.code()})."
            }
            is IOException -> "No se pudo conectar con el servidor. Verifica la URL y tu conexión de red."
            else -> ex.localizedMessage ?: "Ocurrió un error inesperado."
        }
        return Result.failure(IllegalStateException(friendly, ex))
    }
}

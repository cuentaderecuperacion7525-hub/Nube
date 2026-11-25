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
            val (fileName, fileSize) = queryFileMeta(contentResolver, uri)
            val resolvedName = fileName ?: "upload_${System.currentTimeMillis()}.jpg"

            val streamingBody = object : okhttp3.RequestBody() {
                override fun contentType() = mimeType.toMediaTypeOrNull()

                override fun contentLength(): Long = fileSize ?: -1L

                override fun writeTo(sink: okio.BufferedSink) {
                    val input = contentResolver.openInputStream(uri)
                        ?: error("No se pudo abrir el archivo seleccionado.")
                    input.use { stream ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (stream.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                        }
                    }
                }
            }

            val filePart = MultipartBody.Part.createFormData(
                name = "assetData",
                filename = resolvedName,
                body = streamingBody,
            )
            val nowIso = isoUtcNow()
            val textPlain = "text/plain".toMediaTypeOrNull()
            val deviceAssetId = "${resolvedName}-${fileSize ?: 0L}-${UUID.randomUUID().toString().take(8)}"

            clientFactory.getService(serverUrl).uploadAsset(
                assetData = filePart,
                deviceAssetId = deviceAssetId.toRequestBody(textPlain),
                deviceId = "ImmichVault-Android".toRequestBody(textPlain),
                fileCreatedAt = nowIso.toRequestBody(textPlain),
                fileModifiedAt = nowIso.toRequestBody(textPlain),
            )
        }.mapFailure()
    }

    suspend fun downloadOriginalToDevice(
        serverUrl: String,
        context: android.content.Context,
        asset: AssetDto,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = ImmichClientFactory.originalUrl(serverUrl, asset.id)
            val request = okhttp3.Request.Builder().url(url).get().build()
            clientFactory.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Error al descargar original (${response.code}).")
                }
                val body = response.body ?: error("El servidor devolvió un archivo vacío.")
                val fileName = asset.originalFileName.ifBlank { "${asset.id}.jpg" }
                val mimeType = asset.originalMimeType
                    ?: if (asset.type.equals("VIDEO", ignoreCase = true)) "video/mp4" else "image/jpeg"

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val isVideo = asset.type.equals("VIDEO", ignoreCase = true)
                    val collection = if (isVideo) {
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val relativeDir = if (isVideo) {
                        "${android.os.Environment.DIRECTORY_MOVIES}/ImmichVault"
                    } else {
                        "${android.os.Environment.DIRECTORY_PICTURES}/ImmichVault"
                    }
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(collection, values)
                        ?: error("No se pudo crear el archivo en la galería.")
                    try {
                        resolver.openOutputStream(uri)?.use { out ->
                            body.byteStream().use { input ->
                                input.copyTo(out, bufferSize = 16 * 1024)
                            }
                        } ?: error("No se pudo escribir en la galería.")
                        values.clear()
                        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    } catch (t: Throwable) {
                        resolver.delete(uri, null, null)
                        throw t
                    }
                    "Guardado en $relativeDir/$fileName"
                } else {
                    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                    val outFile = java.io.File(dir, fileName)
                    outFile.outputStream().use { out ->
                        body.byteStream().use { input ->
                            input.copyTo(out, bufferSize = 16 * 1024)
                        }
                    }
                    "Guardado en ${outFile.absolutePath}"
                }
            }
        }.mapFailure()
    }

    private fun queryFileMeta(contentResolver: ContentResolver, uri: Uri): Pair<String?, Long?> {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else null
                name to size
            } else null to null
        } ?: (null to null)
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

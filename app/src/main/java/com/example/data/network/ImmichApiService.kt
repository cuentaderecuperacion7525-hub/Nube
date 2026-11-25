package com.example.data.network

import com.example.data.model.AlbumDto
import com.example.data.model.AssetDto
import com.example.data.model.CreateAlbumRequest
import com.example.data.model.LoginRequest
import com.example.data.model.LoginResponse
import com.example.data.model.SearchMetadataRequest
import com.example.data.model.SearchResponse
import com.example.data.model.SearchSmartRequest
import com.example.data.model.ServerAboutResponse
import com.example.data.model.ServerFeaturesResponse
import com.example.data.model.ServerPingResponse
import com.example.data.model.UpdateAssetRequest
import com.example.data.model.UploadAssetResponse
import com.example.data.model.UserMeResponse
import com.example.data.preferences.SessionCredentials
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface ImmichApiService {

    @GET("server/ping")
    suspend fun ping(): ServerPingResponse

    @GET("server/about")
    suspend fun getServerAbout(): ServerAboutResponse

    @GET("server/features")
    suspend fun getServerFeatures(): ServerFeaturesResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()

    @GET("users/me")
    suspend fun getMe(): UserMeResponse

    @POST("search/metadata")
    suspend fun searchMetadata(@Body request: SearchMetadataRequest): SearchResponse

    @POST("search/smart")
    suspend fun searchSmart(@Body request: SearchSmartRequest): SearchResponse

    @GET("assets/{id}")
    suspend fun getAsset(@Path("id") id: String): AssetDto

    @PUT("assets/{id}")
    suspend fun updateAsset(
        @Path("id") id: String,
        @Body request: UpdateAssetRequest,
    ): AssetDto

    @Multipart
    @POST("assets")
    suspend fun uploadAsset(
        @Part assetData: MultipartBody.Part,
        @Part("deviceAssetId") deviceAssetId: RequestBody,
        @Part("deviceId") deviceId: RequestBody,
        @Part("fileCreatedAt") fileCreatedAt: RequestBody,
        @Part("fileModifiedAt") fileModifiedAt: RequestBody,
    ): UploadAssetResponse

    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("albums/{id}")
    suspend fun getAlbum(@Path("id") id: String): AlbumDto

    @POST("albums")
    suspend fun createAlbum(@Body request: CreateAlbumRequest): AlbumDto
}

class ImmichAuthHolder {
    @Volatile
    var credentials: SessionCredentials = SessionCredentials()
}

class ImmichClientFactory(
    val authHolder: ImmichAuthHolder = ImmichAuthHolder(),
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val creds = authHolder.credentials
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/json")
        if (creds.apiKey.isNotBlank()) {
            builder.header("x-api-key", creds.apiKey.trim())
        } else if (creds.accessToken.isNotBlank()) {
            builder.header("Authorization", "Bearer ${creds.accessToken.trim()}")
        }
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var cachedBaseUrl: String? = null
    private var cachedService: ImmichApiService? = null

    fun getService(rawServerUrl: String): ImmichApiService {
        val apiBase = normalizeApiBaseUrl(rawServerUrl)
        val existing = cachedService
        if (existing != null && cachedBaseUrl == apiBase) {
            return existing
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(apiBase)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        val service = retrofit.create(ImmichApiService::class.java)
        cachedBaseUrl = apiBase
        cachedService = service
        return service
    }

    companion object {
        fun normalizeRootUrl(raw: String): String {
            var cleaned = raw.trim().trimEnd('/')
            if (cleaned.isEmpty()) return ""
            if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
                cleaned = "https://$cleaned"
            }
            if (cleaned.endsWith("/api")) {
                cleaned = cleaned.removeSuffix("/api")
            }
            return cleaned
        }

        fun normalizeApiBaseUrl(raw: String): String {
            val root = normalizeRootUrl(raw)
            require(root.isNotBlank()) { "La URL del servidor no puede estar vacía." }
            return "$root/api/"
        }

        fun thumbnailUrl(serverUrl: String, assetId: String, size: String = "thumbnail"): String {
            val root = normalizeRootUrl(serverUrl)
            return "$root/api/assets/$assetId/thumbnail?size=$size"
        }

        fun originalUrl(serverUrl: String, assetId: String): String {
            val root = normalizeRootUrl(serverUrl)
            return "$root/api/assets/$assetId/original"
        }
    }
}

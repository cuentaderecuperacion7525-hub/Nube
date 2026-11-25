package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Immich Server & Auth ---

@JsonClass(generateAdapter = true)
data class ServerPingResponse(
    val res: String = "",
)

@JsonClass(generateAdapter = true)
data class ServerAboutResponse(
    val version: String = "",
    val versionUrl: String? = null,
    val build: String? = null,
    val buildUrl: String? = null,
    val repository: String? = null,
    val repositoryUrl: String? = null,
    val licensed: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ServerFeaturesResponse(
    val smartSearch: Boolean = false,
    val facialRecognition: Boolean = false,
    val duplicateDetection: Boolean = false,
    val map: Boolean = false,
    val reverseGeocoding: Boolean = false,
    val trash: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val accessToken: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val name: String = "",
    val isAdmin: Boolean = false,
    val profileImagePath: String? = null,
    val shouldChangePassword: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class UserMeResponse(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val profileImagePath: String? = null,
    val isAdmin: Boolean = false,
    val quotaSizeInBytes: Long? = null,
    val quotaUsageInBytes: Long? = null,
    val createdAt: String? = null,
)

// --- Immich Assets & Search ---

@JsonClass(generateAdapter = true)
data class SearchMetadataRequest(
    val page: Int = 1,
    val size: Int = 60,
    val order: String = "desc",
    val isFavorite: Boolean? = null,
    val type: String? = null,
    val originalFileName: String? = null,
    val city: String? = null,
    val make: String? = null,
    val withExif: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class SearchSmartRequest(
    val query: String,
    val page: Int = 1,
    val size: Int = 60,
    val withExif: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val assets: SearchAssetsBucket = SearchAssetsBucket(),
)

@JsonClass(generateAdapter = true)
data class SearchAssetsBucket(
    val total: Int = 0,
    val count: Int = 0,
    val items: List<AssetDto> = emptyList(),
    val nextPage: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssetDto(
    val id: String = "",
    val deviceAssetId: String? = null,
    val ownerId: String? = null,
    val type: String = "IMAGE",
    val originalPath: String? = null,
    val originalFileName: String = "",
    val originalMimeType: String? = null,
    val fileCreatedAt: String = "",
    val fileModifiedAt: String? = null,
    val localDateTime: String? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val duration: String? = null,
    val thumbhash: String? = null,
    val checksum: String? = null,
    val exifInfo: ExifInfoDto? = null,
)

@JsonClass(generateAdapter = true)
data class ExifInfoDto(
    val make: String? = null,
    val model: String? = null,
    val lensModel: String? = null,
    val exifImageWidth: Int? = null,
    val exifImageHeight: Int? = null,
    val fileSizeInByte: Long? = null,
    val fileSize: Long? = null,
    val dateTimeOriginal: String? = null,
    val iso: Int? = null,
    val exposureTime: String? = null,
    val fNumber: Double? = null,
    val focalLength: Double? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val resolvedFileSize: Long?
        get() = fileSizeInByte ?: fileSize
}

@JsonClass(generateAdapter = true)
data class UpdateAssetRequest(
    val isFavorite: Boolean? = null,
    val isArchived: Boolean? = null,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class UploadAssetResponse(
    val id: String = "",
    val status: String = "",
    val duplicate: Boolean = false,
)

// --- Immich Albums ---

@JsonClass(generateAdapter = true)
data class AlbumDto(
    val id: String = "",
    val albumName: String = "",
    val description: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val albumThumbnailAssetId: String? = null,
    val assetCount: Int = 0,
    val shared: Boolean = false,
    val ownerId: String? = null,
    val assets: List<AssetDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CreateAlbumRequest(
    val albumName: String,
    val description: String? = null,
)

// --- GitHub Repositories (immich-app) ---

@JsonClass(generateAdapter = true)
data class GitHubRepoDto(
    val id: Long = 0L,
    val name: String = "",
    @Json(name = "full_name") val fullName: String = "",
    val description: String? = null,
    @Json(name = "html_url") val htmlUrl: String = "",
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "forks_count") val forksCount: Int = 0,
    @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
    val language: String? = null,
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "default_branch") val defaultBranch: String = "main",
    val topics: List<String> = emptyList(),
    val archived: Boolean = false,
    val license: GitHubLicenseDto? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubLicenseDto(
    val key: String? = null,
    val name: String? = null,
    @Json(name = "spdx_id") val spdxId: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String = "",
    val name: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String = "",
)

package com.example.data.network

import com.example.data.model.GitHubReleaseDto
import com.example.data.model.GitHubRepoDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GitHubApiService {

    @GET("orgs/immich-app/repos")
    suspend fun getImmichRepos(
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30,
    ): List<GitHubRepoDto>

    @GET("repos/immich-app/immich/releases/latest")
    suspend fun getLatestImmichRelease(): GitHubReleaseDto

    companion object {
        fun create(): GitHubApiService {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "ImmichVault-Android")
                        .build()
                    chain.proceed(req)
                }
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GitHubApiService::class.java)
        }
    }
}

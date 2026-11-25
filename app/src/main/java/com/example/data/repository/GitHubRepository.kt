package com.example.data.repository

import com.example.data.model.GitHubReleaseDto
import com.example.data.model.GitHubRepoDto
import com.example.data.network.GitHubApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GitHubEcosystemData(
    val repos: List<GitHubRepoDto>,
    val latestRelease: GitHubReleaseDto?,
)

class GitHubRepository(
    private val apiService: GitHubApiService,
) {
    suspend fun fetchImmichEcosystem(): Result<GitHubEcosystemData> = withContext(Dispatchers.IO) {
        runCatching {
            val repos = apiService.getImmichRepos(sort = "updated", perPage = 30)
                .sortedByDescending { it.stargazersCount }
            val latest = runCatching { apiService.getLatestImmichRelease() }.getOrNull()
            GitHubEcosystemData(repos = repos, latestRelease = latest)
        }
    }
}

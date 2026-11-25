package com.example.data.network

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient

object ImmichImageLoader {
    fun create(context: Context, okHttpClient: OkHttpClient): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("immich_image_cache"))
                    .maxSizeBytes(128L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

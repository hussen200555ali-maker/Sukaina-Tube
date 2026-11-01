package com.sukaina.tube

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.sukaina.tube.data.AppDatabase
import com.sukaina.tube.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SukainaApp : Application() {

    lateinit var settings: SettingsStore
        private set
    lateinit var database: AppDatabase
        private set

    /** نطاق عام للمهام التي يجب أن تكتمل حتى لو أُغلق الشاشة. */
    val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsStore(this)
        database = AppDatabase.get(this)
        installImageLoader()
    }

    private fun installImageLoader() {
        val loader = ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("thumbs"))
                    .maxSizeBytes(250L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(loader)
    }

    companion object {
        lateinit var instance: SukainaApp
            private set
    }
}

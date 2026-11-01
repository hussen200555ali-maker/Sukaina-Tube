package com.sukaina.tube.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.sukaina.tube.util.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * يقرأ فيديوهات الهاتف من MediaStore ويحفظها في حالة مشتركة بين الشاشات.
 */
object MediaRepo {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    private val _state = MutableStateFlow(LoadState.IDLE)
    val state: StateFlow<LoadState> = _state

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun refresh(context: Context) {
        job?.cancel()
        _state.value = LoadState.LOADING
        job = scope.launch {
            runCatching { queryVideos(context.applicationContext) }
                .onSuccess { list ->
                    _videos.value = list
                    _state.value = if (list.isEmpty()) LoadState.EMPTY else LoadState.READY
                    _error.value = null
                }
                .onFailure {
                    _state.value = LoadState.EMPTY
                    _error.value = it.message
                }
        }
    }

    fun setNoPermission() {
        _state.value = LoadState.NO_PERMISSION
    }

    fun findVideo(id: Long): VideoItem? = _videos.value.firstOrNull { it.id == id }

    /** بحث مباشر عن فيديو واحد في MediaStore (يُستخدم إذا أُعيد تشغيل التطبيق). */
    suspend fun querySingle(context: Context, id: Long): VideoItem? = withContext(Dispatchers.IO) {
        runCatching {
            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )
            context.contentResolver.query(
                collection, projection,
                "${MediaStore.Video.Media._ID} = ?", arrayOf(id.toString()), null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                VideoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = Fmt.prettyTitle(
                        cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)) ?: ""
                    ),
                    bucket = cursor.getString(
                        cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    ) ?: "",
                    durationMs = cursor.getLong(
                        cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                    ),
                    sizeBytes = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE)),
                    dateAddedSec = cursor.getLong(
                        cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                    ),
                    width = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)),
                    height = cursor.getInt(cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT))
                )
            }
        }.getOrNull()
    }

    fun folders(): List<String> =
        _videos.value.map { it.bucket }.filter { it.isNotBlank() }.distinct().sorted()

    /** كل مجلدات الفيديو في الجهاز (بدون تصفية) — تُستخدم في إعدادات ولي الأمر. */
    suspend fun allFolders(context: Context): List<String> = withContext(Dispatchers.IO) {
        val result = LinkedHashSet<String>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Video.Media.BUCKET_DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(index)
                    if (!name.isNullOrBlank()) result.add(name)
                }
            }
        }
        result.sorted()
    }

    fun byBucket(bucket: String): List<VideoItem> = _videos.value.filter { it.bucket == bucket }

    fun shorts(): List<VideoItem> = _videos.value.filter { it.isShort }

    fun others(currentId: Long, bucket: String?, limit: Int = 12): List<VideoItem> {
        val all = _videos.value.filter { it.id != currentId }
        val same = if (bucket != null) all.filter { it.bucket == bucket } else emptyList()
        val rest = all.filter { it.bucket != bucket }
        return (same + rest).take(limit)
    }

    private fun queryVideos(context: Context): List<VideoItem> {
        val allowed = com.sukaina.tube.SukainaApp.instance.settings.allowedBuckets

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val items = ArrayList<VideoItem>(256)

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                if (duration in 1..2_999) continue

                val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "" else ""
                if (allowed.isNotEmpty() && !allowed.contains(bucket)) continue

                val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                val uri = ContentUris.withAppendedId(collection, id)

                items.add(
                    VideoItem(
                        id = id,
                        uri = uri,
                        title = Fmt.prettyTitle(name),
                        bucket = bucket,
                        durationMs = duration,
                        sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                        dateAddedSec = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                        width = if (widthCol >= 0) cursor.getInt(widthCol) else 0,
                        height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    )
                )
            }
        }
        return items
    }
}

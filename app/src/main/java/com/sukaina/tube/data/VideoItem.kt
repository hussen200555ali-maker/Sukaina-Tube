package com.sukaina.tube.data

import android.net.Uri

/**
 * فيديو محلي مقروء من ذاكرة الهاتف عبر MediaStore.
 */
data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val bucket: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val width: Int,
    val height: Int
) {
    /** اسم القناة المعروض، مبني على اسم المجلد داخل الهاتف. */
    val channel: String
        get() = if (bucket.isBlank()) "Sukaina Tube" else "قناة $bucket"

    /** الحرف/الحرفان المستخدمان في صورة القناة الدائرية. */
    val channelInitial: String
        get() {
            val source = bucket.ifBlank { "Sukaina Tube" }.trim()
            return source.take(1).uppercase()
        }

    val isShort: Boolean
        get() = durationMs in 1..70_000
}

enum class LoadState { IDLE, LOADING, READY, EMPTY, NO_PERMISSION }

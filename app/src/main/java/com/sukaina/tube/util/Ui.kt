package com.sukaina.tube.util

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import coil.request.videoFrameMillis
import com.sukaina.tube.R
import com.sukaina.tube.data.VideoItem
import kotlin.math.abs

object Thumbs {

    /** تحميل صورة مصغّرة من داخل ملف الفيديو نفسه (مع كاش). */
    fun load(view: ImageView, video: VideoItem, widthPx: Int = 480) {
        view.load(video.uri) {
            size(widthPx, widthPx * 9 / 16)
            videoFrameMillis(700)
            crossfade(160)
            allowHardware(false)
        }
    }

    fun loadSmall(view: ImageView, video: VideoItem) {
        view.load(video.uri) {
            size(320, 180)
            videoFrameMillis(700)
            crossfade(120)
            allowHardware(false)
        }
    }
}

object Ui {

    private val palette = intArrayOf(
        0xFFB3261E.toInt(), 0xFF7B5EA7.toInt(), 0xFF1E6C4E.toInt(),
        0xFF1A5E9A.toInt(), 0xFF8A4B1E.toInt(), 0xFF0F7B7B.toInt(),
        0xFF8B2E5D.toInt(), 0xFF4A5A1E.toInt()
    )

    fun channelColor(bucket: String): Int =
        palette[abs(bucket.hashCode()) % palette.size]

    fun bindChannelAvatar(view: TextView, video: VideoItem) {
        val bg = android.graphics.drawable.GradientDrawable()
        bg.shape = android.graphics.drawable.GradientDrawable.OVAL
        bg.setColor(channelColor(video.bucket))
        view.background = bg
        view.setTextColor(0xFFFFFFFF.toInt())
        view.text = Fmt.initials(video.channelInitial)
    }

    fun metaLine(video: VideoItem): String {
        val views = Fmt.views(com.sukaina.tube.data.FakeStats.views(video.id))
        val ago = Fmt.timeAgo(video.dateAddedSec)
        return "${video.channel} · $views · $ago"
    }

    fun padTop(view: View) {
        val original = view.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, original + top, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun padBottom(view: View) {
        val original = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, original + bottom)
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun toastShort(view: View, res: Int) {
        android.widget.Toast.makeText(view.context, res, android.widget.Toast.LENGTH_SHORT).show()
    }
}

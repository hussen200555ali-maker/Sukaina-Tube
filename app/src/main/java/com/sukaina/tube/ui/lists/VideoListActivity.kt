package com.sukaina.tube.ui.lists

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sukaina.tube.R
import com.sukaina.tube.SukainaApp
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.data.VideoItem
import com.sukaina.tube.databinding.ActivityVideoListBinding
import com.sukaina.tube.ui.home.VideoCompactAdapter
import com.sukaina.tube.ui.watch.WatchActivity
import com.sukaina.tube.util.Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoListBinding
    private lateinit var adapter: VideoCompactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Ui.padTop(binding.topBar)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ALL
        val bucket = intent.getStringExtra(EXTRA_BUCKET)

        binding.title.text = when (mode) {
            MODE_HISTORY -> getString(R.string.lib_history)
            MODE_LIKED -> getString(R.string.lib_liked)
            MODE_WATCH_LATER -> getString(R.string.lib_watch_later)
            MODE_BUCKET -> getString(R.string.lib_playlists) + " · " + (bucket ?: "")
            else -> getString(R.string.chip_all)
        }

        adapter = VideoCompactAdapter { item ->
            startActivity(WatchActivity.intent(this, item.id))
        }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }

        load(mode, bucket)
    }

    private fun load(mode: String, bucket: String?) {
        lifecycleScope.launch {
            val items: List<VideoItem> = when (mode) {
                MODE_HISTORY -> withContext(Dispatchers.IO) {
                    val ids = (application as SukainaApp).database.dao().history().map { it.videoId }
                    ids.mapNotNull { MediaRepo.findVideo(it) }
                }
                MODE_LIKED -> withContext(Dispatchers.IO) {
                    val ids = (application as SukainaApp).database.dao().liked().map { it.videoId }
                    ids.mapNotNull { MediaRepo.findVideo(it) }
                }
                MODE_WATCH_LATER -> withContext(Dispatchers.IO) {
                    val ids = (application as SukainaApp).database.dao().watchLater().map { it.videoId }
                    ids.mapNotNull { MediaRepo.findVideo(it) }
                }
                MODE_BUCKET -> MediaRepo.byBucket(bucket ?: "")
                else -> MediaRepo.videos.value
            }
            adapter.submitList(items)
            binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    companion object {
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_BUCKET = "bucket"
        const val MODE_HISTORY = "history"
        const val MODE_LIKED = "liked"
        const val MODE_WATCH_LATER = "watch_later"
        const val MODE_BUCKET = "bucket"
        const val MODE_ALL = "all"

        private fun base(context: Context, mode: String, bucket: String? = null) =
            Intent(context, VideoListActivity::class.java).apply {
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_BUCKET, bucket)
            }

        fun historyIntent(context: Context) = base(context, MODE_HISTORY)
        fun likedIntent(context: Context) = base(context, MODE_LIKED)
        fun watchLaterIntent(context: Context) = base(context, MODE_WATCH_LATER)
        fun bucketIntent(context: Context, bucket: String) = base(context, MODE_BUCKET, bucket)
        fun allIntent(context: Context) = base(context, MODE_ALL)
    }
}

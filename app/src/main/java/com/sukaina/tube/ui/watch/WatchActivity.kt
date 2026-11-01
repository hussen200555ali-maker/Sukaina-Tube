package com.sukaina.tube.ui.watch

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.sukaina.tube.R
import com.sukaina.tube.SukainaApp
import com.sukaina.tube.data.FakeStats
import com.sukaina.tube.data.HistoryEntity
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.data.ReactionEntity
import com.sukaina.tube.data.VideoItem
import com.sukaina.tube.data.WatchLaterEntity
import com.sukaina.tube.databinding.ActivityWatchBinding
import com.sukaina.tube.databinding.ItemCommentBinding
import com.sukaina.tube.ui.home.VideoCompactAdapter
import com.sukaina.tube.util.Fmt
import com.sukaina.tube.util.Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class WatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWatchBinding
    private lateinit var player: ExoPlayer
    private lateinit var upNextAdapter: VideoCompactAdapter

    private var video: VideoItem? = null
    private var upNext: List<VideoItem> = emptyList()
    private var reactionValue = 0
    private var isFullscreen = false
    private var pendingSeek = 0L
    private var savedLabel = false

    private val app get() = application as SukainaApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        binding = ActivityWatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.controls.applyInsets()
        binding.controls.onBack = { goBack() }
        binding.controls.onFullscreenToggle = { setFullscreen(it) }
        binding.controls.setFullscreen(false)

        setupPlayer()
        setupDetails()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) setFullscreen(false) else finish()
            }
        })

        lifecycleScope.launch { loadVideo() }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && pendingSeek > 0) {
                        val target = pendingSeek
                        pendingSeek = 0
                        if (target < duration - 4000) seekTo(target)
                    }
                    if (playbackState == Player.STATE_ENDED) onFinished()
                }
            })
        }
        binding.playerView.player = player
        binding.controls.bind(player, "")
        binding.playerView.keepScreenOn = true
    }

    private suspend fun loadVideo() {
        val id = intent.getLongExtra(EXTRA_VIDEO_ID, -1L)
        if (id <= 0) {
            finish()
            return
        }
        var item = MediaRepo.findVideo(id)
        if (item == null) {
            item = withContext(Dispatchers.IO) { MediaRepo.querySingle(this@WatchActivity, id) }
        }
        if (item == null) {
            Toast.makeText(this, R.string.empty_title, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        play(item)
    }

    private fun play(item: VideoItem) {
        saveHistory()

        video = item
        binding.controls.bind(player, item.title)
        player.setMediaItem(MediaItem.fromUri(item.uri))
        player.prepare()
        player.playWhenReady = true

        fillDetails(item)

        upNext = MediaRepo.others(item.id, item.bucket, 10)
        upNextAdapter.submitList(upNext)

        lifecycleScope.launch {
            val dao = app.database.dao()
            val reaction = withContext(Dispatchers.IO) { dao.reactionFor(item.id) }
            val saved = withContext(Dispatchers.IO) { dao.inWatchLater(item.id) }
            val history = withContext(Dispatchers.IO) { dao.historyFor(item.id) }
            reactionValue = reaction?.value ?: 0
            savedLabel = saved
            updateReactionUi(item)
            updateSaveUi()
            pendingSeek = if (app.settings.resumePlayback) history?.positionMs ?: 0L else 0L
            if (pendingSeek > 0 && player.playbackState == Player.STATE_READY) {
                val target = pendingSeek
                pendingSeek = 0
                if (target < player.duration - 4000) player.seekTo(target)
            }
        }
    }

    private fun fillDetails(item: VideoItem) {
        val views = FakeStats.views(item.id)
        val subscribers = FakeStats.subscribers(item.bucket)

        binding.title.text = item.title
        binding.meta.text = "${item.channel} · ${Fmt.views(views)} · ${Fmt.timeAgo(item.dateAddedSec)}"
        binding.descriptionMeta.text =
            "${Fmt.views(views)} · ${Fmt.timeAgo(item.dateAddedSec)}"
        binding.channelName.text = item.channel
        binding.channelMeta.text = getString(R.string.watch_subscribers, Fmt.compact(subscribers))
        binding.commentsTitle.text =
            getString(R.string.watch_comments) + " " + Fmt.compact(FakeStats.likes(item.id) / 3)

        Ui.bindChannelAvatar(binding.channelAvatar, item)
        updateSubscribeUi(item)

        binding.commentsSection.visibility =
            if (app.settings.hideComments) View.GONE else View.VISIBLE

        buildComments(item)
        updateReactionUi(item)
    }

    private fun buildComments(item: VideoItem) {
        val container = binding.commentsContainer
        container.removeAllViews()
        FakeStats.commentsFor(item.id).forEach { comment ->
            val row = ItemCommentBinding.inflate(layoutInflater, container, false)
            row.author.text = "${comment.author} · ${Fmt.relativeFromHours(comment.hoursAgo)}"
            row.body.text = comment.text
            row.likes.text = comment.likes.toString()
            row.avatar.text = comment.author.take(1)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Ui.channelColor(comment.author))
            }
            row.avatar.background = bg
            container.addView(row.root)
        }
    }

    private fun updateReactionUi(item: VideoItem) {
        val base = FakeStats.likes(item.id)
        val extra = if (reactionValue == 1) 1 else 0
        binding.likeCount.text = Fmt.compact(base + extra)
        binding.iconLike.setImageResource(
            if (reactionValue == 1) R.drawable.ic_like else R.drawable.ic_like_outline
        )
        binding.iconLike.imageTintList = android.content.res.ColorStateList.valueOf(
            getColor(if (reactionValue == 1) R.color.like_active else R.color.text_primary)
        )
        binding.btnLike.isSelected = reactionValue == 1
    }

    private fun setupDetails() {
        binding.btnLike.setOnClickListener {
            reactionValue = if (reactionValue == 1) 0 else 1
            persistReaction()
        }
        binding.btnDislike.setOnClickListener {
            reactionValue = if (reactionValue == -1) 0 else -1
            persistReaction()
        }
        binding.btnShare.setOnClickListener {
            Toast.makeText(this, R.string.toast_unavailable, Toast.LENGTH_SHORT).show()
        }
        binding.btnDownload.setOnClickListener {
            Toast.makeText(this, R.string.toast_unavailable, Toast.LENGTH_SHORT).show()
        }
        binding.btnSave.setOnClickListener { toggleWatchLater() }
        binding.btnSubscribe.setOnClickListener { toggleSubscribe() }

        binding.descriptionBox.setOnClickListener {
            val body = binding.descriptionBody
            body.maxLines = if (body.maxLines == 2) 20 else 2
        }

        upNextAdapter = VideoCompactAdapter { item -> play(item) }
        binding.upNext.layoutManager = LinearLayoutManager(this)
        binding.upNext.adapter = upNextAdapter
        binding.upNext.isNestedScrollingEnabled = false
    }

    private fun persistReaction() {
        val item = video ?: return
        updateReactionUi(item)
        val value = reactionValue
        app.io.launch {
            val dao = app.database.dao()
            if (value == 0) dao.deleteReaction(item.id)
            else dao.upsertReaction(ReactionEntity(item.id, value, System.currentTimeMillis()))
        }
    }

    private fun toggleWatchLater() {
        val item = video ?: return
        savedLabel = !savedLabel
        updateSaveUi()
        val saved = savedLabel
        app.io.launch {
            val dao = app.database.dao()
            if (saved) dao.upsertWatchLater(WatchLaterEntity(item.id, System.currentTimeMillis()))
            else dao.deleteWatchLater(item.id)
        }
    }

    private fun updateSaveUi() {
        binding.saveLabel.setText(if (savedLabel) R.string.watch_saved else R.string.watch_save)
    }

    private fun toggleSubscribe() {
        val item = video ?: return
        val nowSubscribed = !app.settings.isSubscribed(item.bucket)
        app.settings.setSubscribed(item.bucket, nowSubscribed)
        updateSubscribeUi(item)
    }

    private fun updateSubscribeUi(item: VideoItem) {
        val subscribed = app.settings.isSubscribed(item.bucket)
        binding.btnSubscribe.text =
            getString(if (subscribed) R.string.watch_subscribed else R.string.watch_subscribe)
        binding.btnSubscribe.setBackgroundResource(
            if (subscribed) R.drawable.bg_pill else R.drawable.bg_button_red
        )
        binding.btnSubscribe.setTextColor(
            getColor(if (subscribed) R.color.text_secondary else R.color.white)
        )
    }

    private fun onFinished() {
        saveHistory()
        if (app.settings.autoPlayNext && upNext.isNotEmpty()) {
            play(upNext.first())
        } else {
            binding.controls.show()
        }
    }

    private fun saveHistory() {
        val item = video ?: return
        if (!this::player.isInitialized) return
        val position = player.currentPosition.coerceAtLeast(0)
        val duration = player.duration.coerceAtLeast(0)
        app.io.launch {
            app.database.dao().upsertHistory(
                HistoryEntity(item.id, System.currentTimeMillis(), position, duration)
            )
        }
    }

    private fun goBack() {
        if (isFullscreen) setFullscreen(false) else finish()
    }

    private fun setFullscreen(enabled: Boolean) {
        isFullscreen = enabled
        val lp = binding.playerContainer.layoutParams as ConstraintLayout.LayoutParams
        if (enabled) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            binding.content.visibility = View.GONE
            lp.dimensionRatio = null
            lp.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            lp.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            binding.content.visibility = View.VISIBLE
            lp.dimensionRatio = "16:9"
            lp.width = 0
            lp.height = 0
            lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            WindowInsetsControllerCompat(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
        binding.playerContainer.layoutParams = lp
        binding.controls.setFullscreen(enabled)
    }

    override fun onStop() {
        super.onStop()
        saveHistory()
        if (!isChangingConfigurations) player.pause()
    }

    override fun onDestroy() {
        saveHistory()
        binding.playerView.player = null
        binding.controls.release()
        player.release()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "video_id"

        fun intent(context: Context, videoId: Long): Intent =
            Intent(context, WatchActivity::class.java).putExtra(EXTRA_VIDEO_ID, videoId)
    }
}

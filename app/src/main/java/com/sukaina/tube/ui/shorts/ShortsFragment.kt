package com.sukaina.tube.ui.shorts

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.viewpager2.widget.ViewPager2
import com.sukaina.tube.R
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.data.VideoItem
import com.sukaina.tube.databinding.FragmentShortsBinding
import com.sukaina.tube.util.Ui
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ShortsFragment : Fragment(R.layout.fragment_shorts) {

    private var _binding: FragmentShortsBinding? = null
    private val binding get() = _binding!!

    private var exoPlayer: ExoPlayer? = null
    private lateinit var adapter: ShortsAdapter
    private var items: List<VideoItem> = emptyList()
    private var lastIds: List<Long> = emptyList()

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            activate(position)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentShortsBinding.bind(view)

        adapter = ShortsAdapter(
            onTap = { togglePlay() },
            onUnavailable = { Ui.toastShort(view, R.string.toast_unavailable) }
        )
        adapter.registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                exoPlayer?.pause()
            }
        })

        binding.pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.pager.offscreenPageLimit = 1
        binding.pager.adapter = adapter
        binding.pager.registerOnPageChangeCallback(pageCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    MediaRepo.videos.collect { list ->
                        val shorts = list.filter { it.isShort }
                        val ids = shorts.map { it.id }
                        if (ids != lastIds) {
                            lastIds = ids
                            items = shorts
                            adapter.submit(shorts)
                            binding.emptyState.visibility =
                                if (shorts.isEmpty()) View.VISIBLE else View.GONE
                            if (shorts.isNotEmpty()) activate(0)
                        }
                    }
                }
                launch {
                    while (isActive) {
                        updateProgress()
                        delay(250)
                    }
                }
            }
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        val player = ExoPlayer.Builder(requireContext()).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    _binding?.loading?.visibility =
                        if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    adapter.isPlaying = isPlaying
                }
            })
        }
        exoPlayer = player
        return player
    }

    private fun activate(position: Int) {
        if (items.isEmpty() || position !in items.indices) return
        val item = items[position]
        val player = ensurePlayer()
        player.setMediaItem(MediaItem.fromUri(item.uri))
        player.prepare()
        player.playWhenReady = true

        adapter.player = player
        adapter.activePosition = position
        adapter.notifyItemChanged(position)
        adapter.refreshActive()
    }

    private fun togglePlay() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    private fun updateProgress() {
        val player = exoPlayer ?: return
        val holder = adapter.activeHolder ?: return
        val duration = player.duration
        if (duration > 0) {
            val fraction = (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            holder.binding.progress.progress = (fraction * 1000).toInt()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            exoPlayer?.pause()
        } else if (items.isNotEmpty()) {
            exoPlayer?.play()
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroyView() {
        binding.pager.unregisterOnPageChangeCallback(pageCallback)
        binding.pager.adapter = null
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
        super.onDestroyView()
    }
}

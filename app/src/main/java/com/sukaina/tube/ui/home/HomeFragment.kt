package com.sukaina.tube.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.sukaina.tube.R
import com.sukaina.tube.SukainaApp
import com.sukaina.tube.data.HistoryEntity
import com.sukaina.tube.data.LoadState
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.data.VideoItem
import com.sukaina.tube.data.WatchLaterEntity
import com.sukaina.tube.databinding.FragmentHomeBinding
import com.sukaina.tube.ui.watch.WatchActivity
import com.sukaina.tube.util.Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: VideoCardAdapter
    private var allVideos: List<VideoItem> = emptyList()
    private var selectedBucket: String? = null
    private var chipsSignature: List<String> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)

        adapter = VideoCardAdapter(
            onClick = { watch(it) },
            onWatchLater = { addWatchLater(it) },
            onShare = { Ui.toastShort(view, R.string.toast_unavailable) }
        )

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(false)

        binding.refresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.yt_red)
        )
        binding.refresh.setOnRefreshListener {
            MediaRepo.refresh(requireContext())
            view.postDelayed({ _binding?.refresh?.isRefreshing = false }, 900)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    MediaRepo.videos.collect { videos ->
                        allVideos = videos
                        buildChips(MediaRepo.folders())
                        applyFilter()
                    }
                }
                launch {
                    MediaRepo.state.collect { renderState(it) }
                }
            }
        }
    }

    private fun renderState(state: LoadState) {
        val b = _binding ?: return
        b.progress.visibility = if (state == LoadState.LOADING) View.VISIBLE else View.GONE
        val empty = state == LoadState.EMPTY && allVideos.isEmpty()
        b.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        b.refresh.visibility = if (empty) View.INVISIBLE else View.VISIBLE
        if (state == LoadState.EMPTY) {
            b.emptyTitle.text = getString(R.string.empty_title)
            b.emptyBody.text = getString(R.string.empty_body)
        }
    }

    private fun buildChips(folders: List<String>) {
        val b = _binding ?: return
        val signature = listOf("__all__") + folders
        if (signature == chipsSignature) {
            updateChipSelection()
            return
        }
        chipsSignature = signature
        if (selectedBucket != null && !folders.contains(selectedBucket)) selectedBucket = null

        b.chipsContainer.removeAllViews()
        b.chipsContainer.addView(createChip(getString(R.string.chip_all), null))
        folders.forEach { folder ->
            b.chipsContainer.addView(createChip(folder, folder))
        }
        updateChipSelection()
    }

    private fun createChip(label: String, bucket: String?): TextView {
        val chip = TextView(requireContext()).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            includeFontPadding = false
            setPadding(dp(14), 0, dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(32)
            ).also { it.marginEnd = dp(8) }
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color)!!)
            setOnClickListener {
                selectedBucket = bucket
                updateChipSelection()
                applyFilter()
            }
        }
        chip.tag = bucket
        return chip
    }

    private fun updateChipSelection() {
        val container = _binding?.chipsContainer ?: return
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) as? TextView ?: continue
            child.isSelected = child.tag == selectedBucket
        }
    }

    private fun applyFilter() {
        val list = if (selectedBucket == null) {
            allVideos
        } else {
            allVideos.filter { it.bucket == selectedBucket }
        }
        adapter.submitList(list)
        val empty = list.isEmpty() && allVideos.isNotEmpty()
        _binding?.emptyState?.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun watch(item: VideoItem) {
        startActivity(WatchActivity.intent(requireContext(), item.id))
    }

    private fun addWatchLater(item: VideoItem) {
        val app = requireActivity().application as SukainaApp
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                app.database.dao().upsertWatchLater(
                    WatchLaterEntity(item.id, System.currentTimeMillis())
                )
            }
            Ui.toastShort(requireView(), R.string.watch_saved)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        binding.list.adapter = null
        _binding = null
        super.onDestroyView()
    }
}

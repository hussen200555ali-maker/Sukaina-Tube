package com.sukaina.tube.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sukaina.tube.R
import com.sukaina.tube.SukainaApp
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.databinding.FragmentLibraryBinding
import com.sukaina.tube.databinding.ItemLibraryRowBinding
import com.sukaina.tube.ui.lists.VideoListActivity
import com.sukaina.tube.ui.parent.ParentPinActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val app get() = requireActivity().application as SukainaApp

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentLibraryBinding.bind(view)

        binding.rowHistory.icon.setImageResource(R.drawable.ic_history)
        binding.rowHistory.title.setText(R.string.lib_history)
        binding.rowHistory.root.setOnClickListener {
            startActivity(VideoListActivity.historyIntent(requireContext()))
        }

        binding.rowLiked.icon.setImageResource(R.drawable.ic_like)
        binding.rowLiked.title.setText(R.string.lib_liked)
        binding.rowLiked.root.setOnClickListener {
            startActivity(VideoListActivity.likedIntent(requireContext()))
        }

        binding.rowWatchLater.icon.setImageResource(R.drawable.ic_playlist)
        binding.rowWatchLater.title.setText(R.string.lib_watch_later)
        binding.rowWatchLater.root.setOnClickListener {
            startActivity(VideoListActivity.watchLaterIntent(requireContext()))
        }

        binding.btnSettings.setOnClickListener { openParent() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MediaRepo.videos.collect { buildFolders() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCounts()
    }

    private fun openParent() {
        startActivity(Intent(requireContext(), ParentPinActivity::class.java))
    }

    private fun buildFolders() {
        val b = _binding ?: return
        b.folderRows.removeAllViews()
        val folders = MediaRepo.folders()
        folders.forEach { bucket ->
            val row = ItemLibraryRowBinding.inflate(
                LayoutInflater.from(requireContext()), b.folderRows, false
            )
            row.icon.setImageResource(R.drawable.ic_folder)
            row.title.text = "قناة $bucket"
            row.count.text = MediaRepo.byBucket(bucket).size.toString()
            row.root.setOnClickListener {
                startActivity(VideoListActivity.bucketIntent(requireContext(), bucket))
            }
            b.folderRows.addView(row.root)
        }
    }

    private fun refreshCounts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val counts = withContext(Dispatchers.IO) {
                val dao = app.database.dao()
                Triple(dao.history().size, dao.liked().size, dao.watchLater().size)
            }
            val b = _binding ?: return@launch
            b.rowHistory.count.text = counts.first.toString()
            b.rowLiked.count.text = counts.second.toString()
            b.rowWatchLater.count.text = counts.third.toString()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

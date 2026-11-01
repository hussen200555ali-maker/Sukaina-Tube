package com.sukaina.tube.ui.subs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sukaina.tube.R
import com.sukaina.tube.data.FakeStats
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.databinding.FragmentSubsBinding
import com.sukaina.tube.databinding.ItemChannelBinding
import com.sukaina.tube.ui.lists.VideoListActivity
import com.sukaina.tube.util.Fmt
import com.sukaina.tube.util.Ui
import kotlinx.coroutines.launch

class SubscriptionsFragment : Fragment(R.layout.fragment_subs) {

    private var _binding: FragmentSubsBinding? = null
    private val binding get() = _binding!!
    private val adapter = ChannelAdapter { bucket ->
        startActivity(VideoListActivity.bucketIntent(requireContext(), bucket))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSubsBinding.bind(view)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                MediaRepo.videos.collect {
                    val folders = MediaRepo.folders()
                    adapter.submit(folders)
                    binding.emptyState.visibility =
                        if (folders.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.list.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private class ChannelAdapter(
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<ChannelAdapter.VH>() {

        private var folders: List<String> = emptyList()

        fun submit(list: List<String>) {
            folders = list
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = folders.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemChannelBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val bucket = folders[position]
            val count = MediaRepo.byBucket(bucket).size
            val subs = FakeStats.subscribers(bucket)
            holder.binding.name.text = "قناة $bucket"
            holder.binding.meta.text =
                "${Fmt.compact(subs)} مشترك · $count فيديو"

            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Ui.channelColor(bucket))
            }
            holder.binding.avatar.background = bg
            holder.binding.avatar.text = bucket.take(1).uppercase()

            holder.binding.root.setOnClickListener { onClick(bucket) }
        }

        class VH(val binding: ItemChannelBinding) : RecyclerView.ViewHolder(binding.root)
    }
}

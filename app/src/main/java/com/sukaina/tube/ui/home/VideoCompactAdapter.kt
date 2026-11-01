package com.sukaina.tube.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sukaina.tube.data.VideoItem
import com.sukaina.tube.databinding.ItemVideoCompactBinding
import com.sukaina.tube.util.Fmt
import com.sukaina.tube.util.Thumbs
import com.sukaina.tube.util.Ui

class VideoCompactAdapter(
    private val onClick: (VideoItem) -> Unit
) : ListAdapter<VideoItem, VideoCompactAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVideoCompactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemVideoCompactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoItem) {
            binding.title.text = item.title
            binding.meta.text =
                "${item.channel} · ${Fmt.views(com.sukaina.tube.data.FakeStats.views(item.id))}"
            binding.duration.text = Fmt.duration(item.durationMs)
            Thumbs.loadSmall(binding.thumb, item)
            binding.root.setOnClickListener { onClick(item) }
            binding.more.setOnClickListener {
                Ui.toastShort(binding.root, com.sukaina.tube.R.string.toast_unavailable)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VideoItem>() {
            override fun areItemsTheSame(a: VideoItem, b: VideoItem) = a.id == b.id
            override fun areContentsTheSame(a: VideoItem, b: VideoItem) = a == b
        }
    }
}

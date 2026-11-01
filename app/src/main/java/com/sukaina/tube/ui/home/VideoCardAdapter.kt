package com.sukaina.tube.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sukaina.tube.R
import com.sukaina.tube.data.VideoItem
import com.sukaina.tube.databinding.ItemVideoCardBinding
import com.sukaina.tube.util.Fmt
import com.sukaina.tube.util.Thumbs
import com.sukaina.tube.util.Ui

class VideoCardAdapter(
    private val onClick: (VideoItem) -> Unit,
    private val onWatchLater: (VideoItem) -> Unit,
    private val onShare: (VideoItem) -> Unit
) : ListAdapter<VideoItem, VideoCardAdapter.VH>(DIFF) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemVideoCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemVideoCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VideoItem) {
            binding.title.text = item.title
            binding.meta.text = Ui.metaLine(item)
            binding.duration.text = Fmt.duration(item.durationMs)
            Ui.bindChannelAvatar(binding.avatar, item)

            Thumbs.load(binding.thumb, item, binding.root.resources.displayMetrics.widthPixels)

            binding.root.setOnClickListener { onClick(item) }

            binding.more.setOnClickListener { anchor ->
                val menu = PopupMenu(binding.root.context, anchor)
                menu.inflate(R.menu.video_card_menu)
                menu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_watch_later -> {
                            onWatchLater(item)
                            true
                        }
                        R.id.action_share -> {
                            onShare(item)
                            true
                        }
                        else -> {
                            Ui.toastShort(binding.root, R.string.toast_unavailable)
                            true
                        }
                    }
                }
                menu.show()
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

package com.sukaina.tube.ui.shorts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.Player
import androidx.recyclerview.widget.RecyclerView
import com.sukaina.tube.R
import com.sukaina.tube.data.FakeStats
import com.sukaina.tube.data.VideoItem
import com.sukaina.tube.databinding.ItemShortBinding
import com.sukaina.tube.util.Fmt
import com.sukaina.tube.util.Thumbs
import com.sukaina.tube.util.Ui

class ShortsAdapter(
    private val onTap: (Int) -> Unit,
    private val onUnavailable: () -> Unit
) : RecyclerView.Adapter<ShortsAdapter.VH>() {

    private var items: List<VideoItem> = emptyList()
    var player: Player? = null
    var activePosition = 0
    var isPlaying = true
        set(value) {
            field = value
            activeHolder?.binding?.playPauseIcon?.visibility =
                if (value) View.GONE else View.VISIBLE
        }

    var activeHolder: VH? = null
        private set

    fun submit(list: List<VideoItem>) {
        items = list
        activePosition = 0
        activeHolder = null
        notifyDataSetChanged()
    }

    fun refreshActive() {
        val holder = activeHolder ?: return
        holder.binding.playPauseIcon.visibility = if (isPlaying) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemShortBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.binding.title.text = item.title
        holder.binding.channel.text = item.channel
        holder.binding.likeCount.text = Fmt.compact(FakeStats.likes(item.id))
        holder.binding.playPauseIcon.visibility = View.GONE

        val avatar = holder.binding.avatar
        Ui.bindChannelAvatar(avatar, item)
        avatar.text = item.channelInitial.ifBlank { "S" }

        val isActive = position == activePosition
        if (isActive) {
            if (activeHolder !== holder) {
                activeHolder?.binding?.player?.player = null
                activeHolder = holder
            }
            holder.binding.player.player = player
            holder.binding.playPauseIcon.visibility = if (isPlaying) View.GONE else View.VISIBLE
        } else if (activeHolder === holder) {
            holder.binding.player.player = null
            activeHolder = null
        }

        holder.binding.tapLayer.setOnClickListener { onTap(position) }

        holder.binding.btnLike.setOnClickListener {
            it.isSelected = !it.isSelected
            holder.binding.btnLike.setImageResource(
                if (it.isSelected) R.drawable.ic_like else R.drawable.ic_like_outline
            )
            holder.binding.btnLike.imageTintList =
                android.content.res.ColorStateList.valueOf(
                    if (it.isSelected) holder.binding.root.context.getColor(R.color.yt_red)
                    else holder.binding.root.context.getColor(R.color.white)
                )
        }
        holder.binding.btnLike.setImageResource(R.drawable.ic_like_outline)
        holder.binding.btnLike.imageTintList =
            android.content.res.ColorStateList.valueOf(
                holder.binding.root.context.getColor(R.color.white)
            )

        holder.binding.btnDislike.setOnClickListener { onUnavailable() }
        holder.binding.btnComments.setOnClickListener { onUnavailable() }
        holder.binding.btnShare.setOnClickListener { onUnavailable() }
    }

    override fun onViewRecycled(holder: VH) {
        if (activeHolder === holder) {
            holder.binding.player.player = null
            activeHolder = null
        }
        super.onViewRecycled(holder)
    }

    class VH(val binding: ItemShortBinding) : RecyclerView.ViewHolder(binding.root)
}

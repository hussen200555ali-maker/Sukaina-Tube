package com.sukaina.tube.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.media3.common.Player
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sukaina.tube.R
import com.sukaina.tube.databinding.ViewPlayerControlsBinding
import com.sukaina.tube.util.Fmt
import kotlin.math.abs
import kotlin.math.max

/**
 * تحكم مشغّل بشكل يوتيوب: اخفاء تلقائي، ضغطة مزدوجة ±10 ثوانٍ، شريط أحمر، سرعة، وملء الشاشة.
 */
class YtPlayerControls @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = ViewPlayerControlsBinding.inflate(LayoutInflater.from(context), this)

    private var player: Player? = null
    private val handler = Handler(Looper.getMainLooper())
    private var controlsVisible = true
    var isFullscreen: Boolean = false
        private set

    var onBack: (() -> Unit)? = null
    var onFullscreenToggle: ((Boolean) -> Unit)? = null

    private val hideRunnable = Runnable { hide() }

    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 300)
        }
    }

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggle()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                seekBy(e.x)
                return true
            }
        }
    )

    @SuppressLint("ClickableViewAccessibility")
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updatePlayIcon()

        override fun onPlaybackStateChanged(playbackState: Int) {
            binding.buffering.visibility =
                if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
            if (playbackState == Player.STATE_READY && player?.isPlaying == true) {
                scheduleHide()
            }
            updatePlayIcon()
        }
    }

    init {
        setBackgroundColor(0x00000000)

        binding.touchLayer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        binding.btnPlayPause.setOnClickListener { togglePlay() }
        binding.btnBack.setOnClickListener { onBack?.invoke() }
        binding.btnFullscreen.setOnClickListener { toggleFullscreen() }
        binding.btnSpeed.setOnClickListener { showSpeedMenu(it) }

        binding.timeBar.setPlayer(null)
    }

    fun bind(player: Player?, title: String) {
        this.player?.removeListener(playerListener)
        this.player = player
        binding.controlsTitle.text = title
        binding.timeBar.setPlayer(player)
        player?.addListener(playerListener)
        updatePlayIcon()
        show()
    }

    fun setFullscreen(enabled: Boolean) {
        isFullscreen = enabled
        binding.btnFullscreen.setImageResource(
            if (enabled) R.drawable.ic_fullscreen_exit_white else R.drawable.ic_fullscreen_white
        )
    }

    /** يمنع الشريط العلوي/السفلي من الدخول تحت شريط الحالة وأزرار النظام. */
    fun applyInsets() {
        val density = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            binding.topRow.layoutParams = binding.topRow.layoutParams.also {
                it.height = (56 * density).toInt() + top
            }
            binding.topRow.setPadding(
                binding.topRow.paddingLeft, top,
                binding.topRow.paddingRight, binding.topRow.paddingBottom
            )
            binding.bottomRow.setPadding(
                binding.bottomRow.paddingLeft,
                binding.bottomRow.paddingTop,
                binding.bottomRow.paddingRight,
                (6 * density).toInt() + bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    fun show() {
        controlsVisible = true
        binding.topRow.visibility = View.VISIBLE
        binding.bottomRow.visibility = View.VISIBLE
        binding.btnPlayPause.visibility = View.VISIBLE
        scheduleHide()
    }

    fun hide() {
        controlsVisible = false
        binding.topRow.visibility = View.GONE
        binding.bottomRow.visibility = View.GONE
        binding.btnPlayPause.visibility = View.GONE
        handler.removeCallbacks(hideRunnable)
    }

    fun toggle() {
        if (controlsVisible) hide() else show()
    }

    private fun scheduleHide() {
        handler.removeCallbacks(hideRunnable)
        val playing = player?.isPlaying == true
        if (playing) {
            handler.postDelayed(hideRunnable, 3200)
        }
    }

    private fun togglePlay() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            show()
        } else {
            p.play()
            scheduleHide()
        }
        updatePlayIcon()
    }

    private fun updatePlayIcon() {
        val playing = player?.isPlaying == true
        binding.btnPlayPause.setImageResource(
            if (playing) R.drawable.ic_pause_white else R.drawable.ic_play_white
        )
        if (!playing) {
            binding.btnPlayPause.visibility = View.VISIBLE
            handler.removeCallbacks(hideRunnable)
        }
    }

    private fun updateProgress() {
        val p = player ?: return
        val position = p.currentPosition.coerceAtLeast(0)
        val duration = p.duration
        binding.position.text = Fmt.duration(position)
        binding.duration.text = if (duration > 0) Fmt.duration(duration) else "--:--"
    }

    private fun seekBy(tapX: Float) {
        val p = player ?: return
        val duration = p.duration
        val forward = tapX > width / 2f
        val delta = if (forward) 10_000L else -10_000L
        val target = (p.currentPosition + delta).coerceIn(0L, max(0L, duration))
        p.seekTo(target)
        showSeekIndicator(forward)
        show()
    }

    private fun showSeekIndicator(forward: Boolean) {
        binding.seekIndicator.visibility = View.VISIBLE
        binding.seekAmount.text =
            if (forward) "10 ثوانٍ للأمام ⏩" else "⏪ 10 ثوانٍ للخلف"
        handler.removeCallbacks(seekHideRunnable)
        handler.postDelayed(seekHideRunnable, 650)
    }

    private val seekHideRunnable = Runnable { binding.seekIndicator.visibility = View.GONE }

    private fun toggleFullscreen() {
        setFullscreen(!isFullscreen)
        onFullscreenToggle?.invoke(isFullscreen)
        show()
    }

    private fun showSpeedMenu(anchor: View) {
        val p = player ?: return
        val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
        val menu = PopupMenu(context, anchor)
        speeds.forEachIndexed { index, speed ->
            menu.menu.add(0, index, index, if (speed == 1f) "عادي" else "${speed}x")
        }
        menu.setOnMenuItemClickListener { item ->
            val speed = speeds[item.itemId]
            p.setPlaybackSpeed(speed)
            binding.btnSpeed.text = if (speed == 1f) "1x" else "${speed}x"
            show()
            true
        }
        menu.show()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(progressRunnable)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        player?.removeListener(playerListener)
        binding.timeBar.setPlayer(null)
        player = null
    }

    private fun approx(value: Float): String = if (abs(value - value.toInt()) < 0.01f) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

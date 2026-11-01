package com.sukaina.tube.util

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * حاوية تحافظ على نسبة عرض/ارتفاع 16:9 مثل بطاقات يوتيوب.
 */
class SixteenNineLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = if (width > 0) (width * 9f / 16f).toInt() else MeasureSpec.getSize(heightMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
    }
}

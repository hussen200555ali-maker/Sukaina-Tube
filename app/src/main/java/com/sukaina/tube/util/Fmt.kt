package com.sukaina.tube.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object Fmt {

    /** 1.2 ألف / 15 ألف / 1.2 مليون */
    fun compact(value: Long): String {
        val n = value.coerceAtLeast(0)
        return when {
            n < 1_000 -> n.toString()
            n < 1_000_000 -> trimDecimal(n / 1_000.0) + " ألف"
            n < 1_000_000_000 -> trimDecimal(n / 1_000_000.0) + " مليون"
            else -> trimDecimal(n / 1_000_000_000.0) + " مليار"
        }
    }

    fun views(value: Long): String = compact(value) + " مشاهدة"

    fun likes(value: Long): String = compact(value)

    private fun trimDecimal(v: Double): String {
        val s = String.format(Locale.US, "%.1f", v)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    /** 3:45 / 1:02:03 */
    fun duration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    /** قبل 3 أيام، قبل أسبوعين، قبل شهر... */
    fun timeAgo(dateAddedSec: Long): String {
        if (dateAddedSec <= 0) return "قبل فترة"
        val now = System.currentTimeMillis() / 1000
        val diff = (now - dateAddedSec).coerceAtLeast(0)

        val minutes = diff / 60
        val hours = diff / 3600
        val days = TimeUnit.SECONDS.toDays(diff)
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            minutes < 1 -> "قبل لحظات"
            minutes < 60 -> "قبل $minutes دقيقة"
            hours < 24 -> "قبل $hours ساعة"
            days == 1L -> "قبل يوم"
            days < 7 -> "قبل $days أيام"
            weeks == 1L -> "قبل أسبوع"
            weeks == 2L -> "قبل أسبوعين"
            weeks < 5 -> "قبل $weeks أسابيع"
            months <= 1 -> "قبل شهر"
            months == 2L -> "قبل شهرين"
            months < 12 -> "قبل $months أشهر"
            years == 1L -> "قبل سنة"
            years == 2L -> "قبل سنتين"
            else -> "قبل $years سنوات"
        }
    }

    fun relativeFromHours(hours: Int): String = when {
        hours < 1 -> "قبل لحظات"
        hours < 24 -> "قبل $hours ساعة"
        hours < 24 * 7 -> "قبل ${hours / 24} يوم"
        else -> "قبل ${hours / 24 / 7} أسبوع"
    }

    fun speedLabel(speed: Float): String =
        if (speed == 1f) "عادي" else trimDecimal(speed.toDouble()) + "x"

    /** تنظيف اسم الملف ليصبح عنواناً معقولاً. */
    fun prettyTitle(displayName: String): String {
        var name = displayName.substringBeforeLast('.')
        name = name.replace('_', ' ').replace('.', ' ')
        name = name.replace(Regex("\\s+"), " ").trim()
        return name.ifBlank { "فيديو" }
    }

    fun initials(text: String): String {
        val first = text.trim().firstOrNull()?.uppercaseChar() ?: 'S'
        return first.toString()
    }
}

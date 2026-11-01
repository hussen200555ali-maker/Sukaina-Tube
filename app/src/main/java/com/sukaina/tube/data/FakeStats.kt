package com.sukaina.tube.data

/**
 * أرقام "وهمية" ثابتة لكل فيديو، مولّدة من معرّفه حتى لا تتغير عند كل فتح للتطبيق.
 */
object FakeStats {

    private fun mix(value: Long): Long {
        var x = value * -0x61c8864680b583ebL + 0x3c6ef372fe94f82bL
        x = x xor (x ushr 33)
        x *= -0x40a7b892e31b1a47L
        x = x xor (x ushr 29)
        x *= -0x6b2fb644ecceee15L
        x = x xor (x ushr 32)
        return if (x == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(x)
    }

    private fun seed(text: String): Long {
        var h = 1125899906842597L
        for (c in text) h = 31 * h + c.code
        return mix(h)
    }

    fun views(id: Long): Long = 1_200L + (mix(id * 31 + 7) % 4_998_800L)

    fun likes(id: Long): Long = (views(id) / 47L).coerceAtLeast(12L)

    fun subscribers(bucket: String): Long = 12_400L + (seed(bucket) % 1_987_600L)

    /** تعليقات وهمية ثابتة تُختار حسب معرّف الفيديو. */
    private val comments = listOf(
        "ما شاء الله، فيديو رائع جداً 🌟",
        "جزاك الله خيراً على هذا المحتوى",
        "أول مرة أشاهد شيئاً بهذا الجمال",
        "شكراً على المجهود، استمروا ❤️",
        "أطفالي يحبون هذا الفيديو كثيراً",
        "اللهم بارك، محتوى مفيد وممتع",
        "من أفضل ما شاهدت هذا الأسبوع",
        "الفيديو جميل جداً، أعدت مشاهدته مرتين",
        "تبارك الرحمن، إبداع حقيقي",
        "أتمنى المزيد من هذه الفيديوهات"
    )

    private val commenters = listOf(
        "أم عبدالله", "خالد الحربي", "نورة", "سارة محمد", "أبو ريان",
        "فاطمة", "عبدالعزيز", "ريم", "محمد الشامي", "لمى"
    )

    data class FakeComment(val author: String, val text: String, val likes: Int, val hoursAgo: Int)

    fun commentsFor(id: Long): List<FakeComment> {
        val base = (mix(id) % comments.size).toInt()
        return (0 until 3).map { i ->
            val c = comments[(base + i * 3) % comments.size]
            val a = commenters[(base + i * 4 + 1) % commenters.size]
            FakeComment(
                author = a,
                text = c,
                likes = ((mix(id + i * 977) % 900L) + 3L).toInt(),
                hoursAgo = ((mix(id + i * 131) % 700L) + 2L).toInt()
            )
        }
    }
}

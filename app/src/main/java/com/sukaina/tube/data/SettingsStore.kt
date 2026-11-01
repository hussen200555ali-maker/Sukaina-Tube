package com.sukaina.tube.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

/**
 * إعدادات التطبيق (تُخزَّن في SharedPreferences) + رمز ولي الأمر.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("sukaina_settings", Context.MODE_PRIVATE)

    private val _revision = MutableStateFlow(0L)
    /** يتغير رقمه عند أي تعديل في الإعدادات حتى تعيد الشاشات التحميل. */
    val revision: StateFlow<Long> = _revision

    private fun bump() {
        _revision.value = _revision.value + 1
    }

    // ---------- المجلدات المسموحة ----------

    /** فارغة = كل مجلدات الهاتف مسموحة. */
    var allowedBuckets: Set<String>
        get() = prefs.getStringSet(KEY_BUCKETS, emptySet()) ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_BUCKETS, value).apply()
            bump()
        }

    fun isBucketAllowed(bucket: String): Boolean {
        val allowed = allowedBuckets
        return allowed.isEmpty() || allowed.contains(bucket)
    }

    // ---------- المفاتيح ----------

    var hideShorts: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SHORTS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HIDE_SHORTS, value).apply()
            bump()
        }

    var hideSearch: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SEARCH, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HIDE_SEARCH, value).apply()
            bump()
        }

    var hideComments: Boolean
        get() = prefs.getBoolean(KEY_HIDE_COMMENTS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HIDE_COMMENTS, value).apply()
            bump()
        }

    var autoPlayNext: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()
            bump()
        }

    var resumePlayback: Boolean
        get() = prefs.getBoolean(KEY_RESUME, true)
        set(value) {
            prefs.edit().putBoolean(KEY_RESUME, value).apply()
            bump()
        }

    var lockExit: Boolean
        get() = prefs.getBoolean(KEY_LOCK_EXIT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LOCK_EXIT, value).apply()
            bump()
        }

    // ---------- الاشتراكات الوهمية ----------

    var subscribedBuckets: Set<String>
        get() = prefs.getStringSet(KEY_SUBS, emptySet()) ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_SUBS, HashSet(value)).apply()
        }

    fun isSubscribed(bucket: String): Boolean = subscribedBuckets.contains(bucket)

    fun setSubscribed(bucket: String, subscribed: Boolean) {
        val current = HashSet(subscribedBuckets)
        if (subscribed) current.add(bucket) else current.remove(bucket)
        subscribedBuckets = current
    }

    // ---------- رمز ولي الأمر ----------

    val hasPin: Boolean
        get() = !prefs.getString(KEY_PIN, null).isNullOrBlank()

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, hash(pin)).apply()
        bump()
    }

    fun checkPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN, null) ?: return true
        return stored == hash(pin)
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
        bump()
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(("sukaina-tube:" + value).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val KEY_BUCKETS = "allowed_buckets"
        const val KEY_HIDE_SHORTS = "hide_shorts"
        const val KEY_HIDE_SEARCH = "hide_search"
        const val KEY_HIDE_COMMENTS = "hide_comments"
        const val KEY_AUTOPLAY = "auto_play_next"
        const val KEY_RESUME = "resume_playback"
        const val KEY_LOCK_EXIT = "lock_exit"
        const val KEY_PIN = "parent_pin"
        const val KEY_SUBS = "subscribed_buckets"
    }
}

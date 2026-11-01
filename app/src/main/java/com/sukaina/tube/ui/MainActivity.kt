package com.sukaina.tube.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sukaina.tube.R
import com.sukaina.tube.SukainaApp
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.databinding.ActivityMainBinding
import com.sukaina.tube.ui.home.HomeFragment
import com.sukaina.tube.ui.library.LibraryFragment
import com.sukaina.tube.ui.parent.ParentPinActivity
import com.sukaina.tube.ui.parent.SearchActivity
import com.sukaina.tube.ui.shorts.ShortsFragment
import com.sukaina.tube.ui.subs.SubscriptionsFragment
import com.sukaina.tube.util.Ui
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var app: SukainaApp
    private var currentTag: String? = null
    private var avatarTaps = 0
    private var lastAvatarTap = 0L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionUi()
    }

    private val exitRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) finishAffinity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        app = application as SukainaApp
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Ui.padTop(binding.topBarInclude.root)
        Ui.padBottom(binding.bottomNav)

        setupTopBar()
        setupBottomNav()
        setupBackHandling()

        binding.permissionOverlay.permissionButton.setOnClickListener {
            permissionLauncher.launch(requiredPermissions())
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home
            showTab("home") { HomeFragment() }
        } else {
            currentTag = savedInstanceState.getString(KEY_TAG, "home")
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.settings.revision.collect {
                    applySettings()
                    MediaRepo.refresh(this@MainActivity)
                }
            }
        }

        ensurePermission()
    }

    override fun onResume() {
        super.onResume()
        if (hasMediaPermission() && MediaRepo.videos.value.isEmpty()) {
            MediaRepo.refresh(this)
        }
        refreshPermissionUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_TAG, currentTag)
        super.onSaveInstanceState(outState)
    }

    // ---------------- الشريط العلوي ----------------

    private fun setupTopBar() {
        val bar = binding.topBarInclude
        bar.logo.setOnLongClickListener {
            openParentGate()
            true
        }
        bar.btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        bar.btnCast.setOnClickListener { Ui.toastShort(it, R.string.toast_unavailable) }
        bar.btnNotifications.setOnClickListener { Ui.toastShort(it, R.string.toast_unavailable) }
        bar.avatar.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastAvatarTap > 1500) avatarTaps = 0
            lastAvatarTap = now
            avatarTaps++
            if (avatarTaps >= 5) {
                avatarTaps = 0
                openParentGate()
            } else {
                binding.bottomNav.selectedItemId = R.id.nav_library
            }
        }
    }

    private fun openParentGate() {
        startActivity(Intent(this, ParentPinActivity::class.java))
    }

    // ---------------- التبويبات ----------------

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_create -> {
                    Ui.toastShort(binding.root, R.string.toast_create)
                    false
                }
                R.id.nav_home -> {
                    showTab("home") { HomeFragment() }
                    true
                }
                R.id.nav_shorts -> {
                    showTab("shorts") { ShortsFragment() }
                    true
                }
                R.id.nav_subs -> {
                    showTab("subs") { SubscriptionsFragment() }
                    true
                }
                R.id.nav_library -> {
                    showTab("library") { LibraryFragment() }
                    true
                }
                else -> false
            }
        }
    }

    private fun showTab(tag: String, factory: () -> Fragment) {
        if (tag == currentTag) return
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()
        fm.fragments.forEach { tx.hide(it) }
        var target = fm.findFragmentByTag(tag)
        if (target == null) {
            target = factory()
            tx.add(R.id.container, target, tag)
        } else {
            tx.show(target)
        }
        tx.commit()
        currentTag = tag
    }

    private fun applySettings() {
        val settings = app.settings
        binding.topBarInclude.btnSearch.visibility =
            if (settings.hideSearch) View.GONE else View.VISIBLE

        val nav = binding.bottomNav.menu
        nav.findItem(R.id.nav_shorts)?.isVisible = !settings.hideShorts
        if (settings.hideShorts && currentTag == "shorts") {
            binding.bottomNav.selectedItemId = R.id.nav_home
        }
    }

    // ---------------- الخروج / القفل ----------------

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentTag != "home") {
                    binding.bottomNav.selectedItemId = R.id.nav_home
                    return
                }
                if (app.settings.lockExit && app.settings.hasPin) {
                    val intent = Intent(this@MainActivity, ParentPinActivity::class.java)
                    intent.putExtra(ParentPinActivity.EXTRA_TARGET, ParentPinActivity.TARGET_EXIT)
                    exitRequest.launch(intent)
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    // ---------------- الصلاحيات ----------------

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 33) {
            if (Build.VERSION.SDK_INT >= 34) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            } else {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasFullMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasPartialMediaPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasMediaPermission(): Boolean = hasFullMediaPermission() || hasPartialMediaPermission()

    private fun ensurePermission() {
        if (!hasMediaPermission()) {
            permissionLauncher.launch(requiredPermissions())
        } else {
            refreshPermissionUi()
        }
    }

    private fun refreshPermissionUi() {
        val overlay = binding.permissionOverlay.root
        if (hasMediaPermission()) {
            overlay.visibility = View.GONE
            if (MediaRepo.videos.value.isEmpty()) {
                MediaRepo.refresh(this)
            }
        } else {
            overlay.visibility = View.VISIBLE
            MediaRepo.setNoPermission()
        }

        val partialBanner = hasPartialMediaPermission() && !hasFullMediaPermission()
        binding.permissionOverlay.permissionBody.text = if (partialBanner) {
            getString(R.string.permission_partial)
        } else {
            getString(R.string.permission_body)
        }
        binding.permissionOverlay.permissionButton.text = if (partialBanner) {
            getString(R.string.permission_full)
        } else {
            getString(R.string.permission_button)
        }
    }

    companion object {
        private const val KEY_TAG = "tab"
        private const val REQ_EXIT = 4301
    }
}

package com.sukaina.tube.ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.checkbox.MaterialCheckBox
import com.sukaina.tube.R
import com.sukaina.tube.SukainaApp
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.data.SettingsStore
import com.sukaina.tube.databinding.ActivityParentSettingsBinding
import com.sukaina.tube.util.Ui
import kotlinx.coroutines.launch

class ParentSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentSettingsBinding
    private lateinit var settings: SettingsStore
    private var suppress = false
    private var allFolders: List<String> = emptyList()

    private val changePinLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Ui.padTop(binding.topBar)

        settings = (application as SukainaApp).settings

        binding.btnBack.setOnClickListener { finish() }
        setupSwitches()
        setupActions()

        lifecycleScope.launch {
            allFolders = MediaRepo.allFolders(this@ParentSettingsActivity)
            rebuildFolders()
        }
    }

    private fun setupSwitches() {
        binding.rowShorts.rowTitle.setText(R.string.parent_hide_shorts)
        binding.rowShorts.rowSubtitle.text = ""
        binding.rowShorts.switchView.isChecked = settings.hideShorts
        binding.rowShorts.switchView.setOnCheckedChangeListener { _, checked ->
            settings.hideShorts = checked
        }

        binding.rowSearch.rowTitle.setText(R.string.parent_hide_search)
        binding.rowSearch.rowSubtitle.text = ""
        binding.rowSearch.switchView.isChecked = settings.hideSearch
        binding.rowSearch.switchView.setOnCheckedChangeListener { _, checked ->
            settings.hideSearch = checked
        }

        binding.rowComments.rowTitle.setText(R.string.parent_hide_comments)
        binding.rowComments.rowSubtitle.setText(R.string.parent_hide_comments_hint)
        binding.rowComments.switchView.isChecked = settings.hideComments
        binding.rowComments.switchView.setOnCheckedChangeListener { _, checked ->
            settings.hideComments = checked
        }

        binding.rowAutoplay.rowTitle.setText(R.string.parent_autoplay)
        binding.rowAutoplay.rowSubtitle.text = ""
        binding.rowAutoplay.switchView.isChecked = settings.autoPlayNext
        binding.rowAutoplay.switchView.setOnCheckedChangeListener { _, checked ->
            settings.autoPlayNext = checked
        }

        binding.rowResume.rowTitle.setText(R.string.parent_resume)
        binding.rowResume.rowSubtitle.text = ""
        binding.rowResume.switchView.isChecked = settings.resumePlayback
        binding.rowResume.switchView.setOnCheckedChangeListener { _, checked ->
            settings.resumePlayback = checked
        }

        binding.rowLockExit.rowTitle.setText(R.string.parent_lock_exit)
        binding.rowLockExit.rowSubtitle.setText(R.string.parent_lock_exit_hint)
        binding.rowLockExit.switchView.isChecked = settings.lockExit
        binding.rowLockExit.switchView.setOnCheckedChangeListener { _, checked ->
            settings.lockExit = checked
        }
    }

    private fun setupActions() {
        binding.checkAll.setOnCheckedChangeListener { _, checked ->
            if (suppress) return@setOnCheckedChangeListener
            settings.allowedBuckets =
                if (checked) emptySet() else allFolders.toSet()
            rebuildFolders()
        }

        binding.btnChangePin.setOnClickListener {
            changePinLauncher.launch(
                Intent(this, ParentPinActivity::class.java)
                    .putExtra(ParentPinActivity.EXTRA_CHANGE, true)
            )
        }

        binding.btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.parent_clear_history)
                .setMessage(R.string.parent_folders_hint)
                .setPositiveButton(R.string.parent_clear_history) { _, _ ->
                    val app = application as SukainaApp
                    app.io.launch { app.database.dao().clearHistory() }
                    Toast.makeText(this, R.string.parent_cleared, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.action_back, null)
                .show()
        }
    }

    private fun rebuildFolders() {
        suppress = true
        binding.folderList.removeAllViews()

        val allowed = settings.allowedBuckets
        val showAll = allowed.isEmpty()
        binding.checkAll.isChecked = showAll
        binding.noFolders.visibility = if (allFolders.isEmpty()) View.VISIBLE else View.GONE

        val density = resources.displayMetrics.density
        allFolders.forEach { bucket ->
            val checkBox = MaterialCheckBox(this)
            checkBox.text = "قناة $bucket"
            checkBox.textSize = 14f
            checkBox.isChecked = showAll || allowed.contains(bucket)
            checkBox.isEnabled = !showAll
            checkBox.setPadding(
                (16 * density).toInt(), 0, (16 * density).toInt(), 0
            )
            checkBox.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            checkBox.setOnCheckedChangeListener { _, checked ->
                if (suppress) return@setOnCheckedChangeListener
                val current = HashSet(settings.allowedBuckets)
                if (current.isEmpty()) current.addAll(allFolders)
                if (checked) current.add(bucket) else current.remove(bucket)
                settings.allowedBuckets = current
            }
            binding.folderList.addView(checkBox)
        }
        suppress = false
    }
}

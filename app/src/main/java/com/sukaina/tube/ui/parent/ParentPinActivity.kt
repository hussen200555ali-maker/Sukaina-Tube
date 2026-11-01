package com.sukaina.tube.ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sukaina.tube.R
import com.sukaina.tube.SukainaApp
import com.sukaina.tube.databinding.ActivityParentPinBinding
import com.sukaina.tube.util.Ui

class ParentPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentPinBinding
    private lateinit var settings: com.sukaina.tube.data.SettingsStore

    private var changeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentPinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Ui.padTop(binding.root)

        settings = (application as SukainaApp).settings
        changeMode = intent.getBooleanExtra(EXTRA_CHANGE, false)

        val creating = changeMode || !settings.hasPin
        binding.title.setText(if (creating) R.string.parent_pin_new else R.string.parent_pin_title)
        binding.subtitle.setText(if (creating) R.string.parent_pin_new else R.string.parent_pin_enter)
        binding.pinConfirm.visibility = if (creating) View.VISIBLE else View.GONE
        binding.btnConfirm.setText(if (creating) R.string.watch_save else R.string.parent_pin_enter)

        binding.btnConfirm.setOnClickListener { confirm() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun confirm() {
        val pin = binding.pinInput.text?.toString().orEmpty()
        if (pin.length != 4) {
            showError(R.string.parent_pin_new)
            return
        }

        val creating = changeMode || !settings.hasPin
        if (creating) {
            val confirm = binding.pinConfirm.text?.toString().orEmpty()
            if (pin != confirm) {
                showError(R.string.parent_pin_mismatch)
                return
            }
            settings.setPin(pin)
            succeed()
        } else {
            if (settings.checkPin(pin)) {
                succeed()
            } else {
                showError(R.string.parent_pin_wrong)
                binding.pinInput.setText("")
            }
        }
    }

    private fun showError(res: Int) {
        binding.error.setText(res)
        binding.error.visibility = View.VISIBLE
    }

    private fun succeed() {
        binding.error.visibility = View.GONE
        if (changeMode) {
            setResult(RESULT_OK)
            finish()
            return
        }
        val target = intent.getStringExtra(EXTRA_TARGET)
        if (target == TARGET_EXIT) {
            setResult(RESULT_OK)
            finish()
        } else {
            startActivity(Intent(this, ParentSettingsActivity::class.java))
            finish()
        }
    }

    companion object {
        const val EXTRA_TARGET = "target"
        const val EXTRA_CHANGE = "change"
        const val TARGET_SETTINGS = "settings"
        const val TARGET_EXIT = "exit"
    }
}

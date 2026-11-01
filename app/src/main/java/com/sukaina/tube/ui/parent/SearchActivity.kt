package com.sukaina.tube.ui.parent

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sukaina.tube.R
import com.sukaina.tube.data.MediaRepo
import com.sukaina.tube.data.VideoItem
import com.sukaina.tube.databinding.ActivitySearchBinding
import com.sukaina.tube.ui.home.VideoCompactAdapter
import com.sukaina.tube.ui.watch.WatchActivity
import com.sukaina.tube.util.Ui

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: VideoCompactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Ui.padTop(binding.topBar)

        adapter = VideoCompactAdapter { item ->
            startActivity(WatchActivity.intent(this, item.id))
        }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSearch.setOnClickListener { runQuery() }

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runQuery()
                true
            } else {
                false
            }
        }
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if ((s?.length ?: 0) >= 2) runQuery() else show(emptyList())
            }
        })

        binding.searchInput.requestFocus()
        binding.searchInput.post {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun runQuery() {
        val query = binding.searchInput.text?.toString()?.trim().orEmpty()
        if (query.isEmpty()) {
            show(emptyList())
            return
        }
        val results: List<VideoItem> = MediaRepo.videos.value.filter {
            it.title.contains(query, ignoreCase = true)
        }
        show(results)
    }

    private fun show(items: List<VideoItem>) {
        adapter.submitList(items)
        binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}

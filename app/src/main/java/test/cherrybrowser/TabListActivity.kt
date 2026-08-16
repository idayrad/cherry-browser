package test.cherrybrowser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Shows the list of tabs using RecyclerView.
 * Result contract (setResult):
 * - selecting a tab: RESULT_OK + extra "selected_tab_id" -> MainActivity should open it
 * - pressing New: RESULT_OK + extra "action"="new"
 *
 * Note: TabManager persists tab list so both activities use same underlying JSON store.
 */
class TabListActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: TabListAdapter
    private lateinit var tabManager: TabManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabManager = TabManager(this)
        tabManager.restore() // ensure current list loaded
        setContentView(R.layout.activity_tab_list)

        rv = findViewById(R.id.recycler_tabs)
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        adapter = TabListAdapter(tabManager.list().toMutableList(),
            onSelect = { tab ->
                val i = Intent().putExtra("selected_tab_id", tab.id)
                setResult(Activity.RESULT_OK, i)
                finish()
            },
            onClose = { tab ->
                // remove from manager and update adapter
                tabManager.remove(tab.id)
                adapter.remove(tab)
            }
        )
        rv.adapter = adapter

        findViewById<Button>(R.id.btn_new_tab).setOnClickListener {
            val i = Intent().putExtra("action", "new")
            setResult(Activity.RESULT_OK, i)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // reload in case of external changes
        adapter.update(tabManager.list())
    }
}

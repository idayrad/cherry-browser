package test.cherrybrowser

import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Simple TabManager: keeps an in-memory list and persists minimal metadata to app files.
 * To keep the example dependency-free we write JSON manually.
 */
class TabManager(private val ctx: Context) {
    private val tabs = mutableListOf<BrowserTab>()
    private val file = File(ctx.filesDir, "tabs.json")

    fun list(): List<BrowserTab> = tabs.toList()

    fun add(tab: BrowserTab) {
        tabs.add(tab)
        persistAsync()
    }

    fun remove(tabId: String) {
        tabs.removeAll { it.id == tabId }
        persistAsync()
    }

    fun find(tabId: String): BrowserTab? = tabs.find { it.id == tabId }

    fun clear() {
        tabs.clear()
        persistAsync()
    }

    fun persistAsync() {
        // lightweight persistence: only store id, url, title, snapshotPath; not the webView state bundles.
        val snapshot = JSONArray()
        tabs.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("url", it.url)
            o.put("title", it.title)
            o.put("snapshotPath", it.snapshotPath)
            snapshot.put(o)
        }
        GlobalScope.launch(Dispatchers.IO) {
            file.writeText(snapshot.toString())
        }
    }

    fun restore() {
        if (!file.exists()) return
        try {
            val s = file.readText()
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val tab = BrowserTab(
                    id = o.getString("id"),
                    url = o.optString("url", "about:blank"),
                    title = o.optString("title", null),
                    snapshotPath = o.optString("snapshotPath", null)
                )
                tabs.add(tab)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Helper to migrate a WebView state bundle into a tab record.
     */
    fun saveWebViewStateToTab(tabId: String, state: Bundle) {
        find(tabId)?.webViewState = state
        persistAsync()
    }
}

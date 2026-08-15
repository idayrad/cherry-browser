package test.cherrybrowser

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.view.KeyEvent
import android.view.View
import android.webkit.ValueCallback
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * MainActivity: hosts the active WebView inside a FrameLayout and bottom bar with controls.
 * Minimal tab list modal and settings entry.
 */
class MainActivity : AppCompatActivity() {
    companion object {
        const val FILE_CHOOSER_REQUEST = 101
    }

    private lateinit var container: ConstraintLayout
    private lateinit var urlInput: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnTabs: ImageButton
    private lateinit var btnMenu: ImageButton

    private val tabManager by lazy { TabManager(this) }
    private var activeWebView: BrowserWebView? = null
    private var activeTab: BrowserTab? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabManager.restore()
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.container)
        urlInput = findViewById(R.id.url_input)
        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnTabs = findViewById(R.id.btn_tabs)
        btnMenu = findViewById(R.id.btn_menu)

        btnBack.setOnClickListener { activeWebView?.goBack() }
        btnForward.setOnClickListener { activeWebView?.goForward() }
        btnTabs.setOnClickListener { showTabList() }
        btnMenu.setOnClickListener { showMenu() }

        urlInput.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val url = urlInput.text.toString().let { normalizeUrl(it) }
                activeWebView?.loadUrl(url)
                true
            } else false
        }

        // If there are restored tabs, open first or open a new one:
        val first = tabManager.list().firstOrNull()
        if (first != null) {
            openTab(first)
        } else {
            newTab("about:blank")
        }
    }

    private fun normalizeUrl(input: String): String {
        var u = input.trim()
        if (!u.contains("://")) {
            u = "https://$u"
        }
        return u
    }

    private fun newTab(url: String) {
        val tab = BrowserTab(url = url)
        tabManager.add(tab)
        openTab(tab)
    }

    private fun openTab(tab: BrowserTab) {
        // Remove previous WebView if any
        activeWebView?.let { vw ->
            // Save its state to tab.webViewState
            val b = Bundle()
            vw.saveState(b)
            activeTab?.webViewState = b
            // Optionally remove view to hold multiple
            container.removeView(vw)
        }

        // Create or reuse a WebView
        val web = BrowserWebView(this)
        activeWebView = web
        activeTab = tab
        container.addView(web, 0)

        // Restore state if available
        tab.webViewState?.let {
            web.restoreState(it)
        } ?: web.loadUrl(tab.url)

        // Hook callbacks
        web.createNewWindowCallback = { targetUrl ->
            // Create a new tab and navigate
            lifecycleScope.launch(Dispatchers.Main) {
                val new = BrowserTab(url = targetUrl ?: "about:blank")
                tabManager.add(new)
                openTab(new)
            }
        }

        web.fileChooserCallback = { valueCallback ->
            // store callback and start picker
            pendingFileChooser = valueCallback
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
            startActivityForResult(Intent.createChooser(intent, "Select file"), FILE_CHOOSER_REQUEST)
        }

        web.downloadHandler = { url, ua, cd, mime ->
            // Start DownloadService
            val intent = Intent(this, DownloadService::class.java).apply {
                putExtra(DownloadService.EXTRA_URL, url)
                putExtra(DownloadService.EXTRA_FILENAME, url.substringAfterLast('/', "download"))
            }
            startService(intent)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        }

        web.webChromeClient = web.webChromeClient // keep configured
        web.webViewClient = web.webViewClient
    }

    private var pendingFileChooser: ValueCallback<Array<Uri>>? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val result = if (resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data
                arrayOf(uri!!)
            } else null
            pendingFileChooser?.onReceiveValue(result)
            pendingFileChooser = null
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showTabList() {
        val tabs = tabManager.list()
        val titles = tabs.map { it.title ?: it.url }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tabs")
            .setItems(titles) { _, which ->
                openTab(tabs[which])
            }
            .setNeutralButton("New") { _, _ -> newTab("about:blank") }
            .show()
    }

    private fun showMenu() {
        val items = arrayOf("New Tab", "Settings", "Console")
        AlertDialog.Builder(this)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> newTab("about:blank")
                    1 -> startActivity(Intent(this, SettingsActivity::class.java))
                    2 -> startActivity(Intent(this, ConsoleActivity::class.java))
                }
            }
            .show()
    }

    override fun onBackPressed() {
        activeWebView?.let {
            if (it.canGoBack()) {
                it.goBack()
                return
            }
        }
        super.onBackPressed()
    }
}

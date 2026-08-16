package test.cherrybrowser

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple console viewer: shows logcat filtered messages when opened.
 * For a full implementation you'd stream logs from a dedicated logger that receives
 * onConsoleMessage events. Here we show a static hint.
 */
class ConsoleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "JS Console viewer: console logging forwarded to Logcat (filter tag: CherryBridge / BrowserWebView).\n\nFuture: connect via bound service or use file-based log to show messages here."
        setContentView(tv)
    }
}

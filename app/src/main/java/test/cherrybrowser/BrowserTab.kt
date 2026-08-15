package test.cherrybrowser

import android.graphics.Bitmap
import android.os.Bundle
import java.io.File
import java.util.UUID

/**
 * Data model for a browser tab.
 * webViewState: store WebView.saveState(Bundle) for later restore.
 * snapshotPath: optional saved snapshot bitmap when tab is put to disk in single-tab-mode.
 */
data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "about:blank",
    var title: String? = null,
    var favicon: Bitmap? = null,
    var isIncognito: Boolean = false,
    var webViewState: Bundle? = null,
    var snapshotPath: String? = null
)

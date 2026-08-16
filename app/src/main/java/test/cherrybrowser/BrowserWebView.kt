package test.cherrybrowser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Message
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Central WebView wrapper with sane defaults for a small browser:
 * - JS enabled
 * - DOM storage, IndexedDB, file access
 * - Configure user agent override support
 * - Console forwarding
 * - File chooser handling delegated via callback
 * - Custom onCreateWindow to support popups -> new tabs (via createNewWindowCallback)
 * - Blob download interception via JS bridge (see injectBlobDownloader)
 */
@SuppressLint("SetJavaScriptEnabled")
class BrowserWebView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : WebView(ctx, attrs) {

    var fileChooserCallback: ((valueCallback: ValueCallback<Array<Uri>>?) -> Unit)? = null
    var createNewWindowCallback: ((targetUrl: String?) -> Unit)? = null
    var downloadHandler: ((url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) -> Unit)? = null

    private val TAG = "BrowserWebView"

    init {
        setup()
    }

    @SuppressLint("NewApi")
    private fun setup() {
        val ws = settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.setSupportMultipleWindows(true)
        ws.javaScriptCanOpenWindowsAutomatically = true
        ws.allowFileAccess = true
        ws.allowContentAccess = true
        ws.databaseEnabled = true
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        // Removed method
		// ws.setAppCacheEnabled(true)

        // improve compatibility
        ws.userAgentString = ws.userAgentString + " CherryBrowser/0.1"
        // Accept mixed content for localhost and many test sites
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        // WebViewClient
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                // handle intent: and market: links
                if (url.startsWith("intent:") || url.startsWith("market:")) {
                    try {
                        val i = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        context.startActivity(i)
                        return true
                    } catch (e: Exception) {
                        // fallback
                    }
                }
                return false // let WebView load it
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // could show a friendly offline page
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                // Intercept downloads (basic heuristic)
                val url = request?.url?.toString() ?: return null
                if (request.requestHeaders["Range"] != null || url.contains("/download") || url.endsWith(".apk") || url.endsWith(".zip")) {
                    downloadHandler?.invoke(url, settings.userAgentString, null, null)
                    // return a small empty response to stop default handling
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        // WebChromeClient
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(TAG, "JS: ${it.message()} -- ${it.sourceId()}:${it.lineNumber()}")
                }
                return true
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.invoke(filePathCallback)
                return true
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                // Create an offscreen WebView or signal MainActivity to create a new tab
                // resultMsg contains a WebView.WebViewTransport, see docs.
                createNewWindowCallback?.invoke(null)
                return true
            }
        }

        // JS bridge for blob download and storage sync
        addJavascriptInterface(AndroidBridge(), "CherryBridge")

        // Inject blob-downloader helper when page loads
        setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                injectBlobDownloader()
                injectLocalStorageSync()
                super.onPageFinished(view, url)
            }
        })
    }

    // JavaScript bridge
    inner class AndroidBridge {
        @JavascriptInterface
        fun postConsole(msg: String) {
            Log.d("CherryBridge", "JS_CONSOLE: $msg")
        }

        // For blob downloads: pass base64 data so native can write
        @JavascriptInterface
        fun downloadBase64(filename: String, base64: String) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                // write to app external files dir
                val out = File(context.getExternalFilesDir(null), filename)
                FileOutputStream(out).use { it.write(bytes) }
                // notify DownloadService or send broadcast (left as an exercise)
                Log.d("CherryBridge", "Saved blob to ${out.absolutePath}")
            } catch (e: Exception) {
                Log.e("CherryBridge", "Failed to save base64: ${e.message}")
            }
        }

        @JavascriptInterface
        fun localStorageSnapshot(json: String) {
            try {
                val f = File(context.filesDir, "localstorage-backup.json")
                f.writeText(json)
            } catch (e: Exception) {
                Log.e("CherryBridge", "LS backup failed")
            }
        }
    }

    private fun injectBlobDownloader() {
        // This helper intercepts clicks on anchors with blob: hrefs and routes them into
        // fetch + base64 -> CherryBridge.downloadBase64
        val js = """
            (function(){
                if (window.__cherry_blob_hooked) return;
                window.__cherry_blob_hooked = true;
                document.addEventListener('click', function(e){
                    var a = e.target.closest && e.target.closest('a');
                    if (!a) return;
                    var href = a.getAttribute('href') || '';
                    if (href.startsWith('blob:')) {
                        e.preventDefault();
                        fetch(href).then(function(r){ return r.blob(); }).then(function(b){
                            var reader = new FileReader();
                            reader.onload = function(){ 
                                var data = reader.result.split(',')[1];
                                var name = a.getAttribute('download') || ('download-' + Date.now());
                                window.CherryBridge.downloadBase64(name, data);
                            };
                            reader.readAsDataURL(b);
                        });
                    }
                }, true);
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    private fun injectLocalStorageSync() {
        val js = """
            (function(){
                try {
                    var json = JSON.stringify(localStorage);
                    window.CherryBridge.localStorageSnapshot(json);
                } catch(e){}
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    /**
     * Helper to start a native download via custom engine.
     */
    fun startDownload(url: String, filenameHint: String?) {
        val ua = settings.userAgentString
        downloadHandler?.invoke(url, ua, null, null)
    }
}

package com.app.adblokerpro

import android.os.Bundle
import android.webkit.*
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize AdBlocker
        AdBlocker.init(this)

        // Configure WebView
        webView = findViewById(R.id.webView)
        webView.webViewClient = MyBrowser()
        webView.webChromeClient = MyChromeClient()

        configureWebView()

        // Load the initial URL
        webView.loadUrl("PASTE_YOUR_WEB_LINK")

        // Handle back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }

    /**
     * Configures WebView settings to enhance performance, security, and user experience.
     */
    private fun configureWebView() {
        val webSettings: WebSettings = webView.settings

        // Enable JavaScript
        webSettings.javaScriptEnabled = true

        // Improve performance
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        // Security settings
        webSettings.allowFileAccess = false
        webSettings.allowContentAccess = false
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        // Media support
        webSettings.mediaPlaybackRequiresUserGesture = false

        // Smooth browsing
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
    }

    /**
     * Handles back navigation within the WebView or shows an exit confirmation dialog.
     */
    private fun handleBackNavigation() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            showExitDialog()
        }
    }

    /**
     * Displays an exit confirmation dialog.
     */
    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Custom WebViewClient to handle URL loading and ad-blocking.
     */
    private inner class MyBrowser : WebViewClient() {
        private val loadedUrls = mutableMapOf<String, Boolean>()

        // Intercept requests to block ads
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()
            val isAd = loadedUrls.getOrPut(url) { AdBlocker.isAd(url) }
            return if (isAd) AdBlocker.createEmptyResource() else super.shouldInterceptRequest(view, request)
        }

        // Handle URL loading
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (URLUtil.isValidUrl(url)) {
                view.loadUrl(url)
                return true
            }
            return false
        }

        // Handle network errors
        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            view.loadUrl("file:///android_asset/error.html") // Load custom error page
        }
    }

    /**
     * Custom WebChromeClient to handle JavaScript dialogs.
     */
    private inner class MyChromeClient : WebChromeClient() {
        // Handle JavaScript alerts
        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Alert")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                .setCancelable(false)
                .show()
            return true
        }

        // Handle JavaScript confirmations
        override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Confirm")
                .setMessage(message)
                .setPositiveButton("Yes") { _, _ -> result.confirm() }
                .setNegativeButton("No") { _, _ -> result.cancel() }
                .setCancelable(false)
                .show()
            return true
        }

        // Handle JavaScript prompts
        override fun onJsPrompt(
            view: WebView,
            url: String,
            message: String,
            defaultValue: String,
            result: JsPromptResult
        ): Boolean {
            val input = EditText(this@MainActivity)
            input.setText(defaultValue)
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Prompt")
                .setMessage(message)
                .setView(input)
                .setPositiveButton("OK") { _, _ -> result.confirm(input.text.toString()) }
                .setNegativeButton("Cancel") { _, _ -> result.cancel() }
                .show()
            return true
        }

        // Provide progress updates
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            // Add your progress update logic here if needed
        }
    }
}

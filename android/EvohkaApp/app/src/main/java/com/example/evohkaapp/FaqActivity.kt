package com.example.evohkaapp

import android.app.Activity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class FaqActivity : EvohkaActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        webView = findViewById(R.id.webFaq)
        webView.settings.apply {
            javaScriptEnabled = true
            blockNetworkLoads = true
            allowContentAccess = false
            allowFileAccess = true
            domStorageEnabled = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return request?.url?.toString() != "file:///android_asset/faq/index.html"
            }
        }
        webView.loadUrl("file:///android_asset/faq/index.html")
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}

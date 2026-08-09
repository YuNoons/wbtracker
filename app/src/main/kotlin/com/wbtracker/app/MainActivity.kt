package com.wbtracker.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.wbtracker.app.bridge.WbBridge
import com.wbtracker.app.data.repository.UserPreferencesRepository
import com.wbtracker.app.domain.repository.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var wbBridge: WbBridge
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val syncIntervalStr = userPreferencesRepository.syncInterval.value
        val hours = syncIntervalStr.filter { it.isDigit() }.toLongOrNull() ?: 6L
        syncScheduler.schedulePeriodicUpdate(hours)
        checkBatteryOptimization()

        webView = WebView(this).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            webViewClient = WebViewClient()
            addJavascriptInterface(wbBridge, "WbBridge")
            loadUrl("file:///android_asset/index.html")
        }

        wbBridge.attachWebView(webView)
        setContentView(webView)

        handleSharedIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        val sharedUrl = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        if (!sharedUrl.isNullOrEmpty()) {
            webView.post {
                val escaped = sharedUrl.replace("'", "\\'")
                webView.evaluateJavascript("if (typeof openAddModal === 'function') { openAddModal(); document.getElementById('wb-url-input').value = '$escaped'; }", null)
            }
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized) {
            webView.evaluateJavascript("window.handleAndroidBack()") { result ->
                if (result == "false" || result == null || result == "null") {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }
}

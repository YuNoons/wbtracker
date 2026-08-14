package com.wbtracker.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.activity.result.contract.ActivityResultContracts
import com.wbtracker.app.bridge.WbBridge
import com.wbtracker.app.data.repository.UserPreferencesRepository
import com.wbtracker.app.domain.repository.ProductRepository
import com.wbtracker.app.domain.repository.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var wbBridge: WbBridge
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var repository: ProductRepository

    private lateinit var webView: WebView
    private var pendingShortcutAction: String? = null
    private var isWebViewLoaded = false

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        userPreferencesRepository.setNotificationsEnabled(isGranted)
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val content = contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: ""

                    val result = repository.importBackupJson(content)
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            val count = result.getOrDefault(0)
                            val msg = "Успешно импортировано $count товаров"
                            val escapedMsg = JSONObject.quote(msg)
                            webView.evaluateJavascript("if (typeof window.showTopToast === 'function') { window.showTopToast('success', $escapedMsg); } else { alert($escapedMsg); } if (typeof window.refreshData === 'function') { window.refreshData(); }", null)
                        } else {
                            val err = result.exceptionOrNull()?.message ?: "Ошибка импорта"
                            val escapedErr = JSONObject.quote(err)
                            webView.evaluateJavascript("if (typeof window.showTopToast === 'function') { window.showTopToast('error', $escapedErr); } else { alert($escapedErr); }", null)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val err = "Ошибка чтения файла: ${e.localizedMessage}"
                        val escapedErr = JSONObject.quote(err)
                        webView.evaluateJavascript("if (typeof window.showTopToast === 'function') { window.showTopToast('error', $escapedErr); } else { alert($escapedErr); }", null)
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val syncIntervalStr = userPreferencesRepository.syncInterval.value
        val hours = syncIntervalStr.filter { it.isDigit() }.toLongOrNull() ?: 6L
        syncScheduler.schedulePeriodicUpdate(hours)

        wbBridge.onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        wbBridge.onImportBackupRequested = {
            try {
                importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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
                userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isWebViewLoaded = true
                    pendingShortcutAction?.let { action ->
                        wbBridge.openShortcutAction(action)
                        pendingShortcutAction = null
                    }
                }
            }
            addJavascriptInterface(wbBridge, "WbBridge")
            loadUrl("file:///android_asset/index.html")
        }

        wbBridge.attachWebView(webView)
        setContentView(webView)

        handleSharedIntent(intent)
        handleShortcutIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
        handleShortcutIntent(intent)
    }

    fun openProductUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    private fun handleShortcutIntent(intent: Intent?) {
        val shortcutAction = intent?.getStringExtra("shortcut_action")
        if (!shortcutAction.isNullOrEmpty()) {
            if (isWebViewLoaded) {
                webView.post {
                    wbBridge.openShortcutAction(shortcutAction)
                }
            } else {
                pendingShortcutAction = shortcutAction
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

    override fun onDestroy() {
        super.onDestroy()
        wbBridge.onRequestNotificationPermission = null
        wbBridge.onImportBackupRequested = null
    }
}

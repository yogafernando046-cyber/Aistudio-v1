package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (fileUploadCallback != null) {
            val results: Array<Uri>? = if (result.resultCode == RESULT_OK && result.data != null) {
                val data = result.data
                if (data?.clipData != null) {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                } else if (data?.data != null) {
                    arrayOf(data.data!!)
                } else null
            } else null
            fileUploadCallback?.onReceiveValue(results)
            fileUploadCallback = null
        }
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        var cameraGranted = false
        var btGranted = true
        for ((permission, isGranted) in results) {
            if (permission == Manifest.permission.CAMERA && isGranted) {
                cameraGranted = true
            }
            if ((permission == Manifest.permission.BLUETOOTH_CONNECT || permission == Manifest.permission.BLUETOOTH_SCAN) && !isGranted) {
                btGranted = false
            }
            Log.d("Permissions", "$permission granted = $isGranted")
        }
        val notifyScript = "if (typeof onAndroidPermissionsResult === 'function') { onAndroidPermissionsResult($cameraGranted, $btGranted); }"
        webView.evaluateJavascript(notifyScript, null)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize WebView
        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        setupWebViewSettings()
        setupClients()

        // Add JavaScript interfaces
        webView.addJavascriptInterface(AndroidNativeBridge(), "AndroidNativeBridge")
        webView.addJavascriptInterface(AndroidNativeBridge(), "AndroidBridge")
        webView.addJavascriptInterface(AndroidNativeBridge(), "AndroidNative")
        webView.addJavascriptInterface(AndroidPrintBridge(), "AndroidPrint")

        // Set content view directly to WebView
        setContentView(webView)

        // Request runtime permissions if needed
        requestRequiredPermissions()

        // Load local POS Web App
        webView.loadUrl("file:///android_asset/www/index.html")

        // Back press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("typeof handleAndroidBackPress === 'function' ? handleAndroidBackPress() : false") { result ->
                    if (result != "true") {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            isEnabled = true
                        }
                    }
                }
            }
        })
    }

    private fun requestRequiredPermissions() {
        val permissionsList = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsList.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsList.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissionsList.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    @Suppress("DEPRECATION")
    private fun setupWebViewSettings() {
        webView.setBackgroundColor(android.graphics.Color.parseColor("#0B0F19"))
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            displayZoomControls = false
            builtInZoomControls = false
            setSupportZoom(false)
        }
    }

    private fun setupClients() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    Log.e("MainActivity", "WebView MainFrame Error: ${error?.description}")
                } else {
                    Log.w("MainActivity", "WebView Subresource Warning: ${request?.url} -> ${error?.description}")
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    // Grant requested web permissions (e.g. Camera for Barcode Scanner)
                    request.grant(request.resources)
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                try {
                    filePickerLauncher.launch(intent)
                } catch (e: Exception) {
                    fileUploadCallback = null
                    return false
                }
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebConsole", "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()}")
                return true
            }
        }
    }

    inner class AndroidPrintBridge {
        @JavascriptInterface
        fun printReceipt(documentName: String?) {
            runOnUiThread {
                try {
                    val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val attributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                            .setResolution(PrintAttributes.Resolution("kasir_idris", "Kasir Idris", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        val title = documentName?.ifBlank { "Struk-Kasir-Idris" } ?: "Struk-Kasir-Idris"
                        val adapter = webView.createPrintDocumentAdapter(title)
                        printManager.print(title, adapter, attributes)
                    }
                } catch (e: Exception) {
                    Log.e("AndroidPrintBridge", "Failed to invoke Android print", e)
                }
            }
        }
    }

    inner class AndroidNativeBridge {
        @JavascriptInterface
        fun getGeminiApiKey(): String {
            return BuildConfig.GEMINI_API_KEY
        }

        @JavascriptInterface
        fun requestAllPermissions() {
            runOnUiThread {
                requestRequiredPermissions()
            }
        }

        @JavascriptInterface
        fun checkPermissionsStatus(): String {
            val hasCamera = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            val hasBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else true

            val obj = JSONObject().apply {
                put("camera", hasCamera)
                put("bluetooth", hasBluetooth)
            }
            return obj.toString()
        }

        @JavascriptInterface
        fun openAppSettings() {
            runOnUiThread {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to open settings", e)
                }
            }
        }

        @JavascriptInterface
        fun printReceipt(documentName: String?) {
            runOnUiThread {
                try {
                    val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val attributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                            .setResolution(PrintAttributes.Resolution("kasir_idris", "Kasir Idris", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        val title = documentName?.ifBlank { "Struk-Kasir-Idris" } ?: "Struk-Kasir-Idris"
                        val adapter = webView.createPrintDocumentAdapter(title)
                        printManager.print(title, adapter, attributes)
                    }
                } catch (e: Exception) {
                    Log.e("AndroidNativeBridge", "Print error", e)
                }
            }
        }

        @JavascriptInterface
        fun vibrate(durationMs: Long) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(durationMs.coerceIn(10L, 500L), VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs.coerceIn(10L, 500L), VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(durationMs.coerceIn(10L, 500L))
                    }
                }
            } catch (e: Exception) {
                Log.w("AndroidNativeBridge", "Vibrate error", e)
            }
        }

        @JavascriptInterface
        fun isBiometricSupported(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            } else {
                false
            }
        }

        @JavascriptInterface
        fun promptBiometric(title: String?, subtitle: String?) {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        val prompt = android.hardware.biometrics.BiometricPrompt.Builder(this@MainActivity)
                            .setTitle(title?.ifBlank { "Login Cepat Kasir Idris" } ?: "Login Cepat Kasir Idris")
                            .setSubtitle(subtitle?.ifBlank { "Verifikasi Sidik Jari / Wajah" } ?: "Verifikasi Sidik Jari / Wajah")
                            .setDescription("Gunakan biometrik perangkat untuk masuk ke Kasir Idris")
                            .setNegativeButton("Batal", mainExecutor) { _, _ ->
                                webView.evaluateJavascript("if (typeof window._onBiometricAuthResult === 'function') { window._onBiometricAuthResult(false, 'Verifikasi dibatalkan'); }", null)
                            }
                            .build()

                        val cancelSignal = android.os.CancellationSignal()
                        prompt.authenticate(
                            cancelSignal,
                            mainExecutor,
                            object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                                    super.onAuthenticationSucceeded(result)
                                    webView.evaluateJavascript("if (typeof window._onBiometricAuthResult === 'function') { window._onBiometricAuthResult(true, 'Sukses'); }", null)
                                }

                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                                    super.onAuthenticationError(errorCode, errString)
                                    val safeMsg = (errString?.toString() ?: "Autentikasi gagal").replace("'", "\\'")
                                    webView.evaluateJavascript("if (typeof window._onBiometricAuthResult === 'function') { window._onBiometricAuthResult(false, '$safeMsg'); }", null)
                                }

                                override fun onAuthenticationFailed() {
                                    super.onAuthenticationFailed()
                                    webView.evaluateJavascript("if (typeof window._onBiometricAuthResult === 'function') { window._onBiometricAuthResult(false, 'Biometrik tidak cocok'); }", null)
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("AndroidNativeBridge", "Biometric error", e)
                        webView.evaluateJavascript("if (typeof window._onBiometricAuthResult === 'function') { window._onBiometricAuthResult(true, 'Sukses'); }", null)
                    }
                } else {
                    webView.evaluateJavascript("if (typeof window._onBiometricAuthResult === 'function') { window._onBiometricAuthResult(true, 'Sukses'); }", null)
                }
            }
        }

        @JavascriptInterface
        fun askGemini(prompt: String, contextData: String, callbackFn: String) {
            lifecycleScope.launch(Dispatchers.IO) {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val resultText = if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    "Kunci Gemini API belum diatur pada panel Secrets AI Studio. Silakan masukkan GEMINI_API_KEY di panel Secrets agar Asisten AI dapat menjawab secara otomatis."
                } else {
                    try {
                        val systemPrompt = "Anda adalah Asisten AI Pintar Toko Kasir Idris (oleh Yoga Fernando). Tugas Anda adalah membantu kasir/pemilik toko menganalisis penjualan, produk terlaris, stok menipis, perhitungan laba bersih, serta memberikan rekomendasi bisnis ritel yang praktis, ramah, dan solutif dalam Bahasa Indonesia. Berikut konteks data toko saat ini: $contextData"
                        
                        val jsonBody = JSONObject().apply {
                            put("systemInstruction", JSONObject().apply {
                                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                            })
                            put("contents", JSONArray().put(
                                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                            ))
                            put("generationConfig", JSONObject().apply {
                                put("temperature", 0.7)
                                put("maxOutputTokens", 1200)
                            })
                        }

                        val request = Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                            .build()

                        val response = okHttpClient.newCall(request).execute()
                        val responseBody = response.body?.string() ?: ""
                        if (response.isSuccessful && responseBody.isNotBlank()) {
                            val jsonResp = JSONObject(responseBody)
                            val candidates = jsonResp.optJSONArray("candidates")
                            val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            parts?.optJSONObject(0)?.optString("text", "Tidak ada respon dari AI.") ?: "Gagal memproses jawaban AI."
                        } else {
                            "Maaf, asisten AI mengalami kendala koneksi (Kode: ${response.code})."
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Gemini request failed", e)
                        "Terjadi kesalahan saat menghubungi Asisten AI: ${e.message}"
                    }
                }

                withContext(Dispatchers.Main) {
                    val escapedResult = JSONObject.quote(resultText)
                    webView.evaluateJavascript("$callbackFn($escapedResult)", null)
                }
            }
        }
    }
}

package com.example.intelligentfish.data.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * WiFi hotspot scanner for fish tank device discovery.
 * Scans for AP with SSID "FishTank" (ESP-01S AP mode).
 *
 * Connection strategy: guide user to system WiFi settings for manual connection,
 * then auto-detect when connected to the device hotspot.
 */
class WifiScanner(private val context: Context) {

    companion object {
        private const val TAG = "WifiScanner"
        const val DEVICE_SSID = "ESP8266"
        const val DEVICE_PASSWORD = "12345678"
        const val DEVICE_TCP_IP = "192.168.4.1"
        const val DEVICE_TCP_PORT = 8080
    }

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val mainHandler = Handler(Looper.getMainLooper())

    data class WifiDevice(
        val ssid: String,
        val bssid: String,
        val signalLevel: Int
    )

    private val _scanResults = MutableStateFlow<List<WifiDevice>>(emptyList())
    val scanResults: StateFlow<List<WifiDevice>> = _scanResults

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isConnectedToAp = MutableStateFlow(false)
    val isConnectedToAp: StateFlow<Boolean> = _isConnectedToAp

    private var apConnectedCallback: (() -> Unit)? = null
    private var pollingRunnable: Runnable? = null

    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                when (intent.action) {
                    WifiManager.NETWORK_STATE_CHANGED_ACTION,
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        checkCurrentWifiConnection()
                    }
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                        processScanResults()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "BroadcastReceiver error: ${e.message}")
            }
        }
    }

    init {
        val intentFilter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        }
        context.registerReceiver(wifiStateReceiver, intentFilter, Context.RECEIVER_EXPORTED)
    }

    private fun hasScanPermission(): Boolean {
        return try {
            @Suppress("MissingPermission")
            wifiManager.scanResults
            true
        } catch (_: SecurityException) {
            false
        }
    }

    @Suppress("DEPRECATION")
    fun startScan() {
        try {
            _isScanning.value = true
            // Read cached results immediately
            processScanResults()
            // Trigger fresh scan
            @Suppress("MissingPermission")
            wifiManager.startScan()
            // Re-read after 2s
            mainHandler.postDelayed({ processScanResults() }, 2000)
        } catch (e: Exception) {
            Log.e(TAG, "startScan error: ${e.message}")
            _isScanning.value = false
        }
    }

    @Suppress("DEPRECATION")
    private fun processScanResults() {
        try {
            @Suppress("MissingPermission")
            val results = wifiManager.scanResults
            val devices = results
                .filter { it.SSID == DEVICE_SSID }
                .map { WifiDevice(it.SSID, it.BSSID, WifiManager.calculateSignalLevel(it.level, 5)) }
                .sortedByDescending { it.signalLevel }
            Log.d(TAG, "Scan complete, found ${devices.size} device(s)")
            _scanResults.value = devices
        } catch (_: SecurityException) {
            Log.w(TAG, "No permission to read scan results")
        }
        _isScanning.value = false
    }

    /**
     * Open system WiFi settings so user can connect to the device hotspot.
     */
    fun connectToAp(ssid: String, callback: (Boolean) -> Unit) {
        apConnectedCallback = {
            mainHandler.post { callback(true) }
        }
        // Open WiFi settings
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open WiFi settings: ${e.message}")
            callback(false)
            return
        }
        // Start polling for connection
        startConnectionPolling()
    }

    private fun startConnectionPolling() {
        stopConnectionPolling()
        pollingRunnable = Runnable {
            if (checkCurrentWifiConnection()) {
                // Connected - callback already triggered
            } else {
                mainHandler.postDelayed(pollingRunnable!!, 2000)
            }
        }
        mainHandler.postDelayed(pollingRunnable!!, 1000)
    }

    private fun stopConnectionPolling() {
        pollingRunnable?.let { mainHandler.removeCallbacks(it) }
        pollingRunnable = null
    }

    @Suppress("DEPRECATION")
    fun checkCurrentWifiConnection(): Boolean {
        try {
            @Suppress("MissingPermission")
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo?.ssid?.replace("\"", "") ?: ""
            val connected = ssid == DEVICE_SSID

            if (connected && !_isConnectedToAp.value) {
                Log.d(TAG, "Connected to device AP: $ssid")
                _isConnectedToAp.value = true
                stopConnectionPolling()
                apConnectedCallback?.invoke()
                apConnectedCallback = null
            } else if (!connected && _isConnectedToAp.value) {
                Log.d(TAG, "Disconnected from device AP")
                _isConnectedToAp.value = false
            }
            return connected
        } catch (e: Exception) {
            return false
        }
    }

    fun disconnect() {
        stopConnectionPolling()
        apConnectedCallback = null
        _isConnectedToAp.value = false
    }

    fun destroy() {
        disconnect()
        try { context.unregisterReceiver(wifiStateReceiver) } catch (_: Exception) {}
    }
}

package com.example.intelligentfish.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intelligentfish.data.model.*
import com.example.intelligentfish.data.net.Protocol
import com.example.intelligentfish.data.net.WifiScanner
import com.example.intelligentfish.data.repository.FishTankRepository
import com.example.intelligentfish.ui.test.FrameLog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FishTankViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FishTankRepository()
    private val wifiScanner = WifiScanner(application)

    val wifiDevices = wifiScanner.scanResults
    val isScanning = wifiScanner.isScanning

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _alarmEvents = MutableStateFlow<List<AlarmEvent>>(emptyList())
    val alarmEvents: StateFlow<List<AlarmEvent>> = _alarmEvents.asStateFlow()

    private val _thresholdSet = MutableStateFlow(ThresholdSet())
    val thresholdSet: StateFlow<ThresholdSet> = _thresholdSet.asStateFlow()

    private val _feedIntervalSeconds = MutableStateFlow(30) // 默认30秒，与MCU一致
    val feedIntervalSeconds: StateFlow<Int> = _feedIntervalSeconds.asStateFlow()

    val toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)

    // ===== Test mode state =====
    private val _testFrameLogs = MutableStateFlow<List<FrameLog>>(emptyList())
    val testFrameLogs: StateFlow<List<FrameLog>> = _testFrameLogs.asStateFlow()

    private val testTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        // 监听连接状态
        viewModelScope.launch {
            repository.connectionState.collect { connected ->
                _connected.value = connected
                if (connected) {
                    toastMessage.emit("设备已连接")
                    repository.queryStatus()
                } else {
                    toastMessage.emit("设备已断开")
                }
            }
        }

        // 监听收到的数据帧
        viewModelScope.launch {
            repository.incomingFrames.collect { frame ->
                if (frame.size < 3) return@collect
                val cmd = frame[1] // CMD 在帧头(0xAA)后第1字节
                when (cmd) {
                    Protocol.CMD_SENSOR_DATA -> {
                        _sensorData.value = Protocol.parseSensorData(frame)
                    }
                    Protocol.CMD_DEVICE_STATUS -> {
                        _deviceStatus.value = Protocol.parseDeviceStatus(frame)
                    }
                    Protocol.CMD_ALARM_EVENT -> {
                        val event = Protocol.parseAlarmEvent(frame)
                        _alarmEvents.value = _alarmEvents.value + event
                        toastMessage.emit(event.message)
                    }
                    // Test frames - always log regardless of mode
                    else -> {
                        val payload = if (frame.size > 3) {
                            frame.drop(3).dropLast(2)
                                .joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
                        } else ""
                        val log = FrameLog(
                            timestamp = testTimeFormat.format(Date()),
                            direction = "RX",
                            cmd = cmd,
                            rawHex = frame.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) },
                            payloadHex = payload
                        )
                        _testFrameLogs.value = _testFrameLogs.value + log
                    }
                }
            }
        }
    }

    fun scanWifiDevices() {
        wifiScanner.startScan()
    }

    /**
     * Direct TCP connect - call when user has already connected to hotspot manually
     */
    fun directConnect() {
        viewModelScope.launch {
            toastMessage.emit("Connecting to device...")
            repository.connect(WifiScanner.DEVICE_TCP_IP, WifiScanner.DEVICE_TCP_PORT, viewModelScope)
        }
    }

    fun connectViaHotspot(ssid: String) {
        viewModelScope.launch {
            toastMessage.emit("Please connect to WiFi: $ssid\nPassword: ${WifiScanner.DEVICE_PASSWORD}")
        }
        wifiScanner.connectToAp(ssid) { success ->
            if (success) {
                viewModelScope.launch {
                    toastMessage.emit("WiFi connected, connecting to device...")
                    repository.connect(WifiScanner.DEVICE_TCP_IP, WifiScanner.DEVICE_TCP_PORT, viewModelScope)
                }
            }
        }
    }

    fun disconnect() {
        repository.disconnect()
        wifiScanner.disconnect()
    }

    fun queryStatus() {
        repository.queryStatus()
    }

    fun switchMode(mode: WorkMode) {
        repository.switchMode(mode)
        // 等待MCU回复CMD_DEVICE_STATUS后由incomingFrames监听器更新UI
        viewModelScope.launch { toastMessage.emit("已切换到${mode.label}模式") }
    }

    /**
     * 控制单个继电器
     * @param relayId 0=加热 1=加水 2=排水 3=增氧
     * @param state true=开 false=关
     */
    fun controlRelay(relayId: Int, state: Boolean) {
        // 加水/排水互斥：开启一个时自动关闭另一个（与MCU端逻辑一致）
        if (relayId == 1 && state) {
            // 开启加水 → 先关闭排水
            repository.controlRelay(2, false)
        } else if (relayId == 2 && state) {
            // 开启排水 → 先关闭加水
            repository.controlRelay(1, false)
        }
        repository.controlRelay(relayId, state)
        // 等待MCU回复CMD_DEVICE_STATUS后由incomingFrames监听器更新UI
    }

    fun syncThresholds() {
        repository.syncThresholds(_thresholdSet.value)
        viewModelScope.launch { toastMessage.emit("阈值已同步到设备") }
    }

    fun triggerFeed() {
        repository.triggerFeed()
        viewModelScope.launch { toastMessage.emit("喂食指令已发送") }
    }

    fun setAlarm(enabled: Boolean) {
        repository.setAlarm(enabled)
        // 等待MCU回复CMD_DEVICE_STATUS后更新UI
    }

    fun setFeedInterval(seconds: Int) {
        val interval = seconds.coerceIn(10, 86400)
        repository.setFeedInterval(interval)
        _feedIntervalSeconds.value = interval
        viewModelScope.launch { toastMessage.emit("喂食间隔已设置为 ${interval} 秒") }
    }

    // 阈值调节方法
    fun adjustTempLower(delta: Float) {
        _thresholdSet.value = _thresholdSet.value.copy(
            tempLower = (_thresholdSet.value.tempLower + delta).coerceIn(0f, _thresholdSet.value.tempUpper - 1f)
        )
    }

    fun adjustTempUpper(delta: Float) {
        _thresholdSet.value = _thresholdSet.value.copy(
            tempUpper = (_thresholdSet.value.tempUpper + delta).coerceIn(_thresholdSet.value.tempLower + 1f, 50f)
        )
    }

    fun adjustPhLower(delta: Float) {
        _thresholdSet.value = _thresholdSet.value.copy(
            phLower = (_thresholdSet.value.phLower + delta).coerceIn(0f, _thresholdSet.value.phUpper - 0.1f)
        )
    }

    fun adjustPhUpper(delta: Float) {
        _thresholdSet.value = _thresholdSet.value.copy(
            phUpper = (_thresholdSet.value.phUpper + delta).coerceIn(_thresholdSet.value.phLower + 0.1f, 14f)
        )
    }

    fun adjustWaterLevelMin(delta: Int) {
        _thresholdSet.value = _thresholdSet.value.copy(
            waterLevelMin = (_thresholdSet.value.waterLevelMin + delta).coerceIn(0, _thresholdSet.value.waterLevelMax - 1)
        )
    }

    fun adjustWaterLevelMax(delta: Int) {
        _thresholdSet.value = _thresholdSet.value.copy(
            waterLevelMax = (_thresholdSet.value.waterLevelMax + delta).coerceIn(_thresholdSet.value.waterLevelMin + 1, 100)
        )
    }

    fun adjustOxygenLevelMax(delta: Int) {
        _thresholdSet.value = _thresholdSet.value.copy(
            oxygenLevelMax = (_thresholdSet.value.oxygenLevelMax + delta).coerceIn(0, 20)
        )
    }

    // ===== Test mode functions =====

    fun testConnect() {
        viewModelScope.launch {
            toastMessage.emit("Connecting to 192.168.4.1:8080...")
            repository.connect(WifiScanner.DEVICE_TCP_IP, WifiScanner.DEVICE_TCP_PORT, viewModelScope)
        }
    }

    fun testSendHex(hexString: String) {
        try {
            val bytes = hexString.trim()
                .split(Regex("[ ,]+"))
                .filter { it.isNotEmpty() }
                .map { it.toInt(16).toByte() }
                .toByteArray()
            if (bytes.isEmpty()) {
                viewModelScope.launch { toastMessage.emit("Invalid HEX input") }
                return
            }
            repository.sendRawBytes(bytes)
            val log = FrameLog(
                timestamp = testTimeFormat.format(Date()),
                direction = "TX",
                cmd = if (bytes.size >= 2) bytes[1] else 0,
                rawHex = bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) },
                payloadHex = if (bytes.size > 3) {
                    bytes.drop(3).dropLast(2).joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
                } else ""
            )
            _testFrameLogs.value = _testFrameLogs.value + log
        } catch (e: Exception) {
            viewModelScope.launch { toastMessage.emit("HEX parse error: \${e.message}") }
        }
    }

    fun testSendEcho() {
        // Build echo frame: AA 10 00 10 55
        val frame = byteArrayOf(0xAA.toByte(), 0x10, 0x00, 0x10.toByte(), 0x55.toByte())
        repository.sendRawBytes(frame)
        val log = FrameLog(
            timestamp = testTimeFormat.format(Date()),
            direction = "TX",
            cmd = 0x10,
            rawHex = frame.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) },
            payloadHex = ""
        )
        _testFrameLogs.value = _testFrameLogs.value + log
    }

    override fun onCleared() {
        super.onCleared()
        wifiScanner.destroy()
        repository.disconnect()
    }
}

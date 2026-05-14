package com.example.intelligentfish.data.repository

import com.example.intelligentfish.data.model.*
import com.example.intelligentfish.data.net.Protocol
import com.example.intelligentfish.data.net.TcpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

class FishTankRepository {

    private val tcpClient = TcpClient()

    val incomingFrames: SharedFlow<ByteArray> = tcpClient.incomingFrames
    val connectionState: SharedFlow<Boolean> = tcpClient.connectionState

    val isConnected: Boolean get() = tcpClient.isConnected

    fun connect(host: String, port: Int, scope: CoroutineScope) {
        tcpClient.connect(host, port, scope)
    }

    fun disconnect() {
        tcpClient.disconnect()
    }

    fun queryStatus() {
        tcpClient.sendFrame(Protocol.buildQueryStatusFrame())
    }

    fun switchMode(mode: WorkMode) {
        tcpClient.sendFrame(Protocol.buildSwitchModeFrame(mode))
    }

    fun controlRelay(relayId: Int, state: Boolean) {
        tcpClient.sendFrame(Protocol.buildControlRelayFrame(relayId, state))
    }

    fun syncThresholds(thresholds: ThresholdSet) {
        tcpClient.sendFrame(Protocol.buildSyncThresholdsFrame(thresholds))
    }

    fun triggerFeed() {
        tcpClient.sendFrame(Protocol.buildTriggerFeedFrame())
    }

    fun setAlarm(enabled: Boolean) {
        tcpClient.sendFrame(Protocol.buildSetAlarmFrame(enabled))
    }

    fun setFeedInterval(seconds: Int) {
        tcpClient.sendFrame(Protocol.buildSetFeedIntervalFrame(seconds))
    }

    fun sendRawBytes(bytes: ByteArray) {
        tcpClient.sendFrame(bytes)
    }
}
